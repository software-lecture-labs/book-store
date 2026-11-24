package com.bookstore.review.api;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/**
 * Moderating reviews (ADM-7). Callers check that only administrators reach it (ACC-6).
 */
public interface ReviewAdminFacade {

    /**
     * Lists every review, newest first.
     *
     * @param page which page
     * @return the reviews
     */
    Page<ReviewView> reviews(PageRequest page);

    /**
     * Removes an unsuitable review; its rating leaves the book's average too.
     *
     * @param reviewId the review
     * @throws NotFoundException if there is no such review
     */
    void remove(long reviewId);
}
