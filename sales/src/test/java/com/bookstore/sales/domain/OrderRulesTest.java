package com.bookstore.sales.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.time.Year;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class OrderRulesTest {

    @ParameterizedTest
    @CsvSource({
            "CREATED, PAID, true",
            "CREATED, CANCELLED, true",
            "CREATED, SHIPPED, false",
            "PAID, SHIPPED, true",
            "PAID, CANCELLED, true",
            "PAID, CREATED, false",
            "SHIPPED, DELIVERED, true",
            "SHIPPED, CANCELLED, false",
            "DELIVERED, CANCELLED, false",
    })
    void statusTransitions(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
    void finalStatusesAllowNothing(OrderStatus status) {
        assertThat(status.isFinal()).isTrue();
        assertThat(status.allowedNext()).isEmpty();
    }

    @Test
    void allowedNextIsUnmodifiable() {
        assertThatThrownBy(() -> OrderStatus.CREATED.allowedNext().add(OrderStatus.DELIVERED))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void orderNumberIsFormattedAndParsed() {
        OrderNumber number = OrderNumber.of(Year.of(2026), 42);

        assertThat(number.value()).isEqualTo("BS-2026-000042");
        assertThat(number.year()).isEqualTo(Year.of(2026));
        assertThat(number.sequence()).isEqualTo(42);
        assertThat(new OrderNumber("BS-2026-000042")).isEqualTo(number);
    }

    @Test
    void orderNumberRejectsBadInput() {
        assertThatThrownBy(() -> new OrderNumber("BS-26-42")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> OrderNumber.of(Year.of(2026), 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> OrderNumber.of(Year.of(2026), 1_000_000)).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @CsvSource({"0.01, 49.90", "499.99, 49.90", "500.00, 0.00", "1250.00, 0.00"})
    void standardShippingIsFreeFromFiveHundred(String subtotal, String expected) {
        assertThat(ShippingPolicy.standard().costFor(Money.of(subtotal))).isEqualTo(Money.of(expected));
    }

    @Test
    void shippingPolicyCanBeSwapped() {
        ShippingPolicy free = ShippingPolicy.alwaysFree();
        ShippingPolicy fivePercent = subtotal -> subtotal.times(new java.math.BigDecimal("0.05"));

        assertThat(free.costFor(Money.of("10"))).isEqualTo(Money.ZERO);
        assertThat(fivePercent.costFor(Money.of("200"))).isEqualTo(Money.of("10.00"));
    }

    @Test
    void orderLineRejectsInvalidParts() {
        assertThat(new OrderLine(BookId.of(1), "Dune", Money.of("12.50"), 4).lineTotal()).isEqualTo(Money.of("50.00"));

        assertThatThrownBy(() -> new OrderLine(BookId.of(1), "Dune", Money.ZERO, 1)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new OrderLine(BookId.of(1), "Dune", Money.of("1"), 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new OrderLine(null, "Dune", Money.of("1"), 1)).isInstanceOf(ValidationException.class);
    }
}
