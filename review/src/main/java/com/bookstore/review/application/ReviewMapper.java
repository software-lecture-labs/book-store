package com.bookstore.review.application;

import com.bookstore.customer.api.CustomerDirectory;
import com.bookstore.review.api.RatingBreakdown;
import com.bookstore.review.api.ReviewView;
import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.RatingDistribution;
import com.bookstore.review.domain.Review;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Translates reviews into the {@code api} contract's views (Mapper, ADR-0003).
 */
final class ReviewMapper {

    /** Shown for an author the customer module no longer knows. */
    static final String UNKNOWN_AUTHOR = "\u2014";

    private ReviewMapper() {
    }

    static ReviewView viewOf(Review review, String authorName) {
        return new ReviewView(review.id().value(), review.bookId(), review.customerId(), authorName,
                review.rating().stars(), review.comment(), review.createdAt(),
                review.updatedAt().isAfter(review.createdAt()));
    }

    /**
     * Maps a page of reviews, asking the customer module for the authors' public names once.
     */
    static Page<ReviewView> pageOf(Page<Review> found, CustomerDirectory customers) {
        Set<CustomerId> authorIds = found.content().stream().map(Review::customerId).collect(Collectors.toSet());
        Map<CustomerId, String> names = customers.publicNames(authorIds);
        return found.map(review -> viewOf(review, names.getOrDefault(review.customerId(), UNKNOWN_AUTHOR)));
    }

    static RatingBreakdown breakdownOf(RatingDistribution distribution) {
        List<Integer> counts = new ArrayList<>();
        for (int stars = Rating.MIN; stars <= Rating.MAX; stars++) {
            counts.add(distribution.count(Rating.of(stars)));
        }
        return new RatingBreakdown(counts, distribution.average().orElse(null));
    }
}
