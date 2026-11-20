package com.bookstore.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.review.api.ReviewCommand;
import com.bookstore.review.api.ReviewView;
import com.bookstore.review.persistence.InMemoryReviewRepository;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestClock;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ReviewAdminServiceTest {

    private static final BookId DUNE = BookId.of(1);
    private static final CustomerId AYSE = CustomerId.of(10);
    private static final CustomerId MEHMET = CustomerId.of(11);

    private final InMemoryReviewRepository reviews = new InMemoryReviewRepository();
    private final ReviewFakes.FakeRatings ratings = new ReviewFakes.FakeRatings(DUNE);
    private final ReviewFakes.FakePurchases purchases = new ReviewFakes.FakePurchases();
    private final ReviewFakes.FakeNames names = new ReviewFakes.FakeNames(Map.of(AYSE, "Ayşe Y."));
    private final DirectTransactions transactions = new DirectTransactions();
    private final ReviewService customers =
            new ReviewService(reviews, ratings, purchases, names, transactions, TestClock.CLOCK);
    private final ReviewAdminService admin = new ReviewAdminService(reviews, ratings, names, transactions);

    @Test
    void everyReviewIsListedWithItsAuthorsName() {
        write(AYSE, 5);
        write(MEHMET, 2);

        assertThat(admin.reviews(PageRequest.first()).content()).extracting(ReviewView::authorName)
                .containsExactlyInAnyOrder("Ayşe Y.", ReviewMapper.UNKNOWN_AUTHOR);
    }

    @Test
    void aRemovedReviewLeavesTheAverageToo() {
        ReviewView unsuitable = write(AYSE, 1);
        write(MEHMET, 5);

        admin.remove(unsuitable.id());

        assertThat(admin.reviews(PageRequest.first()).content()).extracting(ReviewView::stars).containsExactly(5);
        assertThat(ratings.count(DUNE)).isEqualTo(1);
        assertThat(ratings.sum(DUNE)).isEqualTo(5);
        assertThatThrownBy(() -> admin.remove(unsuitable.id())).isInstanceOf(NotFoundException.class);
    }

    private ReviewView write(CustomerId author, int stars) {
        purchases.received(author, DUNE);
        return customers.write(new ReviewCommand(author, DUNE, stars, "Comment of " + author));
    }
}
