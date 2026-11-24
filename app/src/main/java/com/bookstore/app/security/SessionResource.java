package com.bookstore.app.security;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.rest.GuestCartHolder;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.WebException;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.Password;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * The browser's sign-in (ACC-3, ADR-0020): the e-mail address and password are sent once, then the
 * session cookie identifies the caller. API clients use HTTP Basic instead and need none of this.
 * A visitor's cart is merged into the stored cart at sign-in and leaves the session (CRT-2).
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code POST /api/v1/session}</td><td>sign in; the profile, or a 401 problem</td></tr>
 *   <tr><td>{@code GET /api/v1/session}</td><td>the signed-in profile, or a 401 problem</td></tr>
 *   <tr><td>{@code DELETE /api/v1/session}</td><td>sign out; the session ends</td></tr>
 * </table>
 */
@Path("session")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SessionResource {

    private final SecurityContext security;
    private final AccountFacade accounts;
    private final CartFacade cart;
    private final GuestCartHolder guestCart;

    /**
     * Creates the resource.
     *
     * @param security the container's Jakarta Security context
     * @param accounts  the account use cases
     * @param cart      the cart use cases
     * @param guestCart the visitor's cart in the session
     */
    @Inject
    public SessionResource(SecurityContext security, AccountFacade accounts, CartFacade cart,
                           GuestCartHolder guestCart) {
        this.security = security;
        this.accounts = accounts;
        this.cart = cart;
        this.guestCart = guestCart;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected SessionResource() {
        this(null, null, null, null);
    }

    /**
     * Signs in.
     *
     * @param signIn   the e-mail address and the password
     * @param request  the HTTP request
     * @param response the HTTP response
     * @return the signed-in customer's profile
     */
    @POST
    @Operation(summary = "Sign in with e-mail and password; the session cookie identifies the caller from now on "
            + "and the visitor's cart joins the stored cart")
    public CustomerView signIn(@Valid @NotNull SignInRequest signIn, @Context HttpServletRequest request,
                               @Context HttpServletResponse response) {
        UsernamePasswordCredential credential = new UsernamePasswordCredential(signIn.email(),
                new Password(signIn.password()));
        AuthenticationStatus status = security.authenticate(request, response,
                AuthenticationParameters.withParams().credential(credential).newAuthentication(true));
        if (status != AuthenticationStatus.SUCCESS || !(request.getSession(false) instanceof HttpSession session)
                || !(session.getAttribute(SessionCaller.ATTRIBUTE) instanceof SessionCaller caller)) {
            throw WebException.signInFailed();
        }
        CustomerId customerId = CustomerId.of(Long.parseLong(caller.name()));
        GuestCart visitorCart = guestCart.cart();
        if (!visitorCart.isEmpty()) {
            cart.mergeGuestCart(customerId, visitorCart);
            visitorCart.replaceItems(List.of());
        }
        return accounts.profile(customerId);
    }

    /**
     * Returns who is signed in.
     *
     * @param jaxrs the request's JAX-RS security context
     * @return the profile
     */
    @GET
    @Operation(summary = "The signed-in customer's profile")
    public CustomerView current(@Context jakarta.ws.rs.core.SecurityContext jaxrs) {
        return accounts.profile(Callers.customer(jaxrs).customerId());
    }

    /**
     * Signs out: the session ends, with everything in it.
     *
     * @param request the HTTP request
     * @return 204
     */
    @DELETE
    @Operation(summary = "Sign out; the session ends")
    public Response signOut(@Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        try {
            request.logout();
        } catch (ServletException e) {
            throw new IllegalStateException("The container could not sign the caller out", e);
        }
        return Response.noContent().build();
    }

    /**
     * The sign-in request; only its format is checked here (ADR-0009).
     *
     * @param email    the e-mail address
     * @param password the password
     */
    public record SignInRequest(@NotBlank String email, @NotBlank String password) {

        @Override
        public String toString() {
            return "SignInRequest[email=" + email + ", password=***]";
        }
    }
}
