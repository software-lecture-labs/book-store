package com.bookstore.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.money.Money;

import org.junit.jupiter.api.Test;

class BookSearchQueryTest {

    @Test
    void rejectsAnInvertedPriceRange() {
        assertThatThrownBy(() -> BookSearchQuery.everything().priced(Money.of("100"), Money.of("50")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maxPrice");
    }

    @Test
    void anOpenEndedRangeIsFine() {
        assertThat(BookSearchQuery.everything().priced(null, Money.of("50")).maxPrice()).isEqualTo(Money.of("50"));
        assertThat(BookSearchQuery.everything().sortBy()).isEqualTo(BookSearchQuery.DEFAULT_SORT);
    }
}
