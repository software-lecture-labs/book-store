package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.validation.Guard;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An e-mail address, trimmed and lower-cased so that {@code Ali@Example.com} and
 * {@code ali@example.com} are the same customer.
 *
 * @param value the normalised address
 */
public record Email(String value) {

    /** Longest address allowed by the e-mail standards. */
    public static final int MAX_LENGTH = 254;

    /**
     * A deliberately simple format check: something, an at sign, a domain with at least one dot.
     * Whether the mailbox really exists can only be found out by sending mail to it.
     */
    private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+");

    /** Normalises and checks the address. */
    public Email {
        value = Guard.requireText(value, "email", MAX_LENGTH).toLowerCase(Locale.ROOT);
        Guard.requireMatches(value, FORMAT, "email", "is not a valid e-mail address");
    }

    /**
     * Parses an address as typed by a person.
     *
     * @param text the address
     * @return the normalised address
     */
    public static Email of(String text) {
        return new Email(text);
    }

    @Override
    public String toString() {
        return value;
    }
}
