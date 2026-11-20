package com.bookstore.shared.kernel.error;

import java.util.Objects;

/**
 * Root of every business error raised by the application (ADR-0004).
 *
 * <p>It is unchecked so that use cases do not have to declare it, and abstract so that callers
 * always throw one of the specific subtypes. Each instance carries
 *
 * <ul>
 *   <li>a {@link Kind}, which says what sort of failure it is without naming a protocol; the HTTP
 *       layer turns it into a status code (ADR-0009), and</li>
 *   <li>a short, stable machine-readable {@link #code() code} (for example
 *       {@code "order.illegal-transition"}) that clients use to pick a translated message.</li>
 * </ul>
 *
 * <p>Codes are part of the public contract: once released, a code is never renamed.
 */
public abstract class DomainException extends RuntimeException {

    /** What sort of failure an exception reports, independent of any protocol. */
    public enum Kind {

        /** The requested record does not exist (HTTP 404). */
        NOT_FOUND,

        /** The request clashes with the current state: a duplicate, a stale version, an illegal move (HTTP 409). */
        CONFLICT,

        /** The request breaks a business rule (HTTP 422). */
        RULE_VIOLATION,

        /** The caller may not do this to a record that does exist (HTTP 403). */
        FORBIDDEN
    }

    private final Kind kind;
    private final String code;

    /**
     * Creates an exception with a kind, an error code and an English developer-facing message.
     *
     * @param kind    what sort of failure this is
     * @param code    stable machine-readable error code, never blank
     * @param message human-readable description for logs
     */
    protected DomainException(Kind kind, String code, String message) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = requireCode(code);
    }

    /**
     * Creates an exception that wraps a lower-level cause.
     *
     * @param kind    what sort of failure this is
     * @param code    stable machine-readable error code, never blank
     * @param message human-readable description for logs
     * @param cause   the underlying technical error
     */
    protected DomainException(Kind kind, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = requireCode(code);
    }

    /**
     * Returns what sort of failure this is.
     *
     * @return the kind
     */
    public final Kind kind() {
        return kind;
    }

    /**
     * Returns the stable machine-readable error code.
     *
     * @return the error code
     */
    public final String code() {
        return code;
    }

    private static String requireCode(String code) {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        return code;
    }
}
