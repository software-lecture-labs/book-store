package com.bookstore.shared.web;

import java.util.Locale;

/**
 * Reads path and query parameters that arrive as text. JAX-RS answers a parameter it cannot
 * convert with a 404; the API wants a 400 problem that names the parameter, so resources take
 * parameters as {@code String} and read them here.
 */
public final class Params {

    private Params() {
    }

    /**
     * Reads a required id.
     *
     * @param name  the parameter's name, for the error
     * @param value the text
     * @return the id, greater than zero
     * @throws WebException 400 if it is not a positive whole number
     */
    public static long id(String name, String value) {
        try {
            long number = Long.parseLong(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw WebException.badRequest("request.bad-parameter", name + " must be a positive whole number");
    }

    /**
     * Reads an optional id.
     *
     * @param name  the parameter's name
     * @param value the text, may be {@code null} or blank
     * @return the id, or {@code null}
     * @throws WebException 400 if it is present but not a positive whole number
     */
    public static Long optionalId(String name, String value) {
        return isBlank(value) ? null : id(name, value);
    }

    /**
     * Reads an optional whole number.
     *
     * @param name     the parameter's name
     * @param value    the text, may be {@code null}
     * @param fallback the value when it is missing
     * @return the number
     * @throws WebException 400 if it is not a whole number
     */
    public static int integer(String name, String value, int fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw WebException.badRequest("request.bad-parameter", name + " must be a whole number");
        }
    }

    /**
     * Reads an optional yes/no flag.
     *
     * @param name  the parameter's name
     * @param value the text, may be {@code null}
     * @return {@code true} only for {@code true}
     * @throws WebException 400 for anything other than {@code true} or {@code false}
     */
    public static boolean flag(String name, String value) {
        if (isBlank(value)) {
            return false;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw WebException.badRequest("request.bad-parameter", name + " must be true or false");
        };
    }

    /**
     * Returns a value, or {@code null} if it is blank.
     *
     * @param value the text
     * @return the text, or {@code null}
     */
    public static String optional(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
