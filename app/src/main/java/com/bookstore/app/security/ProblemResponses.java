package com.bookstore.app.security;

import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.CorrelationIds;
import com.bookstore.shared.web.HttpStatus;
import com.bookstore.shared.web.Problem;
import com.bookstore.shared.web.WebException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes RFC 9457 problems for the failures found before JAX-RS runs (ADR-0009): a wrong HTTP
 * Basic credential and a missing client header. They look like every other problem of the API, with
 * a correlation id too (ADR-0022).
 */
final class ProblemResponses {

    private static final Jsonb JSON = JsonbBuilder.create();

    private ProblemResponses() {
    }

    /**
     * Writes the problem of an exception.
     *
     * @param request  the request, to see whether it came from the browser pages
     * @param response the response to write
     * @param failure  the failure
     */
    static void write(HttpServletRequest request, HttpServletResponse response, WebException failure) {
        String correlationId = CorrelationIds.accept(request.getHeader(CorrelationIds.HEADER));
        Problem problem = Problem.of(failure.status(), failure.code(), failure.getMessage(), failure.errors())
                .with(Map.of(CorrelationIds.PROBLEM_MEMBER, correlationId));
        response.setStatus(problem.status());
        response.setHeader(CorrelationIds.HEADER, correlationId);
        if (problem.status() == HttpStatus.UNAUTHORIZED && request.getHeader(Callers.CLIENT_HEADER) == null) {
            response.setHeader("WWW-Authenticate", Callers.BASIC_CHALLENGE);
        }
        response.setContentType(Problem.MEDIA_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write(JSON.toJson(problem.body()));
            response.getWriter().flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
