package com.bookstore.shared.web;

import com.bookstore.shared.kernel.error.DomainException;
import com.bookstore.shared.kernel.error.ValidationException;

import java.lang.System.Logger.Level;
import java.util.Comparator;
import java.util.List;

import jakarta.json.bind.JsonbException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * Turns any exception into an RFC 9457 problem (ADR-0009). The status of a business error comes
 * from its {@link DomainException.Kind}, never from its class; Bean Validation and unreadable JSON
 * are 400s; the JAX-RS runtime's own 404, 405 and 415 keep their status; anything unexpected
 * becomes an opaque 500 whose details go to the log only.
 */
public final class ProblemMapper {

    /** The code of an unexpected failure. */
    public static final String INTERNAL_ERROR = "internal-error";

    private static final System.Logger LOG = System.getLogger(ProblemMapper.class.getName());

    /**
     * Returns the HTTP status of a business error.
     *
     * @param kind the error's kind
     * @return 404, 409, 422 or 403
     */
    public static int statusOf(DomainException.Kind kind) {
        return switch (kind) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    }

    /**
     * Describes a failure as a problem.
     *
     * @param failure what went wrong
     * @return the problem to send
     */
    public Problem toProblem(Throwable failure) {
        return switch (failure) {
            case WebException web -> Problem.of(web.status(), web.code(), web.getMessage(), web.errors())
                    .with(web.extensions());
            case ValidationException invalid -> Problem.of(statusOf(invalid.kind()), invalid.code(),
                    invalid.getMessage(), invalid.violations().stream()
                            .map(v -> new Problem.FieldError(v.field(), v.message())).toList());
            case DomainException domain -> Problem.of(statusOf(domain.kind()), domain.code(), domain.getMessage(),
                    List.of());
            case ConstraintViolationException invalid -> Problem.of(HttpStatus.BAD_REQUEST, "request.invalid",
                    "The request has invalid fields", fieldErrors(invalid));
            case NotFoundException notFound -> Problem.of(HttpStatus.NOT_FOUND, "route.not-found",
                    "No endpoint at this path", List.of());
            case NotAllowedException notAllowed -> Problem.of(HttpStatus.METHOD_NOT_ALLOWED,
                    "route.method-not-allowed", "This method is not supported at this path", List.of());
            case NotSupportedException unsupported -> Problem.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "request.unsupported-media-type", "Send the request body as application/json", List.of());
            case WebApplicationException web when web.getResponse().getStatus() == HttpStatus.BAD_REQUEST
                    || isUnreadableJson(web) -> malformedJson();
            case ProcessingException processing when isUnreadableJson(processing) -> malformedJson();
            case JsonbException json -> malformedJson();
            default -> {
                LOG.log(Level.ERROR, "Unexpected failure while handling request " + CorrelationIds.current(), failure);
                yield Problem.of(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR,
                        "An unexpected error occurred. It has been logged.", List.of());
            }
        };
    }

    private static Problem malformedJson() {
        return Problem.of(HttpStatus.BAD_REQUEST, "request.malformed-json",
                "The body is not valid JSON for this request", List.of());
    }

    private static boolean isUnreadableJson(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonbException) {
                return true;
            }
        }
        return false;
    }

    private static List<Problem.FieldError> fieldErrors(ConstraintViolationException invalid) {
        return invalid.getConstraintViolations().stream()
                .map(v -> new Problem.FieldError(fieldOf(v), v.getMessage()))
                .sorted(Comparator.comparing(Problem.FieldError::field).thenComparing(Problem.FieldError::message))
                .toList();
    }

    /** The last node of a path such as {@code register.request.email} is the field the client sent. */
    private static String fieldOf(ConstraintViolation<?> violation) {
        String field = "";
        for (Path.Node node : violation.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }
}
