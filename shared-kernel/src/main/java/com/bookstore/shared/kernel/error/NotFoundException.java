package com.bookstore.shared.kernel.error;

/**
 * Signals that a requested record does not exist. Its kind is {@link Kind#NOT_FOUND}.
 */
public class NotFoundException extends DomainException {

    /** Default error code for missing records. */
    public static final String CODE = "not-found";

    /**
     * Creates an exception for a record looked up by some key.
     *
     * @param type human-readable record type, for example {@code "Book"}
     * @param key  the identifier or key that was looked up
     */
    public NotFoundException(String type, Object key) {
        super(Kind.NOT_FOUND, CODE, type + " '" + key + "' was not found");
    }
}
