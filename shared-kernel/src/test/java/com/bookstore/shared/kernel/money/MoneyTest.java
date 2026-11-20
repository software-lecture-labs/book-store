package com.bookstore.shared.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void roundsToTwoDecimalPlacesHalfUp() {
        assertThat(Money.of("10.005").amount()).isEqualByComparingTo("10.01");
        assertThat(Money.of("10.004").amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of("7").amount().scale()).isEqualTo(2);
    }

    @Test
    void addsDecimalFractionsExactly() {
        Money sum = Money.of("0.10").plus(Money.of("0.20"));

        assertThat(sum).isEqualTo(Money.of("0.30"));
    }

    @Test
    void equalityIgnoresTrailingZerosBecauseScaleIsNormalised() {
        assertThat(Money.of(new BigDecimal("5.5"))).isEqualTo(Money.of("5.50"));
        assertThat(Money.of("5.5")).hasSameHashCodeAs(Money.of("5.50"));
    }

    @Test
    void multipliesByQuantityAndFactor() {
        assertThat(Money.of("49.90").times(3)).isEqualTo(Money.of("149.70"));
        assertThat(Money.of("100.00").times(new BigDecimal("0.185"))).isEqualTo(Money.of("18.50"));
    }

    @Test
    void subtractionMayGoNegative() {
        Money result = Money.of("10").minus(Money.of("12.50"));

        assertThat(result.isNegative()).isTrue();
        assertThat(result).isEqualTo(Money.of("-2.50"));
    }

    @Test
    void sumsAStreamAndTreatsEmptyAsZero() {
        assertThat(Money.sum(Stream.of(Money.of("1.25"), Money.of("2.75")))).isEqualTo(Money.of("4.00"));
        assertThat(Money.sum(Stream.empty())).isEqualTo(Money.ZERO);
    }

    @Test
    void comparesByValue() {
        assertThat(Money.of("10")).isGreaterThan(Money.of("9.99"));
        assertThat(Money.of("10").isAtLeast(Money.of("10.00"))).isTrue();
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.of("0.01").isPositive()).isTrue();
    }

    @Test
    void rejectsArithmeticAcrossCurrencies() {
        Money euros = new Money(BigDecimal.ONE, Currency.getInstance("EUR"));

        assertThatThrownBy(() -> Money.of("1").plus(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TRY")
                .hasMessageContaining("EUR");
    }

    @Test
    void rejectsTextThatIsNotANumber() {
        assertThatThrownBy(() -> Money.of("12,50"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void printsAmountAndCurrencyCode() {
        assertThat(Money.of("149.9")).hasToString("149.90 TRY");
    }
}
