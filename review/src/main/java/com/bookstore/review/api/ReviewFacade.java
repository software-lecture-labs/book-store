package com.bookstore.review.api;

import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.Optional;

/**
 * Reviews and ratings of books (REV-1 – REV-6).
 */
public interface ReviewFacade {

    /** Error code for a customer who has not received the book. */
    String NOT_ELIGIBLE = "review.not-eligible";

    /** Error code for a second review of the same book by the same customer. */
    String DUPLICATE = "review.duplicate";

    /**
     * Writes a review (REV-1 to REV-3).
     *
     * @param command the review
     * @return the stored review
     * @throws NotFoundException     if the book does not exist
     * @throws AccessDeniedException if the customer has not received the book ({@link #NOT_ELIGIBLE})
     * @throws ConflictException     if the customer has already reviewed the book ({@link #DUPLICATE})
     */
    ReviewView write(ReviewCommand command);

    /**
     * Changes the customer's review of a book (REV-3).
     *
     * @param command the new rating and comment
     * @return the changed review
     * @throws NotFoundException if the customer has not reviewed the book
     */
    ReviewView edit(ReviewCommand command);

    /**
     * Deletes the customer's review of a book (REV-3).
     *
     * @param customerId the author
     * @param bookId     the book
     * @throws NotFoundException if the customer has not reviewed the book
     */
    void delete(CustomerId customerId, BookId bookId);

    /**
     * Returns one page of a book's reviews, newest first (REV-4).
     *
     * @param bookId the book
     * @param page   which page to return
     * @return the reviews
     */
    Page<ReviewView> reviewsOf(BookId bookId, PageRequest page);

    /**
     * Returns how many ratings of each star value a book received, for its detail page (REV-6).
     *
     * @param bookId the book
     * @return the breakdown
     */
    RatingBreakdown ratingBreakdown(BookId bookId);

    /**
     * Returns the customer's own review of a book, for pre-filling the edit form.
     *
     * @param customerId the customer
     * @param bookId     the book
     * @return the review, if the customer wrote one
     */
    Optional<ReviewView> myReview(CustomerId customerId, BookId bookId);

    /**
     * Tells whether the customer may write a review of a book, to decide whether to show the form.
     *
     * @param customerId the customer
     * @param bookId     the book
     * @return {@code true} if the customer received the book and has not reviewed it yet
     */
    boolean canReview(CustomerId customerId, BookId bookId);
}
