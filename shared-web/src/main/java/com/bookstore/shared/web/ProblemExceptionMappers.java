package com.bookstore.shared.web;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * The JAX-RS exception mappers that answer every failure with an RFC 9457 problem (ADR-0009,
 * ADR-0010). Two mappers are needed: the runtime has its own mapper for Bean Validation, and only a
 * mapper for the more specific {@link ConstraintViolationException} takes precedence over it.
 *
 * <p>A 401 asks for HTTP Basic with {@code WWW-Authenticate}, except for the browser pages, which
 * send {@link Callers#CLIENT_HEADER} and sign in with the session (ADR-0020): the header would make
 * the browser open its own password dialog.
 */
public final class ProblemExceptionMappers {

    private static final ProblemMapper MAPPER = new ProblemMapper();

    private ProblemExceptionMappers() {
    }

    /**
     * Writes a problem as the response, asking for HTTP Basic if it is a 401.
     *
     * @param problem the problem
     * @return the response
     */
    public static Response responseOf(Problem problem) {
        return responseOf(problem, true);
    }

    /**
     * Writes a problem as the response.
     * The request's correlation id, if there is one, becomes the member {@value CorrelationIds#PROBLEM_MEMBER}
     * (ADR-0022).
     *
     * @param problem   the problem
     * @param challenge whether a 401 asks for HTTP Basic
     * @return the response
     */
    public static Response responseOf(Problem problem, boolean challenge) {
        Map<String, Object> body = new LinkedHashMap<>(problem.body());
        String correlationId = CorrelationIds.current();
        if (correlationId != null) {
            body.put(CorrelationIds.PROBLEM_MEMBER, correlationId);
        }
        Response.ResponseBuilder response = Response.status(problem.status())
                .type(Problem.MEDIA_TYPE)
                .entity(body);
        if (challenge && problem.status() == HttpStatus.UNAUTHORIZED) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, Callers.BASIC_CHALLENGE);
        }
        return response.build();
    }

    private static boolean challenges(HttpHeaders headers) {
        return headers == null || headers.getHeaderString(Callers.CLIENT_HEADER) == null;
    }

    /** Maps every exception that has no more specific mapper. */
    @Provider
    @ApplicationScoped
    public static class AnyFailure implements ExceptionMapper<Throwable> {

        @Context
        private HttpHeaders headers;

        @Override
        public Response toResponse(Throwable failure) {
            return responseOf(MAPPER.toProblem(failure), challenges(headers));
        }
    }

    /** Maps the format errors Bean Validation found in a request record annotated with {@code @Valid}. */
    @Provider
    @ApplicationScoped
    public static class InvalidRequest implements ExceptionMapper<ConstraintViolationException> {

        @Context
        private HttpHeaders headers;

        @Override
        public Response toResponse(ConstraintViolationException failure) {
            return responseOf(MAPPER.toProblem(failure), challenges(headers));
        }
    }
}
