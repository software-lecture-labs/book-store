package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Arrays;

/**
 * The language a book is written in.
 */
public enum Language {

    /** Turkish. */
    TURKISH("tr"),

    /** English. */
    ENGLISH("en"),

    /** German. */
    GERMAN("de"),

    /** French. */
    FRENCH("fr"),

    /** Spanish. */
    SPANISH("es"),

    /** Russian. */
    RUSSIAN("ru"),

    /** Any other language. */
    OTHER("xx");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    /**
     * Returns the two-letter code, as stored in the database.
     *
     * @return the code, for example {@code "tr"}
     */
    public String code() {
        return code;
    }

    /**
     * Finds the language for a two-letter code.
     *
     * @param code the code, case-insensitive
     * @return the language
     * @throws ValidationException if no language has that code
     */
    public static Language fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new ValidationException("language", "unknown language code: " + code));
    }
}
