package com.bookstore.review.application;

import com.bookstore.catalog.api.RatingFacade;
import com.bookstore.customer.api.CustomerDirectory;
import com.bookstore.review.api.ReviewAdminFacade;
import com.bookstore.review.api.ReviewView;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewId;
import com.bookstore.review.domain.ReviewRepository;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The administrator's review use cases (ADM-7). Removing a review takes its rating out of the
 * book's average in the same transaction, as when its author deletes it (ADR-0021).
 */
@ApplicationScoped
public class ReviewAdminService implements ReviewAdminFacade {

    private final ReviewRepository reviews;
    private final RatingFacade ratings;
    private final CustomerDirectory customers;
    private final TransactionRunner transactions;

    /**
     * Creates the service.
     *
     * @param reviews      where reviews are stored
     * @param ratings      the catalog's rating totals
     * @param customers    the customer module's directory, for authors' names
     * @param transactions runs each change in a transaction
     */
    @Inject
    public ReviewAdminService(ReviewRepository reviews, RatingFacade ratings, CustomerDirectory customers,
                              TransactionRunner transactions) {
        this.reviews = reviews;
        this.ratings = ratings;
        this.customers = customers;
        this.transactions = transactions;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected ReviewAdminService() {
        this(null, null, null, null);
    }

    @Override
    public Page<ReviewView> reviews(PageRequest page) {
        return ReviewMapper.pageOf(reviews.findAll(page), customers);
    }

    @Override
    public void remove(long reviewId) {
        transactions.run(() -> {
            ReviewId id = ReviewId.of(reviewId);
            Review review = reviews.findById(id).orElseThrow(() -> new NotFoundException("Review", id));
            ratings.removeRating(review.bookId(), review.rating().stars());
            reviews.delete(review);
        });
    }
}
