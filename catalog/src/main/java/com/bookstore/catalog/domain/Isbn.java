package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.validation.Guard;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An International Standard Book Number, always stored in its 13-digit form without separators.
 *
 * <p>{@link #parse(String)} accepts the forms people type: with or without hyphens and spaces, and
 * the older 10-digit form, which it converts to 13 digits.
 *
 * @param value thirteen digits with a valid check digit, for example {@code "9789750719387"}
 */
public record Isbn(String value) {

    private static final Pattern THIRTEEN_DIGITS = Pattern.compile("97[89]\\d{10}");
    private static final Pattern TEN_CHARACTERS = Pattern.compile("\\d{9}[\\dX]");
    private static final String FIELD = "isbn";

    /** Requires the canonical 13-digit form with a correct check digit. */
    public Isbn {
        Guard.requireMatches(value, THIRTEEN_DIGITS, FIELD, "must be 13 digits starting with 978 or 979");
        int[] digits = digitsOf(value);
        if (digits[12] != isbn13CheckDigit(digits)) {
            throw new ValidationException(FIELD, "has an invalid check digit");
        }
    }

    /**
     * Parses an ISBN as typed by a person.
     *
     * @param text an ISBN-13 or ISBN-10, hyphens and spaces allowed
     * @return the ISBN in 13-digit form
     * @throws ValidationException if the text is not a valid ISBN
     */
    public static Isbn parse(String text) {
        Guard.requireNonNull(text, FIELD);
        String compact = text.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (compact.length() == 10) {
            return new Isbn(convertIsbn10(compact));
        }
        return new Isbn(compact);
    }

    /**
     * Formats the ISBN as {@code 978-9750719387}. Real ISBNs have variable-length groups that need a
     * registry to split correctly, so only the prefix is separated.
     *
     * @return the ISBN with the prefix separated
     */
    public String formatted() {
        return value.substring(0, 3) + "-" + value.substring(3);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String convertIsbn10(String isbn10) {
        Guard.requireMatches(isbn10, TEN_CHARACTERS, FIELD, "must be 10 or 13 digits");
        if (isbn10CheckValue(isbn10) % 11 != 0) {
            throw new ValidationException(FIELD, "has an invalid check digit");
        }
        String body = "978" + isbn10.substring(0, 9);
        int[] digits = digitsOf(body + "0");
        return body + isbn13CheckDigit(digits);
    }

    /**
     * ISBN-10 check: the sum of each digit times its weight (10 down to 1) must be divisible by 11.
     * The last character may be {@code X}, meaning 10.
     */
    private static int isbn10CheckValue(String isbn10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn10.charAt(i);
            int digit = c == 'X' ? 10 : c - '0';
            sum += digit * (10 - i);
        }
        return sum;
    }

    /**
     * ISBN-13 check digit: weight the first twelve digits alternately 1 and 3, and pick the digit
     * that brings the sum up to a multiple of 10.
     */
    private static int isbn13CheckDigit(int[] digits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += digits[i] * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10;
    }

    private static int[] digitsOf(String text) {
        int[] digits = new int[text.length()];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = text.charAt(i) - '0';
        }
        return digits;
    }
}
