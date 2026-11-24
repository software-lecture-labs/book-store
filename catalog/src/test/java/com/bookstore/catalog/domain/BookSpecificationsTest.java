package com.bookstore.catalog.domain;

import static com.bookstore.catalog.domain.BookSpecifications.active;
import static com.bookstore.catalog.domain.BookSpecifications.byAuthor;
import static com.bookstore.catalog.domain.BookSpecifications.inCategories;
import static com.bookstore.catalog.domain.BookSpecifications.inLanguage;
import static com.bookstore.catalog.domain.BookSpecifications.inStock;
import static com.bookstore.catalog.domain.BookSpecifications.matchingText;
import static com.bookstore.catalog.domain.BookSpecifications.pricedFrom;
import static com.bookstore.catalog.domain.BookSpecifications.pricedUpTo;
import static com.bookstore.catalog.domain.CatalogTestData.author;
import static com.bookstore.catalog.domain.CatalogTestData.bookBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.specification.Specification;

import java.time.Year;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BookSpecificationsTest {

    private final Book madonna = bookBuilder(1, "Kürk Mantolu Madonna", "120", 5)
            .author(author(10, "Sabahattin Ali"))
            .publicationYear(Year.of(1943))
            .build();
    private final Book dune = bookBuilder(2, "Dune", "350", 0)
            .language(Language.ENGLISH)
            .publicationYear(Year.of(1965))
            .build();
    private final Book calikusu = bookBuilder(3, "Çalıkuşu", "90", 2).build();
    private final Book hidden = bookBuilder(4, "Hidden", "50", 9).active(false).build();
    private final List<Book> books = List.of(madonna, dune, calikusu, hidden);

    @Test
    void activeMatchesEveryBookStillSold() {
        assertThat(search(active())).containsExactlyInAnyOrder(madonna, dune, calikusu);
        assertThat(search(Specification.all())).contains(hidden);
    }

    @Test
    void stockBelowFindsBooksRunningLowAndNeedsAPositiveLimit() {
        assertThat(search(BookSpecifications.stockBelow(3))).containsExactlyInAnyOrder(dune, calikusu);
        assertThatThrownBy(() -> BookSpecifications.stockBelow(0)).isInstanceOf(ValidationException.class);
    }

    @Test
    void textMatchesTitlesIgnoringCaseAndTurkishLetters() {
        assertThat(search(matchingText("CALIKUSU"))).containsExactly(calikusu);
        assertThat(search(matchingText("mantolu"))).containsExactly(madonna);
    }

    @Test
    void textMatchesAuthorNames() {
        assertThat(search(matchingText("sabahattın"))).containsExactly(madonna);
    }

    @Test
    void textMatchesIsbnFragmentsOfFourOrMoreDigits() {
        String isbn = dune.isbn().value();

        assertThat(search(matchingText(isbn))).containsExactly(dune);
        assertThat(search(matchingText("978"))).isEmpty();
    }

    @Test
    void blankTextMatchesEverything() {
        assertThat(matchingText("   ")).isEqualTo(Specification.all());
        assertThat(matchingText(null)).isEqualTo(Specification.all());
    }

    @Test
    void overLongTextIsRejected() {
        String tooLong = "x".repeat(BookSpecifications.MAX_TEXT_LENGTH + 1);

        assertThatThrownBy(() -> matchingText(tooLong)).isInstanceOf(ValidationException.class);
    }

    @Test
    void filtersByPriceLanguageStockAndCategory() {
        assertThat(search(pricedFrom(Money.of("100")).and(pricedUpTo(Money.of("350")))))
                .containsExactlyInAnyOrder(madonna, dune);
        assertThat(search(inLanguage(Language.ENGLISH))).containsExactly(dune);
        assertThat(search(active().and(inStock()))).containsExactlyInAnyOrder(madonna, calikusu);
        assertThat(search(inCategories(Set.of(CategoryId.of(2))))).hasSize(4);
        assertThat(search(inCategories(Set.of(CategoryId.of(99))))).isEmpty();
    }

    @Test
    void filtersByAuthor() {
        assertThat(search(byAuthor(AuthorId.of(10)))).containsExactly(madonna);
        assertThat(search(byAuthor(AuthorId.of(102)))).containsExactly(dune);
        assertThat(search(byAuthor(AuthorId.of(999)))).isEmpty();
    }

    @Test
    void rulesCombineWithOrAndNot() {
        assertThat(search(inLanguage(Language.ENGLISH).or(byAuthor(AuthorId.of(10)))))
                .containsExactlyInAnyOrder(dune, madonna);
        assertThat(search(active().and(inStock().not()))).containsExactly(dune);
    }

    @Test
    void rulesThatNeedAValueRejectAMissingOne() {
        assertThatThrownBy(() -> inCategories(Set.of())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> byAuthor(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pricedFrom(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void sortsByPriceInBothDirections() {
        assertThat(sorted(new BookSort(BookSortField.PRICE, SortOrder.ASC))).containsExactly(calikusu, madonna, dune);
        assertThat(sorted(new BookSort(BookSortField.PRICE, SortOrder.DESC))).containsExactly(dune, madonna, calikusu);
    }

    @Test
    void sortsByTitleIgnoringTurkishLetters() {
        assertThat(sorted(BookSort.DEFAULT)).containsExactly(calikusu, dune, madonna);
    }

    @Test
    void sortsByYearWithUnknownYearsFirst() {
        assertThat(sorted(new BookSort(BookSortField.PUBLICATION_YEAR, SortOrder.ASC)))
                .containsExactly(calikusu, madonna, dune);
    }

    @Test
    void sortsByRatingAndBreaksTiesByTitle() {
        madonna.addRating(5);
        dune.addRating(3);

        assertThat(sorted(new BookSort(BookSortField.RATING, SortOrder.DESC))).containsExactly(madonna, dune, calikusu);
        assertThat(sorted(new BookSort(BookSortField.RATING, SortOrder.ASC))).containsExactly(calikusu, dune, madonna);
    }

    private List<Book> search(Specification<Book> rule) {
        return books.stream().filter(rule::isSatisfiedBy).toList();
    }

    private List<Book> sorted(BookSort sort) {
        return books.stream().filter(Book::isActive).sorted(sort.comparator()).toList();
    }
}
