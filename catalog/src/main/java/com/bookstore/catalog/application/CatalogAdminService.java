package com.bookstore.catalog.application;

import com.bookstore.catalog.api.AdminBookRow;
import com.bookstore.catalog.api.AuthorView;
import com.bookstore.catalog.api.BookCommand;
import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.api.CatalogStats;
import com.bookstore.catalog.api.CategoryNode;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.AuthorId;
import com.bookstore.catalog.domain.AuthorRepository;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookRepository;
import com.bookstore.catalog.domain.BookSort;
import com.bookstore.catalog.domain.BookSpecifications;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.CategoryId;
import com.bookstore.catalog.domain.CategoryRepository;
import com.bookstore.catalog.domain.CoverImage;
import com.bookstore.catalog.domain.FileStorage;
import com.bookstore.catalog.domain.Isbn;
import com.bookstore.catalog.domain.Language;
import com.bookstore.catalog.domain.Stock;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.specification.Specification;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.text.Collator;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The administrator's catalog use cases (ADM-1 – ADM-3, ADM-8). Every change runs in one
 * transaction and goes through the domain's intention-revealing methods; the repositories reject
 * stale versions, so two administrators cannot overwrite each other unnoticed.
 */
@ApplicationScoped
public class CatalogAdminService implements CatalogAdminFacade {

    /** The most books listed as running low on the overview. */
    static final int LOW_STOCK_LIST_SIZE = 20;

    /** The storage folder of cover images. */
    static final String COVER_FOLDER = "covers";

    private static final Collator TURKISH = Collator.getInstance(Locale.forLanguageTag("tr"));

    private final BookRepository books;
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final TransactionRunner transactions;
    private final FileStorage files;

