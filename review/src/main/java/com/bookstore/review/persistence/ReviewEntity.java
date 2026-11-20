package com.bookstore.review.persistence;

import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewId;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A row of {@code reviews} as JPA sees it (ADR-0023). It is the persistence model only: the domain's
 * {@link Review} stays free of annotations, and the two are copied into each other here, the way
 * the JDBC repository maps rows. Times are stored as UTC wall-clock times, as the JDBC code does.
 */
@Entity
@Table(name = "reviews")
class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private int version;

    @Column(name = "book_id", nullable = false, updatable = false)
    private long bookId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String commentText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** For JPA only. */
    protected ReviewEntity() {
    }

    /**
     * Copies a review, keeping its id and version for a stored one.
     *
     * @param review the review
     * @return the entity
     */
    static ReviewEntity of(Review review) {
        ReviewEntity entity = new ReviewEntity();
        if (!review.isNew()) {
            entity.id = review.id().value();
            entity.version = review.version();
        }
        entity.bookId = review.bookId().value();
        entity.userId = review.customerId().value();
        entity.copyChangeable(review);
        entity.createdAt = LocalDateTime.ofInstant(review.createdAt(), ZoneOffset.UTC);
        return entity;
    }

    /**
     * Takes over what an edit can change: the rating, the comment and the edit time.
     *
     * @param review the edited review
     */
    void copyChangeable(Review review) {
        rating = review.rating().stars();
        commentText = review.comment();
        updatedAt = LocalDateTime.ofInstant(review.updatedAt(), ZoneOffset.UTC);
    }

    /** @return the stored review, rebuilt for the domain */
    Review toReview() {
        return Review.reconstitute(ReviewId.of(id), version, BookId.of(bookId), CustomerId.of(userId), Rating.of(rating),
                commentText, createdAt.toInstant(ZoneOffset.UTC), updatedAt.toInstant(ZoneOffset.UTC));
    }

    /** @return the generated id */
    long id() {
        return id;
    }

    /** @return the version JPA keeps */
    int version() {
        return version;
    }
}
