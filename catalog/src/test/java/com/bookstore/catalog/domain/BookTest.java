package com.bookstore.catalog.domain;

import static com.bookstore.catalog.domain.CatalogTestData.author;
import static com.bookstore.catalog.domain.CatalogTestData.book;
import static com.bookstore.catalog.domain.CatalogTestData.bookBuilder;
import static com.bookstore.catalog.domain.CatalogTestData.novels;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;

class BookTest {

    @Test
    void builderCreatesABookWithDefaults() {
        Book book = Book.builder()
                .isbn(Isbn.parse("978-975-07-1938-7"))
                .title("  Kürk Mantolu Madonna ")
                .author(author(1, "Sabahattin Ali"))
                .category(novels())
                .price(Money.of("120"))
                .build();

        assertThat(book.isNew()).isTrue();
        assertThat(book.title()).isEqualTo("Kürk Mantolu Madonna");
        assertThat(book.language()).isEqualTo(Language.TURKISH);
        assertThat(book.isActive()).isTrue();
        assertThat(book.stock()).isEqualTo(Stock.EMPTY);
        assertThat(book.isAvailableForSale()).isFalse();
        assertThat(book.publicationYear()).isEmpty();
        assertThat(book.coverImage()).isEmpty();
        assertThat(book.ratings()).isEqualTo(RatingSummary.NONE);
    }

    @Test
    void builderRejectsMissingRequiredAttributes() {
        assertThatThrownBy(() -> Book.builder().title("No ISBN").build())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("isbn");
        assertThatThrownBy(() -> Book.builder().isbn(Isbn.parse("9789750719387")).title("t")
                .category(novels()).price(Money.of("10")).build())
                .hasMessageContaining("authors");
    }

    @Test
    void priceMustBePositive() {
        assertThatThrownBy(() -> bookBuilder(1, "Free", "0", 1).build())
                .hasMessageContaining("price");
        Book book = book(1, "Dune", "100", 1);
        assertThatThrownBy(() -> book.changePrice(Money.of("-1"))).isInstanceOf(ValidationException.class);
    }

    @Test
    void publicationYearMustBeAfterThePrintingPress() {
        assertThatThrownBy(() -> bookBuilder(1, "Old", "10", 1).publicationYear(Year.of(1200)).build())
                .hasMessageContaining("publicationYear");
    }

    @Test
    void authorsKeepTheirOrderAndAreNotDuplicated() {
        Author gaiman = author(1, "Neil Gaiman");
        Author pratchett = author(2, "Terry Pratchett");
        Book book = book(1, "Good Omens", "200", 1);

        book.changeAuthors(List.of(gaiman, pratchett, gaiman));

        assertThat(book.authors()).containsExactly(gaiman, pratchett);
        assertThat(book.authorNames()).isEqualTo("Neil Gaiman, Terry Pratchett");
        assertThatThrownBy(() -> book.authors().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reserveTakesCopiesOutOfStock() {
        Book book = book(1, "Dune", "100", 5);

        book.reserve(3);

        assertThat(book.stock().quantity()).isEqualTo(2);
    }

    @Test
    void reserveFailsWhenStockIsShortAndLeavesStockUnchanged() {
        Book book = book(1, "Dune", "100", 2);

        assertThatThrownBy(() -> book.reserve(3))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.requested()).isEqualTo(3);
                    assertThat(e.available()).isEqualTo(2);
                    assertThat(e.code()).isEqualTo(InsufficientStockException.CODE);
                });
        assertThat(book.stock().quantity()).isEqualTo(2);
    }

    @Test
    void inactiveBookCannotBeReserved() {
        Book book = book(1, "Dune", "100", 5);
        book.deactivate();

        assertThat(book.isAvailableForSale()).isFalse();
        assertThatThrownBy(() -> book.reserve(1))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("book.inactive"));
    }

    @Test
    void releaseAndChangeStock() {
        Book book = book(1, "Dune", "100", 1);

        book.release(4);
        assertThat(book.stock().quantity()).isEqualTo(5);

        book.changeStock(0);
        assertThat(book.isAvailableForSale()).isFalse();
        assertThatThrownBy(() -> book.changeStock(-1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void changeDetailsValidatesEverythingBeforeChangingAnything() {
        Book book = book(1, "Dune", "100", 1);

        assertThatThrownBy(() -> book.changeDetails("Dune Messiah", null, null, Language.ENGLISH, -5, null))
                .isInstanceOf(ValidationException.class);
        assertThat(book.title()).isEqualTo("Dune");

        book.changeDetails("Dune Messiah", "Ace", Year.of(1969), Language.ENGLISH, 256, "Book two");
        assertThat(book.title()).isEqualTo("Dune Messiah");
        assertThat(book.publicationYear()).contains(Year.of(1969));
    }

    @Test
    void ratingsAreAddedAndRemoved() {
        Book book = book(1, "Dune", "100", 1);

        book.addRating(5);
        book.addRating(4);
        book.addRating(4);
        assertThat(book.ratings().count()).isEqualTo(3);
        assertThat(book.ratings().average()).contains(new BigDecimal("4.3"));

        book.removeRating(5);
        assertThat(book.ratings().average()).contains(new BigDecimal("4.0"));
    }

    @Test
    void ratingSummaryRejectsImpossibleTotals() {
        assertThatThrownBy(() -> new RatingSummary(2, 11)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> RatingSummary.NONE.without(3)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> RatingSummary.NONE.with(6)).isInstanceOf(ValidationException.class);
        assertThat(RatingSummary.NONE.average()).isEmpty();
    }

    @Test
    void languageIsFoundByCode() {
        assertThat(Language.fromCode("EN")).isEqualTo(Language.ENGLISH);
        assertThatThrownBy(() -> Language.fromCode("zz")).isInstanceOf(ValidationException.class);
    }
}
