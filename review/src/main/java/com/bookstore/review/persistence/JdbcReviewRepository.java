package com.bookstore.review.persistence;

import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewId;
import com.bookstore.review.domain.ReviewRepository;
import com.bookstore.shared.jdbc.DataAccessException;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.RecordRowMapper;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Stores reviews in the {@code reviews} table. A unique key on {@code (user_id, book_id)} keeps
 * customers to one review per book even if two requests arrive at the same time.
 */
@ApplicationScoped
public class JdbcReviewRepository implements ReviewRepository {

    private static final String COLUMNS = "id, version, book_id, user_id, rating, comment_text, created_at, updated_at";
    private static final String NEWEST_FIRST = " ORDER BY created_at DESC, id DESC";
    private static final RecordRowMapper<ReviewRow> ROWS = RecordRowMapper.of(ReviewRow.class);

    private final JdbcExecutor jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc runs the SQL
     */
    @Inject
    public JdbcReviewRepository(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcReviewRepository() {
        this(null);
    }

    @Override
    public Optional<Review> findById(ReviewId id) {
        return jdbc.queryOne("SELECT " + COLUMNS + " FROM reviews WHERE id = ?", ROWS, id).map(ReviewRow::toReview);
    }

    @Override
    public Optional<Review> findByBookIdAndCustomerId(BookId bookId, CustomerId customerId) {
        return jdbc.queryOne("SELECT " + COLUMNS + " FROM reviews WHERE book_id = ? AND user_id = ?", ROWS, bookId,
                customerId).map(ReviewRow::toReview);
    }

    @Override
    public Page<Review> findByBookId(BookId bookId, PageRequest page) {
        long total = jdbc.queryForLong("SELECT COUNT(*) FROM reviews WHERE book_id = ?", bookId);
        List<Review> reviews = jdbc.query("SELECT " + COLUMNS + " FROM reviews WHERE book_id = ?" + NEWEST_FIRST
                + " LIMIT ? OFFSET ?", ROWS, bookId, page.size(), page.offset())
                .stream().map(ReviewRow::toReview).toList();
        return new Page<>(reviews, page, total);
    }

    @Override
    public List<Rating> findRatingsByBookId(BookId bookId) {
        return jdbc.query("SELECT rating FROM reviews WHERE book_id = ?", row -> Rating.of(row.getInt(1)), bookId);
    }

    @Override
    public Page<Review> findAll(PageRequest page) {
        long total = jdbc.queryForLong("SELECT COUNT(*) FROM reviews");
        List<Review> reviews = jdbc.query("SELECT " + COLUMNS + " FROM reviews" + NEWEST_FIRST + " LIMIT ? OFFSET ?",
                ROWS, page.size(), page.offset()).stream().map(ReviewRow::toReview).toList();
        return new Page<>(reviews, page, total);
    }

    @Override
    public Review save(Review review) {
        if (review.isNew()) {
            try {
                long id = jdbc.insert("INSERT INTO reviews (book_id, user_id, rating, comment_text, created_at, "
                                + "updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                        review.bookId(), review.customerId(), review.rating().stars(), review.comment(),
                        review.createdAt(), review.updatedAt());
                review.markPersisted(ReviewId.of(id), 0);
            } catch (DataAccessException e) {
                if (e.violates("uq_reviews_user_book")) {
                    throw new ConflictException("review.duplicate", "The customer has already reviewed this book");
                }
                throw e;
            }
        } else {
            jdbc.updateVersioned("Review " + review.id(),
                    "UPDATE reviews SET rating = ?, comment_text = ?, updated_at = ?, version = version + 1 "
                            + "WHERE id = ? AND version = ?",
                    review.rating().stars(), review.comment(), review.updatedAt(), review.id(), review.version());
            review.markPersisted(review.id(), review.version() + 1);
        }
        return review;
    }

    @Override
    public void delete(Review review) {
        jdbc.update("DELETE FROM reviews WHERE id = ?", review.id());
    }

    /** One {@code reviews} row, read by reflection. */
    private record ReviewRow(long id, int version, long bookId, long userId, int rating, String commentText,
                             Instant createdAt, Instant updatedAt) {

        Review toReview() {
            return Review.reconstitute(ReviewId.of(id), version, BookId.of(bookId), CustomerId.of(userId),
                    Rating.of(rating), commentText, createdAt, updatedAt);
        }
    }
}
