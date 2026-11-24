package com.bookstore.customer.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/** Managing one signed-in customer's favorite books. */
public interface FavoriteFacade {
    void add(CustomerId customerId, BookId bookId);

    void remove(CustomerId customerId, BookId bookId);

    Page<BookId> favorites(CustomerId customerId, PageRequest page);
}
