package com.bookstore.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.review.api.RatingBreakdown;
import com.bookstore.review.api.ReviewCommand;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.review.api.ReviewView;
import com.bookstore.review.persistence.InMemoryReviewRepository;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReviewServiceTest {

    private static final BookId DUNE = BookId.of(1);
    private static final CustomerId AYSE = CustomerId.of(10);
    private static final CustomerId MEHMET = CustomerId.of(11);

    private final InMemoryReviewRepository reviews = new InMemoryReviewRepository();
    private final ReviewFakes.FakeRatings ratings = new ReviewFakes.FakeRatings(DUNE);
    private final ReviewFakes.FakePurchases purchases = new ReviewFakes.FakePurchases();
    private final ReviewFakes.FakeNames names = new ReviewFakes.FakeNames(Map.of(AYSE, "Ayşe Y.", MEHMET, "Mehmet Ö."));
    private final DirectTransactions transactions = new DirectTransactions();
    private final ReviewService service =
            new ReviewService(reviews, ratings, purchases, names, transactions, TestClock.CLOCK);

    @Test
    void onlyCustomersWhoReceivedTheBookCanReviewIt() {
        assertThat(service.canReview(AYSE, DUNE)).isFalse();
        assertThatThrownBy(() -> write(AYSE, 5))
                .isInstanceOfSatisfying(AccessDeniedException.class,
                        e -> assertThat(e.code()).isEqualTo(ReviewFacade.NOT_ELIGIBLE));
        assertThat(ratings.count(DUNE)).isZero();
    }

    @Test
    void unknownBookIsReported() {
        purchases.received(AYSE, BookId.of(99));

        assertThatThrownBy(() -> service.write(new ReviewCommand(AYSE, BookId.of(99), 4, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void writingAReviewUpdatesTheBooksRating() {
        purchases.received(AYSE, DUNE);
        assertThat(service.canReview(AYSE, DUNE)).isTrue();

        ReviewView review = service.write(new ReviewCommand(AYSE, DUNE, 4, "Harika bir kitap"));

        assertThat(review.authorName()).isEqualTo("Ayşe Y.");
        assertThat(review.stars()).isEqualTo(4);
        assertThat(review.edited()).isFalse();
        assertThat(ratings.count(DUNE)).isEqualTo(1);
        assertThat(ratings.sum(DUNE)).isEqualTo(4);
        assertThat(service.ratingBreakdown(DUNE).count(4)).isEqualTo(1);
        assertThat(service.canReview(AYSE, DUNE)).isFalse();
    }

    @Test
    void aCustomerReviewsABookOnlyOnce() {
        purchases.received(AYSE, DUNE);
        write(AYSE, 4);

        assertThatThrownBy(() -> write(AYSE, 5))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(ReviewFacade.DUPLICATE));
        assertThat(ratings.count(DUNE)).isEqualTo(1);
    }

    @Test
    void editingMovesTheRatingInTheTotals() {
        purchases.received(AYSE, DUNE);
        write(AYSE, 2);

        ReviewView edited = new ReviewService(reviews, ratings, purchases, names, transactions,
                Clock.offset(TestClock.CLOCK, Duration.ofDays(1)))
                .edit(new ReviewCommand(AYSE, DUNE, 5, "Fikrim değişti"));

        assertThat(edited.stars()).isEqualTo(5);
        assertThat(edited.comment()).isEqualTo("Fikrim değişti");
        assertThat(edited.edited()).isTrue();
        assertThat(ratings.count(DUNE)).isEqualTo(1);
        assertThat(ratings.sum(DUNE)).isEqualTo(5);
        assertThat(service.myReview(AYSE, DUNE)).map(ReviewView::stars).contains(5);
    }

    @Test
    void deletingRemovesTheRatingFromTheTotals() {
        purchases.received(AYSE, DUNE);
        write(AYSE, 3);

        service.delete(AYSE, DUNE);

        assertThat(ratings.count(DUNE)).isZero();
        assertThat(service.myReview(AYSE, DUNE)).isEmpty();
        assertThat(service.canReview(AYSE, DUNE)).as("may review again").isTrue();
        assertThatThrownBy(() -> service.delete(AYSE, DUNE)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reviewsAreListedNewestFirstWithPublicNames() {
        purchases.received(AYSE, DUNE);
        purchases.received(MEHMET, DUNE);
        write(AYSE, 5);
        write(MEHMET, 3);

        Page<ReviewView> page = service.reviewsOf(DUNE, PageRequest.first());

        assertThat(page.content()).extracting(ReviewView::authorName).containsExactly("Mehmet Ö.", "Ayşe Y.");
        RatingBreakdown distribution = service.ratingBreakdown(DUNE);
        assertThat(distribution.total()).isEqualTo(2);
        assertThat(distribution.average()).isEqualByComparingTo("4.0");
    }

    @Test
    void invalidRatingIsRejectedBeforeAnythingHappens() {
        purchases.received(AYSE, DUNE);

        assertThatThrownBy(() -> write(AYSE, 6)).hasMessageContaining("rating");
        assertThat(transactions.started()).isZero();
    }

    private ReviewView write(CustomerId customerId, int stars) {
        return service.write(new ReviewCommand(customerId, DUNE, stars, null));
    }

}
