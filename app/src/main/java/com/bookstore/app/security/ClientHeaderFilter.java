package com.bookstore.app.security;

import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.WebException;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protects the session cookie against cross-site requests (ADR-0020): a request that comes with a
 * session cookie and changes state must carry {@link Callers#CLIENT_HEADER}; otherwise it is a
 * 403 problem. Another site cannot add the header, because the API allows no cross-origin requests.
 * HTTP Basic requests do not sign in with the cookie and are not checked.
 */
@WebFilter(urlPatterns = "/api/*")
public class ClientHeaderFilter extends HttpFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (rejects(request.getMethod(), request.getHeader("Authorization") != null,
                request.getRequestedSessionId() != null, request.getHeader(Callers.CLIENT_HEADER) != null)) {
            ProblemResponses.write(request, response, WebException.clientHeaderMissing());
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * The rule itself.
     *
     * @param method        the HTTP method
     * @param basic         whether the request has an {@code Authorization} header
     * @param sessionCookie whether the request names a session
     * @param clientHeader  whether the request has the client header
     * @return {@code true} if the request must be rejected
     */
    static boolean rejects(String method, boolean basic, boolean sessionCookie, boolean clientHeader) {
        return !SAFE_METHODS.contains(method) && !basic && sessionCookie && !clientHeader;
    }
}
