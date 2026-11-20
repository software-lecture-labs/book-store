package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.AuthorId;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookRepository;
import com.bookstore.catalog.domain.BookSort;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.CategoryId;
import com.bookstore.catalog.domain.Isbn;
import com.bookstore.catalog.domain.Language;
import com.bookstore.catalog.domain.RatingSummary;
import com.bookstore.catalog.domain.Stock;
import com.bookstore.shared.jdbc.Columns;
import com.bookstore.shared.jdbc.DataAccessException;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.specification.Specification;
import com.bookstore.shared.kernel.text.TextNormalizer;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Stores books in the {@code books} table and their authors in {@code book_authors}.
 *
 * <p>Loading books takes three queries whatever the number of books: one for the book rows, one
 * for all their authors, and one for the category tree. This avoids running one author query per
 * book (the "N+1 queries" problem).
 */
@ApplicationScoped
public class JdbcBookRepository implements BookRepository {

    private static final String COLUMNS = "b.id, b.version, b.isbn, b.title, b.category_id, b.publisher, "
            + "b.publication_year, b.language, b.page_count, b.description, b.price, b.stock, b.cover_image, "
            + "b.active, b.rating_count, b.rating_sum";

    private final JdbcExecutor jdbc;
    private final JdbcCategoryRepository categories;

