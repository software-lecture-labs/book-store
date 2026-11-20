package com.bookstore.shared.kernel.validation;

import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Precondition checks for value objects and entities.
 *
 * <p>Every method either returns the (possibly normalised) value or throws a
 * {@link ValidationException} naming the field. This keeps constructors short:
 *
 * <pre>{@code
 * this.title = Guard.requireText(title, "title", 300);
 * }</pre>
 */
public final class Guard {

    private Guard() {
    }

    /**
     * Requires a non-null value.
     *
     * @param value the value to check
     * @param field field name used in the error
     * @param <T>   value type
     * @return the value
     */
    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new ValidationException(field, "is required");
        }
        return value;
    }

    /**
     * Requires text that is not blank and not longer than {@code maxLength} after trimming.
     *
     * @param value     the text to check
     * @param field     field name used in the error
     * @param maxLength maximum length after trimming
     * @return the trimmed text
     */
    public static String requireText(String value, String field, int maxLength) {
        requireNonNull(value, field);
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationException(field, "must not be blank");
        }
        return requireMaxLength(trimmed, field, maxLength);
    }

    /**
     * Normalises optional text: {@code null} or blank becomes an empty string, otherwise the
     * trimmed text must not be longer than {@code maxLength}.
     *
     * @param value     the text to check, may be {@code null}
     * @param field     field name used in the error
     * @param maxLength maximum length after trimming
     * @return the trimmed text, or an empty string
     */
    public static String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return "";
        }
        return requireMaxLength(value.strip(), field, maxLength);
    }

    /**
     * Requires text not longer than {@code maxLength}.
     *
     * @param value     the text to check
     * @param field     field name used in the error
     * @param maxLength maximum length
     * @return the text
     */
    public static String requireMaxLength(String value, String field, int maxLength) {
        requireNonNull(value, field);
        if (value.length() > maxLength) {
            throw new ValidationException(field, "must be at most " + maxLength + " characters");
        }
        return value;
    }

    /**
     * Requires text that matches a regular expression completely.
     *
     * @param value   the text to check
     * @param pattern the required format
     * @param field   field name used in the error
     * @param message what the format should be, used in the error
     * @return the text
     */
    public static String requireMatches(String value, Pattern pattern, String field, String message) {
        requireNonNull(value, field);
        if (!pattern.matcher(value).matches()) {
            throw new ValidationException(field, message);
        }
        return value;
    }

    /**
     * Requires a number greater than zero.
     *
     * @param value the number to check
     * @param field field name used in the error
     * @return the number
     */
    public static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new ValidationException(field, "must be greater than zero");
        }
        return value;
    }

    /**
     * Requires a number that is zero or greater.
     *
     * @param value the number to check
     * @param field field name used in the error
     * @return the number
     */
    public static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new ValidationException(field, "must not be negative");
        }
        return value;
    }

    /**
     * Requires a value inside a closed range. Works for any comparable type, so the same check
     * serves integers, years and money.
     *
     * @param value the value to check
     * @param min   lowest allowed value, inclusive
     * @param max   highest allowed value, inclusive
     * @param field field name used in the error
     * @param <T>   a type whose instances can be compared with each other
     * @return the value
     */
    public static <T extends Comparable<? super T>> T requireBetween(T value, T min, T max, String field) {
        requireNonNull(value, field);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ValidationException(field, "must be between " + min + " and " + max);
        }
        return value;
    }

    /**
     * Requires a collection with at least one element and no {@code null} elements.
     *
     * @param values the collection to check
     * @param field  field name used in the error
     * @param <C>    collection type
     * @return the collection
     */
    public static <C extends Collection<?>> C requireNotEmpty(C values, String field) {
        requireNonNull(values, field);
        if (values.isEmpty()) {
            throw new ValidationException(field, "must not be empty");
        }
        for (Object value : values) {
            if (value == null) {
                throw new ValidationException(field, "must not contain empty entries");
            }
        }
        return values;
    }

    /**
     * Requires a condition that the caller has already evaluated.
     *
     * @param condition the condition that must hold
     * @param field     field name used in the error
     * @param message   what is wrong when the condition does not hold
     */
    public static void require(boolean condition, String field, String message) {
        if (!condition) {
            throw new ValidationException(field, message);
        }
    }
}
