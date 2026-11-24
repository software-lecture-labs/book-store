package com.bookstore.review.persistence;

import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewId;
import com.bookstore.review.domain.ReviewRepository;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.InMemoryStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Reviews in a map, enforcing one review per customer and book like a unique key would. */
public class InMemoryReviewRepository extends InMemoryStore<ReviewId, Review> implements ReviewRepository {

    /** Creates an empty repository. */
    public InMemoryReviewRepository() {
        super(ReviewId::new);
    }

    @Override
    protected Review copyOf(Review review) {
        return Review.reconstitute(review.id(), review.version(), review.bookId(), review.customerId(),
                review.rating(), review.comment(), review.createdAt(), review.updatedAt());
    }

    private static final Comparator<Review> NEWEST_FIRST =
            Comparator.comparing(Review::createdAt).thenComparing(review -> review.id().value()).reversed();

    @Override
    public Optional<Review> findByBookIdAndCustomerId(BookId bookId, CustomerId customerId) {
        return all().stream()
                .filter(review -> review.bookId().equals(bookId) && review.isWrittenBy(customerId))
                .findFirst();
    }

    @Override
    public Page<Review> findByBookId(BookId bookId, PageRequest page) {
        return Page.fromList(where(review -> review.bookId().equals(bookId)).stream().sorted(NEWEST_FIRST).toList(), page);
    }

    @Override
    public List<Rating> findRatingsByBookId(BookId bookId) {
        return where(review -> review.bookId().equals(bookId)).stream().map(Review::rating).toList();
    }

    @Override
    public Page<Review> findAll(PageRequest page) {
        return Page.fromList(all().stream().sorted(NEWEST_FIRST).toList(), page);
    }

    @Override
    protected void beforeSave(Review review) {
        Optional<Review> existing = findByBookIdAndCustomerId(review.bookId(), review.customerId());
        if (existing.isPresent() && (review.isNew() || !existing.get().id().equals(review.id()))) {
            throw new ConflictException("review.duplicate", "duplicate review");
        }
    }
}
