package com.bookstore.shared.kernel.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import org.junit.jupiter.api.Test;

class IdentifierTest {

    @Test
    void idsAreEqualByValueAndTypeOnly() {
        assertThat(BookId.of(7)).isEqualTo(new BookId(7)).isNotEqualTo(new BookId(8));
        assertThat((Object) BookId.of(7)).isNotEqualTo(CustomerId.of(7));
    }

    @Test
    void zeroAndNegativeValuesAreRejected() {
        assertThatThrownBy(() -> new BookId(0)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("BookId");
        assertThatThrownBy(() -> new CustomerId(-1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void anIdPrintsAsItsValue() {
        assertThat(BookId.of(42)).hasToString("42");
    }
}
