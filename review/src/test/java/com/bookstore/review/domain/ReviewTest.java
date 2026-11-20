package com.bookstore.review.domain;

import static com.bookstore.shared.kernel.testing.TestClock.CLOCK;
import static com.bookstore.shared.kernel.testing.TestClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReviewTest {

    private static final CustomerId CUSTOMER_ID = CustomerId.of(7);

    @Test
    void writesAReviewWithAnOptionalComment() {
        Review review = Review.write(BookId.of(1), CUSTOMER_ID, Rating.of(4), null, CLOCK);

        assertThat(review.comment()).isEmpty();
        assertThat(review.createdAt()).isEqualTo(NOW);
        assertThat(review.isWrittenBy(CUSTOMER_ID)).isTrue();
        assertThat(review.isWrittenBy(CustomerId.of(99))).isFalse();
    }

    @Test
    void editingUpdatesRatingCommentAndTime() {
        Review review = Review.write(BookId.of(1), CUSTOMER_ID, Rating.of(4), "İyi", CLOCK);
        Clock later = Clock.offset(CLOCK, Duration.ofDays(2));

        review.edit(Rating.of(5), "  Çok iyi  ", later);

        assertThat(review.rating()).isEqualTo(Rating.of(5));
        assertThat(review.comment()).isEqualTo("Çok iyi");
        assertThat(review.createdAt()).isEqualTo(NOW);
        assertThat(review.updatedAt()).isEqualTo(NOW.plus(Duration.ofDays(2)));
    }

    @Test
    void commentLengthIsLimited() {
        String tooLong = "x".repeat(Review.MAX_COMMENT_LENGTH + 1);

        assertThatThrownBy(() -> Review.write(BookId.of(1), CUSTOMER_ID, Rating.of(3), tooLong, CLOCK))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void ratingMustBeBetweenOneAndFive() {
        assertThat(Rating.of(5)).isGreaterThan(Rating.of(1));
        assertThatThrownBy(() -> Rating.of(0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Rating.of(6)).isInstanceOf(ValidationException.class);
    }

    @Test
    void distributionCountsEachStarValue() {
        RatingDistribution distribution = RatingDistribution.of(List.of(
                Rating.of(5), Rating.of(5), Rating.of(4), Rating.of(1)));

        assertThat(distribution.total()).isEqualTo(4);
        assertThat(distribution.count(Rating.of(5))).isEqualTo(2);
        assertThat(distribution.count(Rating.of(3))).isZero();
        assertThat(distribution.percentage(Rating.of(5))).isEqualTo(50);
        assertThat(distribution.average()).contains(new BigDecimal("3.8"));
        assertThat(distribution).hasToString("RatingDistribution[1, 0, 0, 1, 2]");
    }

    @Test
    void emptyDistributionHasNoAverage() {
        RatingDistribution empty = RatingDistribution.of(List.of());

        assertThat(empty.total()).isZero();
        assertThat(empty.percentage(Rating.of(3))).isZero();
        assertThat(empty.average()).isEmpty();
        assertThat(empty).isEqualTo(RatingDistribution.of(List.of()));
    }
}
