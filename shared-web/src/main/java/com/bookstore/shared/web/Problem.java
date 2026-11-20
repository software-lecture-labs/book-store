package com.bookstore.shared.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An error response in the RFC 9457 "Problem Details" format, sent as
 * {@code application/problem+json} (ADR-0009).
 *
 * @param type       a URI that names the kind of problem, for example {@code "/problems/email.taken"}
 * @param title      a short summary of the HTTP status, for example {@code "Conflict"}
 * @param status     the HTTP status code
 * @param detail     a human-readable explanation of this occurrence
 * @param code       the stable machine-readable code clients branch on
 * @param errors     the invalid fields, empty unless the request had format or validation errors
 * @param extensions extra members RFC 9457 allows, written next to the standard ones, for example the
 *                   changes a cart went through
 */
public record Problem(String type, String title, int status, String detail, String code, List<FieldError> errors,
                      Map<String, Object> extensions) {

    /** The media type of a problem response. */
    public static final String MEDIA_TYPE = "application/problem+json";

    /** Copies the field errors and the extensions. */
    public Problem {
        Objects.requireNonNull(code, "code");
        errors = List.copyOf(errors);
        extensions = Map.copyOf(extensions);
    }

    /**
     * Creates a problem whose type is derived from its code.
     *
     * @param status the HTTP status code
     * @param code   the stable code
     * @param detail the explanation
     * @param errors the invalid fields
     * @return the problem
     */
    public static Problem of(int status, String code, String detail, List<FieldError> errors) {
        return new Problem("/problems/" + code, HttpStatus.reasonOf(status), status, detail, code, errors,
                Map.of());
    }

    /**
     * Returns a copy with extension members.
     *
     * @param members the members, by name
     * @return the problem
     */
    public Problem with(Map<String, Object> members) {
        return new Problem(type, title, status, detail, code, errors, members);
    }

    /**
     * Returns the response body: the standard members in RFC 9457 order, then the extensions at the
     * same level.
     *
     * @return the members, by name
     */
    public Map<String, Object> body() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("code", code);
        body.put("errors", errors);
        body.putAll(extensions);
        return body;
    }

    /**
     * One invalid field of a request.
     *
     * @param field   the field's name as the client sent it, for example {@code "quantity"}
     * @param message why it is invalid
     */
    public record FieldError(String field, String message) {
    }
}
