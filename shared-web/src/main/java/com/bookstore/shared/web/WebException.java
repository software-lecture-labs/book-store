package com.bookstore.shared.web;

import java.util.List;
import java.util.Map;

/**
 * An error of the HTTP layer itself, not of the business: a malformed request, a missing
 * credential, an unknown path. Business errors are {@code DomainException}s and get their status
 * from their kind (ADR-0009).
 */
public final class WebException extends RuntimeException {

    private final int status;
    private final String code;
    private final List<Problem.FieldError> errors;
    private final Map<String, Object> extensions;

    private WebException(int status, String code, String message, List<Problem.FieldError> errors) {
        this(status, code, message, errors, Map.of());
    }

    private WebException(int status, String code, String message, List<Problem.FieldError> errors,
                         Map<String, Object> extensions) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
        this.extensions = Map.copyOf(extensions);
    }

    /**
     * A business conflict that the client needs more than a code to resolve, for example the list of
     * corrections a cart went through. The members are added to the problem (RFC 9457 extensions).
     *
     * @param code       the stable code
     * @param message    what happened
     * @param extensions the extra members
     * @return the exception
     */
    public static WebException conflict(String code, String message, Map<String, Object> extensions) {
        return new WebException(HttpStatus.CONFLICT, code, message, List.of(), extensions);
    }

    /**
     * The request's format is wrong: unreadable JSON, a missing or malformed parameter.
     *
     * @param code    the stable code, for example {@code "request.malformed-json"}
     * @param message what is wrong
     * @return the exception
     */
    public static WebException badRequest(String code, String message) {
        return new WebException(HttpStatus.BAD_REQUEST, code, message, List.of());
    }

    /**
     * Fields of the request failed Bean Validation.
     *
     * @param errors every invalid field
     * @return the exception
     */
    public static WebException invalid(List<Problem.FieldError> errors) {
        return new WebException(HttpStatus.BAD_REQUEST, "request.invalid", "The request has invalid fields", errors);
    }

    /** @return the exception for a request without valid credentials */
    public static WebException unauthenticated() {
        return new WebException(HttpStatus.UNAUTHORIZED, "authentication.required",
                "Sign in to use this endpoint", List.of());
    }

    /** @return the exception for a sign-in with a wrong e-mail address, password or a disabled account */
    public static WebException signInFailed() {
        return new WebException(HttpStatus.UNAUTHORIZED, "authentication.failed",
                "The e-mail address or the password is wrong", List.of());
    }

    /** @return the exception for a cookie-session request that changes state without the client header (ADR-0020) */
    public static WebException clientHeaderMissing() {
        return new WebException(HttpStatus.FORBIDDEN, "csrf.header-missing",
                "Requests signed in with the session cookie must carry the X-Bookstore-Client header", List.of());
    }

    /** @return the exception for a change whose {@code If-Match} names an outdated version (ADR-0022) */
    public static WebException preconditionFailed() {
        return new WebException(HttpStatus.PRECONDITION_FAILED, "precondition.failed",
                "The record has changed since it was read; load it again", List.of());
    }

    /** @return the exception for a signed-in caller who lacks the required role */
    public static WebException forbidden() {
        return new WebException(HttpStatus.FORBIDDEN, "access-denied", "This endpoint is for administrators",
                List.of());
    }

    /**
     * No endpoint has this path.
     *
     * @param path the path
     * @return the exception
     */
    public static WebException noRoute(String path) {
        return new WebException(HttpStatus.NOT_FOUND, "route.not-found", "No endpoint at " + path, List.of());
    }

    /**
     * The path exists but not with this method.
     *
     * @param method the method
     * @param path   the path
     * @return the exception
     */
    public static WebException methodNotAllowed(String method, String path) {
        return new WebException(HttpStatus.METHOD_NOT_ALLOWED, "route.method-not-allowed",
                method + " is not supported at " + path, List.of());
    }

    /** @return the exception for a body that is not JSON */
    public static WebException unsupportedMediaType() {
        return new WebException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "request.unsupported-media-type",
                "Send the request body as application/json", List.of());
    }

    /** @return the HTTP status code */
    public int status() {
        return status;
    }

    /** @return the stable machine-readable code */
    public String code() {
        return code;
    }

    /** @return the invalid fields, if any */
    public List<Problem.FieldError> errors() {
        return errors;
    }

    /** @return the extension members of the problem, if any */
    public Map<String, Object> extensions() {
        return extensions;
    }
}
