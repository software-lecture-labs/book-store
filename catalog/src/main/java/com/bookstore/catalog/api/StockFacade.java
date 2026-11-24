package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The catalog's prices and stock, as other modules may use them (ADR-0003: a business module uses
 * another only through its {@code api} package). The sales module uses it for carts and checkout.
 *
 * <p>Changes join the caller's transaction, so stock taken for an order is put back if the order
 * is rolled back.
 */
public interface StockFacade {

    /**
     * Returns the current offer for several books. Ids with no book are skipped; deactivated books
     * are included with {@code active == false}.
     *
     * @param bookIds the book ids
     * @return the offers, in no particular order
     */
    List<BookOffer> offers(Collection<BookId> bookIds);

    /**
     * Takes copies out of stock, all or nothing: if any book is missing, no longer sold or short of
     * stock, no stock is taken at all.
     *
     * @param quantities copies to take, keyed by book id
     * @throws com.bookstore.shared.kernel.error.NotFoundException if a book does not exist
     * @throws com.bookstore.shared.kernel.error.ConflictException if a book is no longer sold or
     *                                                             has too few copies
     */
    void reserve(Map<BookId, Integer> quantities);

    /**
     * Puts copies back into stock, for example when an order is cancelled.
     *
     * @param quantities copies to return, keyed by book id
     * @throws com.bookstore.shared.kernel.error.NotFoundException if a book does not exist
     */
    void release(Map<BookId, Integer> quantities);
}
