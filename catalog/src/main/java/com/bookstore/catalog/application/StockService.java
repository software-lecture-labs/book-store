package com.bookstore.catalog.application;

import com.bookstore.catalog.api.BookOffer;
import com.bookstore.catalog.api.StockFacade;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookRepository;
import com.bookstore.catalog.domain.InsufficientStockException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Prices and stock for other modules.
 */
@ApplicationScoped
public class StockService implements StockFacade {

    private final BookRepository books;
    private final TransactionRunner transactions;

    /**
     * Creates the service.
     *
     * @param books        where books are stored
     * @param transactions runs work in a transaction; joins the caller's if there is one
     */
    @Inject
    public StockService(BookRepository books, TransactionRunner transactions) {
        this.books = books;
        this.transactions = transactions;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected StockService() {
        this(null, null);
    }

    @Override
    public List<BookOffer> offers(Collection<BookId> bookIds) {
        return books.findAllById(bookIds).stream().map(StockService::offerOf).toList();
    }

    @Override
    public void reserve(Map<BookId, Integer> quantities) {
        transactions.run(() -> {
            Map<BookId, Book> found = load(quantities);
            // Check every book before taking any stock, so a failure leaves all of them unchanged.
            quantities.forEach((bookId, quantity) -> {
                Book book = found.get(bookId);
                if (!book.isActive()) {
                    throw new ConflictException("book.inactive", "'" + book.title() + "' is no longer sold");
                }
                if (!book.stock().hasAtLeast(quantity)) {
                    throw new InsufficientStockException(book.title(), quantity, book.stock().quantity());
                }
            });
            quantities.forEach((bookId, quantity) -> found.get(bookId).reserve(quantity));
            found.values().forEach(books::save);
        });
    }

    @Override
    public void release(Map<BookId, Integer> quantities) {
        transactions.run(() -> {
            Map<BookId, Book> found = load(quantities);
            quantities.forEach((bookId, quantity) -> found.get(bookId).release(quantity));
            found.values().forEach(books::save);
        });
    }

    private Map<BookId, Book> load(Map<BookId, Integer> quantities) {
        Map<BookId, Book> found = books.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Book::id, Function.identity()));
        for (BookId bookId : quantities.keySet()) {
            if (!found.containsKey(bookId)) {
                throw new NotFoundException("Book", bookId);
            }
        }
        return found;
    }

    private static BookOffer offerOf(Book book) {
        return new BookOffer(book.id(), book.title(), book.authorNames(), book.price(), book.stock().quantity(),
                book.isActive());
    }
}
