package com.bookstore.sales.api;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

/**
 * The shopping cart of a visitor or a signed-in customer (CRT-1 – CRT-5). Every view is checked
 * against the catalog first: prices and stock are current, and what changed is reported in
 * {@link CartView#adjustments()}.
 */
public interface CartFacade {

    /**
     * Shows a cart.
     *
     * @param ref the cart
     * @return the cart, checked against the catalog
     */
    CartView view(CartRef ref);

    /**
     * Adds copies of a book.
     *
     * @param ref      the cart
     * @param bookId   the book
     * @param quantity the number of copies to add
     * @return the cart
     * @throws NotFoundException   if the book does not exist or is no longer sold
     * @throws ValidationException if the cart would hold too many copies or lines
     */
    CartView add(CartRef ref, BookId bookId, int quantity);

    /**
     * Sets the number of copies of a book; zero removes it.
     *
     * @param ref      the cart
     * @param bookId   the book
     * @param quantity the new number of copies
     * @return the cart
     * @throws NotFoundException if the book does not exist or is no longer sold
     */
    CartView changeQuantity(CartRef ref, BookId bookId, int quantity);

    /**
     * Removes a book.
     *
     * @param ref    the cart
     * @param bookId the book
     * @return the cart
     */
    CartView remove(CartRef ref, BookId bookId);

    /**
     * Empties a cart.
     *
     * @param ref the cart
     * @return the empty cart
     */
    CartView clear(CartRef ref);

    /**
     * Merges a visitor's cart into their stored cart after they sign in (CRT-2). The web layer
     * drops the guest cart afterwards.
     *
     * @param customerId the customer who signed in
     * @param guestCart  the cart they filled as a visitor
     * @return the merged cart
     */
    CartView mergeGuestCart(CustomerId customerId, GuestCart guestCart);
}
