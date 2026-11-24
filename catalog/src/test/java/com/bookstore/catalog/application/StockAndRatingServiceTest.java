package com.bookstore.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.api.BookOffer;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.InsufficientStockException;
import com.bookstore.catalog.domain.RatingSummary;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** The catalog's facades for the sales and review modules. */
class StockAndRatingServiceTest {

    private final CatalogShop shop = new CatalogShop();
    private final Book dune = shop.book("Dune", "100.00", 10);
    private final Book madonna = shop.book("Kürk Mantolu Madonna", "45.50", 3);

    @Test
    void offersCopyTheCurrentPriceStockAndStatus() {
        madonna.deactivate();
        shop.books.save(madonna);

        List<BookOffer> offers = shop.stock.offers(List.of(dune.id(), madonna.id(), BookId.of(999)));

        assertThat(offers).containsExactlyInAnyOrder(
                new BookOffer(dune.id(), "Dune", "Author of Dune", Money.of("100.00"), 10, true),
                new BookOffer(madonna.id(), "Kürk Mantolu Madonna", "Author of Kürk Mantolu Madonna",
                        Money.of("45.50"), 3, false));
    }

    @Test
    void reserveTakesStockFromEveryBook() {
        shop.stock.reserve(Map.of(dune.id(), 4, madonna.id(), 3));

        assertThat(stockOf(dune)).isEqualTo(6);
        assertThat(stockOf(madonna)).isZero();
    }

    @Test
    void reserveIsAllOrNothing() {
        assertThatThrownBy(() -> shop.stock.reserve(Map.of(dune.id(), 4, madonna.id(), 4)))
                .isInstanceOfSatisfying(InsufficientStockException.class,
                        e -> assertThat(e.bookTitle()).isEqualTo("Kürk Mantolu Madonna"));

        assertThat(stockOf(dune)).as("no stock taken from the book that had enough").isEqualTo(10);
        assertThat(stockOf(madonna)).isEqualTo(3);
    }

    @Test
    void reserveRefusesInactiveAndMissingBooks() {
        madonna.deactivate();
        shop.books.save(madonna);

        assertThatThrownBy(() -> shop.stock.reserve(Map.of(madonna.id(), 1)))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("book.inactive"));
        assertThatThrownBy(() -> shop.stock.reserve(Map.of(BookId.of(999), 1))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void releasePutsStockBack() {
        shop.stock.reserve(Map.of(dune.id(), 4));

        shop.stock.release(Map.of(dune.id(), 4));

        assertThat(stockOf(dune)).isEqualTo(10);
    }

    @Test
    void ratingsChangeTheBooksTotals() {
        shop.ratings.addRating(dune.id(), 5);
        shop.ratings.addRating(dune.id(), 2);
        shop.ratings.removeRating(dune.id(), 5);

        assertThat(shop.books.findById(dune.id()).orElseThrow().ratings()).isEqualTo(new RatingSummary(1, 2));
        assertThat(shop.catalog.book(dune.id()).averageRating()).isEqualTo(new BigDecimal("2.0"));
        assertThat(shop.ratings.exists(dune.id())).isTrue();
        assertThat(shop.ratings.exists(BookId.of(999))).isFalse();
        assertThatThrownBy(() -> shop.ratings.addRating(BookId.of(999), 3)).isInstanceOf(NotFoundException.class);
    }

    private int stockOf(Book book) {
        return shop.books.findById(book.id()).orElseThrow().stock().quantity();
    }
}
