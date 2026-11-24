package com.bookstore.shared.kernel.error;

/**
 * Signals that a request is valid on its own but conflicts with the current state, for example an
 * e-mail address that is already registered or an illegal order status change. Its kind is
 * {@link Kind#CONFLICT}.
 */
public class ConflictException extends DomainException {

    /**
     * The code of a change made to an outdated copy: someone else changed the record since it was
     * read (optimistic locking, ADR-0015, ADR-0022).
     */
    public static final String CONCURRENT_MODIFICATION = "concurrent-modification";

    /**
     * Creates an exception with a specific error code.
     *
     * @param code    stable machine-readable code, for example {@code "email.taken"}
     * @param message human-readable description for logs
     */
    public ConflictException(String code, String message) {
        super(Kind.CONFLICT, code, message);
    }
}
