package com.bookstore.app.security;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.WebException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * The server's only authentication mechanism (ADR-0020). It runs before every request and decides
 * who the caller is; whether the endpoint needs a caller or a role, the resource decides.
 *
 * <ol>
 *   <li>{@code POST /api/v1/session} calls {@code SecurityContext.authenticate} with the e-mail
 *       address and password: if they are right, the caller is kept in the HTTP session, under a
 *       new session id.</li>
 *   <li>A request with {@code Authorization: Basic} is checked every time and never gets a
 *       session; wrong credentials are a 401 problem.</li>
 *   <li>A request whose session holds a caller is that caller, as long as the account is still active;
 *       the session of an account the administrator switched off ends (ACC-7, ADR-0021).</li>
 *   <li>Every other request is anonymous.</li>
 * </ol>
 *
 * <p>The session is kept by this class rather than with {@code @AutoApplySession}, which would give
 * every Basic request a session, too.
 */
@ApplicationScoped
public class BookstoreAuthenticationMechanism implements HttpAuthenticationMechanism {

    private final IdentityStoreHandler identityStores;
    private final AccountFacade accounts;

    /**
     * Creates the mechanism.
     *
     * @param identityStores the container's handler over the identity stores
     * @param accounts       the accounts, to end the session of one that was switched off
     */
    @Inject
    public BookstoreAuthenticationMechanism(IdentityStoreHandler identityStores, AccountFacade accounts) {
        this.identityStores = identityStores;
        this.accounts = accounts;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected BookstoreAuthenticationMechanism() {
        this(null, null);
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                                HttpMessageContext context) {
        if (context.isAuthenticationRequest() && context.getAuthParameters() != null
                && context.getAuthParameters().getCredential() instanceof UsernamePasswordCredential credential) {
            return signIn(request, context, credential);
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            return basic(request, response, context, authorization);
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionCaller.ATTRIBUTE) instanceof SessionCaller caller) {
            if (!accounts.isActive(CustomerId.of(Long.parseLong(caller.name())))) {
                session.invalidate();
                return context.doNothing();
            }
            return context.notifyContainerAboutLogin(caller.name(), caller.groups());
        }
        return context.doNothing();
    }

    private AuthenticationStatus signIn(HttpServletRequest request, HttpMessageContext context,
                                        UsernamePasswordCredential credential) {
        CredentialValidationResult result = identityStores.validate(credential);
        if (result.getStatus() != CredentialValidationResult.Status.VALID) {
            return AuthenticationStatus.SEND_FAILURE;
        }
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SessionCaller caller = new SessionCaller(result.getCallerPrincipal().getName(), result.getCallerGroups());
        request.getSession(true).setAttribute(SessionCaller.ATTRIBUTE, caller);
        return context.notifyContainerAboutLogin(caller.name(), caller.groups());
    }

    private AuthenticationStatus basic(HttpServletRequest request, HttpServletResponse response,
                                       HttpMessageContext context, String authorization) {
        CredentialValidationResult result = BasicCredentials.parse(authorization)
                .map(identityStores::validate)
                .orElse(CredentialValidationResult.INVALID_RESULT);
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            return context.notifyContainerAboutLogin(result);
        }
        ProblemResponses.write(request, response, WebException.unauthenticated());
        return AuthenticationStatus.SEND_FAILURE;
    }
}
