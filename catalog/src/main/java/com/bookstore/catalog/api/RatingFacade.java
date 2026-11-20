package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;

/**
 * The rating totals kept on each book (ADR-0012), as the review module updates them. Changes join
 * the caller's transaction, so the totals and the reviews always change together.
 */
public interface RatingFacade {

    /**
     * Tells whether a book exists, active or not.
     *
     * @param bookId the book's id
     * @return {@code true} if there is such a book
     */
    boolean exists(BookId bookId);

    /**
     * Adds a rating to a book's totals.
     *
     * @param bookId the book's id
     * @param stars  from 1 to 5
     * @throws com.bookstore.shared.kernel.error.NotFoundException if the book does not exist
     */
    void addRating(BookId bookId, int stars);

    /**
     * Removes a rating that was added before from a book's totals.
     *
     * @param bookId the book's id
     * @param stars  the star value that was added
     * @throws com.bookstore.shared.kernel.error.NotFoundException if the book does not exist
     */
    void removeRating(BookId bookId, int stars);
}
