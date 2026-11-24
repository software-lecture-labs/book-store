package com.bookstore.catalog.application;

import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.Isbn;
import com.bookstore.catalog.domain.Stock;
import com.bookstore.catalog.persistence.InMemoryAuthorRepository;
import com.bookstore.catalog.persistence.InMemoryBookRepository;
import com.bookstore.catalog.persistence.InMemoryCategoryRepository;
import com.bookstore.catalog.persistence.InMemoryFileStorage;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.testing.DirectTransactions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The catalog module wired with in-memory repositories, plus a small category tree.
 */
public final class CatalogShop {

    public final InMemoryBookRepository books = new InMemoryBookRepository();
    public final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
    public final InMemoryAuthorRepository authors = new InMemoryAuthorRepository();
    public final DirectTransactions transactions = new DirectTransactions();
    public final InMemoryFileStorage files = new InMemoryFileStorage();

    public final CatalogService catalog = new CatalogService(books, authors, categories, files);
    public final StockService stock = new StockService(books, transactions);
    public final RatingService ratings = new RatingService(books, transactions);
    public final CatalogAdminService admin = new CatalogAdminService(books, authors, categories, transactions, files);

    /** "Literature", with the subcategories "Novel" and "Poetry". */
    public final Category literature;
    public final Category novels;
    public final Category poetry;
    /** "Science", without subcategories. */
    public final Category science;

    private final AtomicInteger counter = new AtomicInteger();

    public CatalogShop() {
        literature = categories.save(Category.root("Literature"));
        novels = categories.save(literature.addSubcategory("Novel"));
        poetry = categories.save(literature.addSubcategory("Poetry"));
        science = categories.save(Category.root("Science"));
    }

    /** Returns an author with a new id. */
    public Author author(String name) {
        return authors.save(Author.create(name));
    }

    /** Stores an author, as the administrator would. */
    public Author storedAuthor(String name) {
        return author(name);
    }

    /** Stores an active novel. */
    public Book book(String title, String price, int stock) {
        return book(title, price, stock, novels, author("Author of " + title));
    }

    /** Stores an active book in a category, by an author. */
    public Book book(String title, String price, int stock, Category category, Author author) {
        return books.save(Book.builder()
                .isbn(nextIsbn())
                .title(title)
                .author(author)
                .category(category)
                .price(Money.of(price))
                .stock(Stock.of(stock))
                .build());
    }

    private Isbn nextIsbn() {
        String body = "978" + String.format("%09d", counter.incrementAndGet());
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (body.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return new Isbn(body + (10 - sum % 10) % 10);
    }
}
