package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/** Stores a customer's favorite book ids. */
public interface FavoriteRepository {
    void add(CustomerId customerId, BookId bookId);

    boolean remove(CustomerId customerId, BookId bookId);

    Page<BookId> findByCustomer(CustomerId customerId, PageRequest page);

    boolean exists(CustomerId customerId, BookId bookId);
}
