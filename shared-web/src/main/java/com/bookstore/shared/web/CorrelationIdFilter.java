package com.bookstore.shared.web;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Gives every API request its correlation id before anything else runs, and puts it into the
 * answer's {@value CorrelationIds#HEADER} header (ADR-0022). Problems get it from
 * {@link ProblemExceptionMappers}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
@ApplicationScoped
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String PROPERTY = CorrelationIdFilter.class.getName();

    @Override
    public void filter(ContainerRequestContext request) {
        String id = CorrelationIds.accept(header(request, CorrelationIds.HEADER));
        request.setProperty(PROPERTY, id);
        CorrelationIds.begin(id);
    }

    /** Header names are case-insensitive (RFC 9110); not every container's header map is. */
    private static String header(ContainerRequestContext request, String name) {
        return request.getHeaders().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty())
                .map(entry -> entry.getValue().getFirst())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        Object id = request.getProperty(PROPERTY);
        if (id instanceof String value) {
            response.getHeaders().putSingle(CorrelationIds.HEADER, value);
        }
        CorrelationIds.end();
    }
}