    /**
     * Creates the repository.
     *
     * @param jdbc       runs the SQL
     * @param categories loads the category tree that books point into
     */
    @Inject
    public JdbcBookRepository(JdbcExecutor jdbc, JdbcCategoryRepository categories) {
        this.jdbc = jdbc;
        this.categories = categories;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcBookRepository() {
        this(null, null);
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Optional<Book> findById(BookId id) {
        return load("SELECT " + COLUMNS + " FROM books b WHERE b.id = ?", id).stream().findFirst();
    }

    @Override
    public Optional<Book> findByIsbn(Isbn isbn) {
        return load("SELECT " + COLUMNS + " FROM books b WHERE b.isbn = ?", isbn.value()).stream().findFirst();
    }

    @Override
    public List<Book> findAllById(Collection<BookId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return load("SELECT " + COLUMNS + " FROM books b WHERE b.id IN (" + JdbcExecutor.placeholders(ids.size())
                + ")", JdbcExecutor.params(ids));
    }

    @Override
    public Page<Book> search(Specification<Book> specification, BookSort sort, PageRequest page) {
        BookSearchSql sql = new BookSearchSql(specification, sort);
        Object[] whereParams = sql.params().toArray();

        long total = jdbc.queryForLong("SELECT COUNT(*) FROM books b" + sql.where(), whereParams);
        if (total == 0) {
            return Page.empty(page);
        }
        Object[] pageParams = Stream.concat(sql.params().stream(), Stream.of(page.size(), page.offset())).toArray();
        List<Book> books = load("SELECT " + COLUMNS + " FROM books b" + sql.where() + sql.orderBy()
                + " LIMIT ? OFFSET ?", pageParams);
        return new Page<>(books, page, total);
    }

    @Override
    public boolean existsByAuthorId(AuthorId authorId) {
        return jdbc.exists("SELECT 1 FROM book_authors WHERE author_id = ? LIMIT 1", authorId);
    }

    @Override
    public boolean existsByCategoryId(CategoryId categoryId) {
        return jdbc.exists("SELECT 1 FROM books WHERE category_id = ? LIMIT 1", categoryId);
    }

    // ---------------------------------------------------------------- commands

    @Override
    public Book save(Book book) {
        try {
            if (book.isNew()) {
                insert(book);
            } else {
                update(book);
            }
        } catch (DataAccessException e) {
            if (e.violates("uq_books_isbn")) {
                throw new ConflictException("book.isbn-taken", "ISBN " + book.isbn() + " is already used");
            }
            throw e;
        }
        return book;
    }

    private void insert(Book book) {
        long id = jdbc.insert("INSERT INTO books (isbn, title, title_key, category_id, publisher, publication_year, "
                        + "language, page_count, description, price, stock, cover_image, active, rating_count, rating_sum) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                book.isbn().value(), book.title(), TextNormalizer.searchKey(book.title()), book.category().id(),
                book.publisher(), book.publicationYear(), book.language().code(), book.pageCount(),
                book.description(), book.price(), book.stock().quantity(), book.coverImage(), book.isActive(),
                book.ratings().count(), book.ratings().sum());
        book.markPersisted(new BookId(id), 0);
        insertAuthors(book);
    }

    private void update(Book book) {
        jdbc.updateVersioned("Book " + book.isbn(),
                "UPDATE books SET title = ?, title_key = ?, category_id = ?, publisher = ?, publication_year = ?, "
                        + "language = ?, page_count = ?, description = ?, price = ?, stock = ?, cover_image = ?, "
                        + "active = ?, rating_count = ?, rating_sum = ?, version = version + 1 "
                        + "WHERE id = ? AND version = ?",
                book.title(), TextNormalizer.searchKey(book.title()), book.category().id(), book.publisher(),
                book.publicationYear(), book.language().code(), book.pageCount(), book.description(), book.price(),
                book.stock().quantity(), book.coverImage(), book.isActive(), book.ratings().count(),
                book.ratings().sum(), book.id(), book.version());
        book.markPersisted(book.id(), book.version() + 1);
        jdbc.update("DELETE FROM book_authors WHERE book_id = ?", book.id());
        insertAuthors(book);
    }

    private void insertAuthors(Book book) {
        List<Object[]> rows = new ArrayList<>();
        int order = 0;
        for (Author author : book.authors()) {
            if (author.isNew()) {
                throw new IllegalStateException("Store author '" + author.name() + "' before the book");
            }
            rows.add(new Object[] {book.id(), author.id(), order++});
        }
        jdbc.batch("INSERT INTO book_authors (book_id, author_id, sort_order) VALUES (?, ?, ?)", rows);
    }

    // ---------------------------------------------------------------- loading

    private List<Book> load(String sql, Object... params) {
        List<BookRow> rows = jdbc.query(sql, BookRow::read, params);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Author>> authors = authorsOf(rows.stream().map(BookRow::id).toList());
        Map<CategoryId, Category> tree = categories.loadTree();
        return rows.stream().map(row -> row.toBook(authors.getOrDefault(row.id(), List.of()), tree)).toList();
    }

    private Map<Long, List<Author>> authorsOf(List<Long> bookIds) {
        List<BookAuthorRow> rows = jdbc.query(
                "SELECT ba.book_id, a.id, a.version, a.name FROM book_authors ba JOIN authors a ON a.id = ba.author_id "
                        + "WHERE ba.book_id IN (" + JdbcExecutor.placeholders(bookIds.size()) + ") "
                        + "ORDER BY ba.book_id, ba.sort_order",
                row -> new BookAuthorRow(row.getLong("book_id"), JdbcAuthorRepository.MAPPER.map(row)),
                JdbcExecutor.params(bookIds));
        return rows.stream().collect(Collectors.groupingBy(BookAuthorRow::bookId, LinkedHashMap::new,
                Collectors.mapping(BookAuthorRow::author, Collectors.toList())));
    }

    private record BookAuthorRow(long bookId, Author author) {
    }

    /** The columns of one {@code books} row, before the authors and category are attached. */
    private record BookRow(long id, int version, String isbn, String title, long categoryId, String publisher,
                           Year publicationYear, String language, int pageCount,
                           String description, BigDecimal price, int stock, String coverImage,
                           boolean active, int ratingCount, int ratingSum) {

        static BookRow read(ResultSet row) throws SQLException {
            return new BookRow(row.getLong("id"), row.getInt("version"), row.getString("isbn"),
                    row.getString("title"), row.getLong("category_id"), row.getString("publisher"),
                    Columns.year(row, "publication_year").orElse(null), row.getString("language"), row.getInt("page_count"),
                    row.getString("description"), row.getBigDecimal("price"), row.getInt("stock"),
                    row.getString("cover_image"), row.getBoolean("active"), row.getInt("rating_count"),
                    row.getInt("rating_sum"));
        }

        Book toBook(List<Author> authors, Map<CategoryId, Category> tree) {
            Category category = tree.get(new CategoryId(categoryId));
            if (category == null) {
                throw new IllegalStateException("Book " + id + " points to missing category " + categoryId);
            }
            return Book.builder()
                    .persisted(new BookId(id), version)
                    .isbn(new Isbn(isbn))
                    .title(title)
                    .authors(authors)
                    .category(category)
                    .publisher(publisher)
                    .publicationYear(publicationYear)
                    .language(Language.fromCode(language))
                    .pageCount(pageCount)
                    .description(description)
                    .price(Money.of(price))
                    .stock(Stock.of(stock))
                    .coverImage(coverImage)
                    .active(active)
                    .ratings(new RatingSummary(ratingCount, ratingSum))
                    .build();
        }
    }
}
