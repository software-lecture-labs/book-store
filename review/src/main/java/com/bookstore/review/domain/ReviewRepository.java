package com.bookstore.review.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Stores and finds reviews.
 */
public interface ReviewRepository {

    /**
     * Finds a review by id.
     *
     * @param id the id
     * @return the review, or empty
     */
    Optional<Review> findById(ReviewId id);

    /**
     * Finds a customer's review of a book.
     *
     * @param bookId     the book's id
     * @param customerId the customer's id
     * @return the review, or empty if the customer has not reviewed the book
     */
    Optional<Review> findByBookIdAndCustomerId(BookId bookId, CustomerId customerId);

    /**
     * Returns one page of a book's reviews, newest first.
     *
     * @param bookId the book's id
     * @param page   which page to return
     * @return the page
     */
    Page<Review> findByBookId(BookId bookId, PageRequest page);

    /**
     * Returns every rating given to a book, for its rating distribution.
     *
     * @param bookId the book's id
     * @return the ratings, in no particular order
     */
    List<Rating> findRatingsByBookId(BookId bookId);

    /**
     * Returns one page of all reviews, newest first, for moderation.
     *
     * @param page which page to return
     * @return the page
     */
    Page<Review> findAll(PageRequest page);

    /**
     * Inserts a new review or updates a stored one.
     *
     * @param review the review
     * @return the same review, with its id and version updated
     * @throws com.bookstore.shared.kernel.error.ConflictException if the customer has already reviewed the
     *                                                      book
     */
    Review save(Review review);

    /**
     * Deletes a review.
     *
     * @param review a stored review
     */
    void delete(Review review);
}
