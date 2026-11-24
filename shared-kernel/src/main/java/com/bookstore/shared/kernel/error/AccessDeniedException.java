package com.bookstore.shared.kernel.error;

/**
 * Signals that the current user may not access a record that does exist, for example another
 * customer's order. Its kind is {@link Kind#FORBIDDEN}.
 */
public class AccessDeniedException extends DomainException {

    /** Default error code for ownership violations. */
    public static final String CODE = "access-denied";

    /**
     * Creates an exception with a description of what was refused.
     *
     * @param message what the user tried to access
     */
    public AccessDeniedException(String message) {
        super(Kind.FORBIDDEN, CODE, message);
    }

    /**
     * Creates an exception with a more specific error code.
     *
     * @param code    stable machine-readable code, for example {@code "review.not-eligible"}
     * @param message what the user tried to do
     */
    public AccessDeniedException(String code, String message) {
        super(Kind.FORBIDDEN, code, message);
    }
}
