package com.bookstore.customer.application;

import com.bookstore.customer.api.FavoriteFacade;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.customer.domain.FavoriteRepository;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Customer favorite-book use cases. */
@ApplicationScoped
public class FavoriteService implements FavoriteFacade {
    public static final String FAVORITE_EXISTS = "favorite.exists";

    private final CustomerRepository customers;
    private final FavoriteRepository favorites;
    private final TransactionRunner transactions;

    @Inject
    public FavoriteService(CustomerRepository customers, FavoriteRepository favorites, TransactionRunner transactions) {
        this.customers = customers;
        this.favorites = favorites;
        this.transactions = transactions;
    }

    protected FavoriteService() {
        this(null, null, null);
    }

    @Override
    public void add(CustomerId customerId, BookId bookId) {
        requireCustomer(customerId);
        transactions.inTransaction(() -> {
            if (favorites.exists(customerId, bookId)) {
                throw new ConflictException(FAVORITE_EXISTS, "This book is already in your favorites");
            }
            favorites.add(customerId, bookId);
            return null;
        });
    }

    @Override
    public void remove(CustomerId customerId, BookId bookId) {
        requireCustomer(customerId);
        transactions.inTransaction(() -> favorites.remove(customerId, bookId));
    }

    @Override
    public Page<BookId> favorites(CustomerId customerId, PageRequest page) {
        requireCustomer(customerId);
        return favorites.findByCustomer(customerId, page);
    }

    private void requireCustomer(CustomerId customerId) {
        if (customers.findById(customerId).isEmpty()) {
            throw new NotFoundException("Customer", customerId);
        }
    }
}
