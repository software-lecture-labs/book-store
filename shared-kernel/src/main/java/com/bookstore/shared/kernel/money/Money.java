package com.bookstore.shared.kernel.money;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.validation.Guard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An amount of money in a currency.
 *
 * <p>Amounts are always stored with {@value #SCALE} decimal places and rounded
 * {@link RoundingMode#HALF_UP half up}. Arithmetic between different currencies is rejected.
 * {@code double} and {@code float} are never used for money because they cannot represent most
 * decimal fractions exactly ({@code 0.1 + 0.2 != 0.3}).
 *
 * <p>It is {@link Serializable} because guest carts, which remember the prices a visitor saw, are
 * kept in the HTTP session.
 *
 * @param amount   the amount, rounded to two decimal places
 * @param currency the currency
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money>, Serializable {

    /** Number of decimal places kept for every amount. */
    public static final int SCALE = 2;

    /** Rounding applied whenever an amount has more than {@link #SCALE} decimal places. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** The currency used throughout the store. */
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("TRY");

    /** Zero in the default currency. */
    public static final Money ZERO = new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);

    /** Normalises the scale and rejects missing parts. */
    public Money {
        Guard.requireNonNull(amount, "amount");
        Guard.requireNonNull(currency, "currency");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    /**
     * Creates an amount in the default currency.
     *
     * @param amount the amount
     * @return the money value
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    /**
     * Parses an amount in the default currency, for example {@code "149.90"}.
     *
     * @param amount a decimal number with a dot as the decimal separator
     * @return the money value
     * @throws ValidationException if the text is not a number
     */
    public static Money of(String amount) {
        Guard.requireNonNull(amount, "amount");
        try {
            return of(new BigDecimal(amount.strip()));
        } catch (NumberFormatException e) {
            throw new ValidationException("amount", "is not a valid number: " + amount);
        }
    }

    /**
     * Adds up a stream of amounts. An empty stream sums to {@link #ZERO}.
     *
     * @param amounts the amounts, all in the same currency
     * @return the total
     */
    public static Money sum(Stream<Money> amounts) {
        return amounts.reduce(ZERO, Money::plus);
    }

    /**
     * Returns the sum of this amount and another.
     *
     * @param other an amount in the same currency
     * @return the sum
     */
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Returns this amount minus another.
     *
     * @param other an amount in the same currency
     * @return the difference, which may be negative
     */
    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * Returns this amount multiplied by a quantity, for example a unit price times the number of
     * copies.
     *
     * @param quantity the multiplier
     * @return the product
     */
    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    /**
     * Returns this amount multiplied by a decimal factor and rounded, for example a percentage.
     *
     * @param factor the multiplier
     * @return the rounded product
     */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        return new Money(amount.multiply(factor), currency);
    }

    /**
     * Tells whether the amount is exactly zero.
     *
     * @return {@code true} for zero
     */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    /**
     * Tells whether the amount is greater than zero.
     *
     * @return {@code true} for positive amounts
     */
    public boolean isPositive() {
        return amount.signum() > 0;
    }

    /**
     * Tells whether the amount is less than zero.
     *
     * @return {@code true} for negative amounts
     */
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /**
     * Tells whether this amount is greater than or equal to another.
     *
     * @param other an amount in the same currency
     * @return {@code true} if this amount is at least {@code other}
     */
    public boolean isAtLeast(Money other) {
        return compareTo(other) >= 0;
    }

    /**
     * Orders amounts by value. Only amounts in the same currency can be compared.
     *
     * @param other an amount in the same currency
     * @return a negative number, zero or a positive number
     */
    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    /**
     * Returns the amount and currency code, for example {@code "149.90 TRY"}. Formatting for
     * display is left to the web layer.
     *
     * @return a technical representation
     */
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency.getCurrencyCode() + " and " + other.currency.getCurrencyCode());
        }
    }
}
