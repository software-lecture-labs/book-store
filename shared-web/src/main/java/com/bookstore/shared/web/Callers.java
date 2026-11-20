package com.bookstore.shared.web;

import com.bookstore.shared.kernel.identity.CustomerId;

import java.security.Principal;
import java.util.Optional;

import jakarta.ws.rs.core.SecurityContext;

/**
 * Finds the signed-in caller in a request's {@link SecurityContext} (ADR-0020). On the server
 * Jakarta Security fills it: the principal's name is the customer's id and the role is a group. In
 * the HTTP tests a filter fills it in the same way. Resources ask for the role they need.
 */
public final class Callers {

    /**
     * The header the browser pages send with every request (ADR-0020). A request signed in with the
     * session cookie that changes state without it is rejected, so another site cannot send it.
     */
    public static final String CLIENT_HEADER = "X-Bookstore-Client";

    /** The {@code WWW-Authenticate} value of a 401 for API clients. */
    public static final String BASIC_CHALLENGE = "Basic realm=\"bookstore\", charset=\"UTF-8\"";

    private Callers() {
    }

    /**
     * Returns the caller, if they signed in.
     *
     * @param security the request's security context
     * @return the caller; empty for an anonymous request or a principal that is not a customer
     */
    public static Optional<Caller> of(SecurityContext security) {
        Principal principal = security.getUserPrincipal();
        if (principal == null) {
            return Optional.empty();
        }
        long id;
        try {
            id = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (id <= 0) {
            return Optional.empty();
        }
        String role = security.isUserInRole(Caller.ADMIN) ? Caller.ADMIN : Caller.CUSTOMER;
        return Optional.of(new Caller(CustomerId.of(id), role));
    }

    /**
     * Requires a signed-in caller.
     *
     * @param security the request's security context
     * @return the caller
     * @throws WebException 401 for an anonymous request
     */
    public static Caller customer(SecurityContext security) {
        return of(security).orElseThrow(WebException::unauthenticated);
    }

    /**
     * Requires a signed-in administrator.
     *
     * @param security the request's security context
     * @return the caller
     * @throws WebException 401 for an anonymous request, 403 for a customer
     */
    public static Caller administrator(SecurityContext security) {
        Caller caller = customer(security);
        if (!caller.isAdmin()) {
            throw WebException.forbidden();
        }
        return caller;
    }
}