    /**
     * Creates the service.
     *
     * @param books        where books are stored
     * @param authors      where authors are stored
     * @param categories   where categories are stored
     * @param transactions runs each change in a transaction
     * @param files        where cover images are kept
     */
    @Inject
    public CatalogAdminService(BookRepository books, AuthorRepository authors, CategoryRepository categories,
                               TransactionRunner transactions, FileStorage files) {
        this.books = books;
        this.authors = authors;
        this.categories = categories;
        this.transactions = transactions;
        this.files = files;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CatalogAdminService() {
        this(null, null, null, null, null);
    }

    @Override
    public Page<AdminBookRow> books(String text, PageRequest page) {
        return books.search(BookSpecifications.matchingText(text), BookSort.DEFAULT, page).map(CatalogMapper::rowOf);
    }

    @Override
    public BookDetails book(BookId bookId) {
        return CatalogMapper.detailsOf(load(bookId));
    }

    @Override
    public BookDetails createBook(BookCommand command) {
        return transactions.inTransaction(() -> {
            Isbn isbn = Isbn.parse(command.isbn());
            if (books.findByIsbn(isbn).isPresent()) {
                throw new ConflictException("book.isbn-taken", "ISBN " + isbn + " is already used by another book");
            }
            Book book = Book.builder()
                    .isbn(isbn)
                    .title(command.title())
                    .authors(authorsOf(command.authorIds()))
                    .category(category(command.categoryId()))
                    .publisher(command.publisher())
                    .publicationYear(yearOf(command.publicationYear()))
                    .language(languageOf(command.language()))
                    .pageCount(command.pageCount())
                    .description(command.description())
                    .price(command.price())
                    .stock(Stock.of(command.stock()))
                    .build();
            return CatalogMapper.detailsOf(books.save(book));
        });
    }

    @Override
    public BookDetails updateBook(BookId bookId, BookCommand command, Integer expectedVersion) {
        return change(bookId, expectedVersion, book -> {
            book.changeDetails(command.title(), command.publisher(), yearOf(command.publicationYear()),
                    languageOf(command.language()), command.pageCount(), command.description());
            book.changeAuthors(authorsOf(command.authorIds()));
            book.moveTo(category(command.categoryId()));
            book.changePrice(command.price());
            book.changeStock(command.stock());
        });
    }

    @Override
    public BookDetails changePrice(BookId bookId, Money price, Integer expectedVersion) {
        return change(bookId, expectedVersion, book -> book.changePrice(price));
    }

    @Override
    public BookDetails changeStock(BookId bookId, int quantity, Integer expectedVersion) {
        return change(bookId, expectedVersion, book -> book.changeStock(quantity));
    }

    @Override
    public BookDetails setActive(BookId bookId, boolean active, Integer expectedVersion) {
        return change(bookId, expectedVersion, book -> {
            if (active) {
                book.activate();
            } else {
                book.deactivate();
            }
        });
    }

    /**
     * Stores the file first and records its key in the book's transaction. If that fails, the new
     * file is deleted again; once it succeeded, the replaced file goes.
     */
    @Override
    public BookDetails attachCover(BookId bookId, byte[] content) {
        CoverImage image = CoverImage.of(content);
        load(bookId);
        String key = files.store(COVER_FOLDER, image.content(), image.type().extension());
        AtomicReference<String> replaced = new AtomicReference<>();
        BookDetails details;
        try {
            details = change(bookId, null, book -> {
                replaced.set(book.coverImage().orElse(null));
                book.attachCover(key);
            });
        } catch (RuntimeException e) {
            files.delete(key);
            throw e;
        }
        if (replaced.get() != null) {
            files.delete(replaced.get());
        }
        return details;
    }

    @Override
    public List<AuthorView> authors() {
        return authors.findAll().stream()
                .sorted(Comparator.comparing(Author::name, TURKISH))
                .map(CatalogMapper::authorViewOf)
                .toList();
    }

    @Override
    public AuthorView addAuthor(String name) {
        return transactions.inTransaction(() -> CatalogMapper.authorViewOf(authors.save(Author.create(name))));
    }

    @Override
    public AuthorView renameAuthor(long authorId, String name) {
        return transactions.inTransaction(() -> {
            Author author = author(authorId);
            author.rename(name);
            return CatalogMapper.authorViewOf(authors.save(author));
        });
    }

    @Override
    public void removeAuthor(long authorId) {
        transactions.run(() -> {
            Author author = author(authorId);
            if (books.existsByAuthorId(author.id())) {
                throw new ConflictException("author.in-use", author.name() + " still has books in the catalog");
            }
            authors.delete(author);
        });
    }

    @Override
    public CategoryNode addCategory(String name, Long parentId) {
        return transactions.inTransaction(() -> {
            Category category = parentId == null ? Category.root(name) : category(parentId).addSubcategory(name);
            return CatalogMapper.nodeOf(categories.save(category));
        });
    }

    @Override
    public CategoryNode renameCategory(long categoryId, String name) {
        return transactions.inTransaction(() -> {
            Category category = category(categoryId);
            category.rename(name);
            return CatalogMapper.nodeOf(categories.save(category));
        });
    }

    @Override
    public void removeCategory(long categoryId) {
        transactions.run(() -> {
            Category category = category(categoryId);
            if (!category.children().isEmpty()) {
                throw new ConflictException("category.has-children", category.name() + " has subcategories");
            }
            if (books.existsByCategoryId(category.id())) {
                throw new ConflictException("category.in-use", category.name() + " still has books");
            }
            categories.delete(category);
        });
    }

    @Override
    public CatalogStats stats(int lowStockBelow) {
        PageRequest countOnly = new PageRequest(0, 1);
        long total = books.search(Specification.all(), BookSort.DEFAULT, countOnly).totalElements();
        long active = books.search(BookSpecifications.active(), BookSort.DEFAULT, countOnly).totalElements();
        Specification<Book> runningLow = BookSpecifications.active().and(BookSpecifications.stockBelow(lowStockBelow));
        List<AdminBookRow> low = books.search(runningLow, BookSort.DEFAULT, new PageRequest(0, LOW_STOCK_LIST_SIZE))
                .content().stream()
                .map(CatalogMapper::rowOf)
                .sorted(Comparator.comparingInt(AdminBookRow::stock))
                .toList();
        return new CatalogStats(total, active, lowStockBelow, low);
    }

    /**
     * Loads a book, checks the version the caller read, applies a change and saves, in one
     * transaction. The repository also rejects a version that changed between the load and the save.
     */
    private BookDetails change(BookId bookId, Integer expectedVersion, Consumer<Book> change) {
        return transactions.inTransaction(() -> {
            Book book = load(bookId);
            if (expectedVersion != null && book.version() != expectedVersion) {
                throw new ConflictException(ConflictException.CONCURRENT_MODIFICATION, "Book " + bookId
                        + " is at version " + book.version() + ", not " + expectedVersion);
            }
            change.accept(book);
            return CatalogMapper.detailsOf(books.save(book));
        });
    }

    private Book load(BookId bookId) {
        return books.findById(bookId).orElseThrow(() -> new NotFoundException("Book", bookId));
    }

    private Author author(long authorId) {
        AuthorId id = AuthorId.of(authorId);
        return authors.findById(id).orElseThrow(() -> new NotFoundException("Author", id));
    }

    private Category category(long categoryId) {
        CategoryId id = CategoryId.of(categoryId);
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    }

    /** Loads the authors in the order given; unknown ids are a 404. */
    private List<Author> authorsOf(List<Long> authorIds) {
        if (authorIds.isEmpty()) {
            throw new ValidationException("authorIds", "must name at least one author");
        }
        List<AuthorId> ids = authorIds.stream().distinct().map(AuthorId::of).toList();
        Map<AuthorId, Author> found = authors.findAllById(ids).stream()
                .collect(Collectors.toMap(Author::id, Function.identity()));
        return ids.stream()
                .map(id -> {
                    Author author = found.get(id);
                    if (author == null) {
                        throw new NotFoundException("Author", id);
                    }
                    return author;
                })
                .toList();
    }

    private static Year yearOf(Integer year) {
        return year == null ? null : Year.of(year);
    }

    private static Language languageOf(String code) {
        if (code == null) {
            throw new ValidationException("language", "must not be empty");
        }
        return Language.fromCode(code);
    }
}
