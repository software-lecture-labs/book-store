package com.bookstore.app.security;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Just enough of the Servlet and Jakarta Security objects to run the authentication mechanism
 * without a server. Each fake answers the methods the mechanism calls and fails on any other.
 */
final class FakeHttp {

    /** The request side: headers, the session and whether it is a programmatic sign-in. */
    final Map<String, String> headers = new HashMap<>();
    final Map<String, Object> sessionAttributes = new HashMap<>();
    boolean hasSession;
    int sessionIdChanges;
    boolean invalidated;
    AuthenticationParameters authParameters;

    /** The response side. */
    int status = 200;
    final Map<String, String> responseHeaders = new HashMap<>();
    final StringWriter body = new StringWriter();

    /** What the mechanism told the container. */
    String signedInAs;
    Set<String> signedInGroups;

    HttpServletRequest request() {
        return proxy(HttpServletRequest.class, (method, args) -> switch (method) {
            case "getHeader" -> headers.get((String) args[0]);
            case "getSession" -> {
                boolean create = args == null || (boolean) args[0];
                if (!hasSession && create) {
                    hasSession = true;
                }
                yield hasSession ? session() : null;
            }
            case "changeSessionId" -> {
                sessionIdChanges++;
                yield "new-id";
            }
            default -> unexpected(method);
        });
    }

    HttpServletResponse response() {
        PrintWriter writer = new PrintWriter(body);
        return proxy(HttpServletResponse.class, (method, args) -> switch (method) {
            case "setStatus" -> {
                status = (int) args[0];
                yield null;
            }
            case "setHeader" -> {
                responseHeaders.put((String) args[0], (String) args[1]);
                yield null;
            }
            case "setContentType" -> {
                responseHeaders.put("Content-Type", (String) args[0]);
                yield null;
            }
            case "setCharacterEncoding" -> null;
            case "getWriter" -> writer;
            default -> unexpected(method);
        });
    }

    HttpMessageContext context() {
        return proxy(HttpMessageContext.class, (method, args) -> switch (method) {
            case "isAuthenticationRequest" -> authParameters != null;
            case "getAuthParameters" -> authParameters;
            case "doNothing" -> AuthenticationStatus.NOT_DONE;
            case "notifyContainerAboutLogin" -> {
                if (args[0] instanceof CredentialValidationResult result) {
                    signedInAs = result.getCallerPrincipal().getName();
                    signedInGroups = result.getCallerGroups();
                } else {
                    signedInAs = (String) args[0];
                    @SuppressWarnings("unchecked")
                    Set<String> groups = (Set<String>) args[1];
                    signedInGroups = groups;
                }
                yield AuthenticationStatus.SUCCESS;
            }
            default -> unexpected(method);
        });
    }

    private HttpSession session() {
        return proxy(HttpSession.class, (method, args) -> switch (method) {
            case "getAttribute" -> sessionAttributes.get((String) args[0]);
            case "setAttribute" -> {
                sessionAttributes.put((String) args[0], args[1]);
                yield null;
            }
            case "invalidate" -> {
                sessionAttributes.clear();
                hasSession = false;
                invalidated = true;
                yield null;
            }
            default -> unexpected(method);
        });
    }

    private static Object unexpected(String method) {
        throw new UnsupportedOperationException("The fake does not answer " + method);
    }

    @FunctionalInterface
    private interface Answer {
        Object answer(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Answer answer) {
        Object proxy = Proxy.newProxyInstance(FakeHttp.class.getClassLoader(), new Class<?>[]{type},
                (self, method, args) -> switch (method.getName()) {
                    case "toString" -> "Fake " + type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(self);
                    case "equals" -> self == args[0];
                    default -> answer.answer(method.getName(), args);
                });
        return type.cast(proxy);
    }
}
