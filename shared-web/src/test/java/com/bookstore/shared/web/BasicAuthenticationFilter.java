package com.bookstore.shared.web;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Objects;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Stands in for the server's Jakarta Security in the HTTP tests (ADR-0020): signs the caller in
 * from the {@code Authorization: Basic} header and fills the request's {@link SecurityContext} the
 * way the server does, with the customer's id as the principal's name and the role as a group. A
 * request without the header stays anonymous, wrong credentials are a 401.
 */
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthenticationFilter implements ContainerRequestFilter {

    private final Authenticator authenticator;

    /**
     * Creates the filter.
     *
     * @param authenticator checks user names and passwords
     */
    public BasicAuthenticationFilter(Authenticator authenticator) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
    }

    @Override
    public void filter(ContainerRequestContext request) {
        String authorization = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            return;
        }
        Caller caller = authenticate(authorization);
        boolean secure = request.getSecurityContext().isSecure();
        request.setSecurityContext(new CallerContext(caller, secure));
    }

    private Caller authenticate(String authorization) {
        if (!authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw WebException.unauthenticated();
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw WebException.unauthenticated();
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            throw WebException.unauthenticated();
        }
        return authenticator.authenticate(decoded.substring(0, colon), decoded.substring(colon + 1))
                .orElseThrow(WebException::unauthenticated);
    }

    /** A security context that knows the signed-in caller, shaped like the server's. */
    private record CallerContext(Caller caller, boolean secure) implements SecurityContext {

        @Override
        public Principal getUserPrincipal() {
            String name = Long.toString(caller.customerId().value());
            return () -> name;
        }

        @Override
        public boolean isUserInRole(String role) {
            return caller.role().equals(role);
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }
}
