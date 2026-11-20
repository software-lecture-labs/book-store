package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.validation.Guard;

import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The number printed on an order, for example {@code BS-2026-000042}: a prefix, the year and a
 * six-digit sequence that restarts every year.
 *
 * @param value the formatted number
 */
public record OrderNumber(String value) {

    /** Largest sequence number that fits in six digits. */
    public static final long MAX_SEQUENCE = 999_999;

    private static final Pattern FORMAT = Pattern.compile("BS-(\\d{4})-(\\d{6})");

    /** Checks the format. */
    public OrderNumber {
        Guard.requireMatches(value, FORMAT, "orderNumber", "must look like BS-2026-000042");
    }

    /**
     * Builds the number for a year and a sequence.
     *
     * @param year     the year the order was placed
     * @param sequence the order's position in that year, from 1
     * @return the order number
     */
    public static OrderNumber of(Year year, long sequence) {
        Guard.requireBetween(sequence, 1L, MAX_SEQUENCE, "sequence");
        return new OrderNumber(String.format("BS-%04d-%06d", year.getValue(), sequence));
    }

    /**
     * Returns the year part.
     *
     * @return the year
     */
    public Year year() {
        return Year.of(Integer.parseInt(parts().group(1)));
    }

    /**
     * Returns the sequence part.
     *
     * @return the sequence, from 1
     */
    public long sequence() {
        return Long.parseLong(parts().group(2));
    }

    @Override
    public String toString() {
        return value;
    }

    private Matcher parts() {
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalStateException("unreachable: format was checked in the constructor");
        }
        return matcher;
    }
}
