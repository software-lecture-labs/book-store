package com.bookstore.shared.web;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The correlation id of the request being handled (ADR-0022): taken from the client's
 * {@value #HEADER} header when it is well-formed, otherwise made up. It is answered in the same
 * header and in every problem, and unexpected failures are logged with it, so what a user reports
 * can be found in the log.
 *
 * <p>The id of the current request is kept per thread from the request filter to the response
 * filter; JAX-RS handles a request, its exception mapper and its response filters on one thread.
 */
public final class CorrelationIds {

    /** The request and response header. */
    public static final String HEADER = "X-Correlation-Id";

    /** The member of a problem that carries the id. */
    public static final String PROBLEM_MEMBER = "correlationId";

    private static final Pattern WELL_FORMED = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIds() {
    }

    /**
     * Chooses the id for a request.
     *
     * @param fromClient the client's header value, or {@code null}
     * @return the client's id if it is well-formed, otherwise a new random one
     */
    public static String accept(String fromClient) {
        return fromClient != null && WELL_FORMED.matcher(fromClient).matches()
                ? fromClient
                : UUID.randomUUID().toString();
    }

    /** @return the id of the request this thread handles, or {@code null} outside a request */
    public static String current() {
        return CURRENT.get();
    }

    /** @param id the id of the request this thread starts handling */
    static void begin(String id) {
        CURRENT.set(id);
    }

    /** Forgets the id once the request is answered. */
    static void end() {
        CURRENT.remove();
    }
}
