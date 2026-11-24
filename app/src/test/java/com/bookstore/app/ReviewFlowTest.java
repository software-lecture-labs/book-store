package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.domain.Book;
import com.bookstore.customer.domain.Customer;
import com.bookstore.review.api.ReviewCommand;
import com.bookstore.review.api.ReviewView;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.shared.kernel.error.AccessDeniedException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * A review touches three other modules: sales says whether the customer received the book, the
 * catalog keeps the rating totals and the customer module gives the author's public name. This
 * test runs that path on the real database.
 */
class ReviewFlowTest {

    private final JdbcBookstore store = new JdbcBookstore();
    private final Book dune = store.catalogDb.book("Dune", "100.00", 5);
    private final Customer ayse = store.customerDb.customer("ayse@example.com");

    @Test
    void aDeliveredBookCanBeReviewedAndTheCatalogShowsTheRating() {
        assertThatThrownBy(() -> review(5)).isInstanceOf(AccessDeniedException.class);

        String number = buy();
        store.orderAdmin.changeStatus(number, "SHIPPED");
        assertThatThrownBy(() -> review(5)).as("shipped is not delivered").isInstanceOf(AccessDeniedException.class);
        store.orderAdmin.changeStatus(number, "DELIVERED");

        assertThat(review(4).authorName()).isEqualTo("Ayşe Y.");

        assertThat(store.catalog.book(dune.id()).averageRating()).isEqualTo(new BigDecimal("4.0"));
        assertThat(store.catalog.book(dune.id()).ratingCount()).isEqualTo(1);
        assertThat(store.reviews.ratingBreakdown(dune.id()).count(4)).isEqualTo(1);
    }

    @Test
    void deletingTheReviewRemovesTheRatingFromTheCatalog() {
        String number = buy();
        store.orderAdmin.changeStatus(number, "SHIPPED");
        store.orderAdmin.changeStatus(number, "DELIVERED");
        review(2);

        store.reviews.delete(ayse.id(), dune.id());

        assertThat(store.catalog.book(dune.id()).ratingCount()).isZero();
        assertThat(store.catalog.book(dune.id()).averageRating()).isNull();
    }

    private String buy() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 1);
        long addressId = ayse.defaultAddress().orElseThrow().id().value();
        return store.checkout.placeOrder(new PlaceOrderCommand(ayse.id(), addressId, "CREDIT_CARD"))
                .orderNumber();
    }

    private ReviewView review(int stars) {
        return store.reviews.write(new ReviewCommand(ayse.id(), dune.id(), stars, null));
    }
}
