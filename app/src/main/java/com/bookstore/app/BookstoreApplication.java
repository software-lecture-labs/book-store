package com.bookstore.app;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * The JAX-RS application (ADR-0009, ADR-0010): every module's {@code rest} resources live under
 * {@code /api/v1}. The server finds the resources, the {@code shared-web} providers (problem
 * mappers, JSON-B settings) and the Jakarta Security mechanism in the WAR by themselves, so nothing
 * is listed here;
 * the rest of the path stays free for the HTML pages of Phase 7.
 *
 * <p>MicroProfile OpenAPI publishes the API's description at {@code /openapi}, built from the
 * resources' JAX-RS annotations and the definition below (API-4).
 */
@ApplicationPath("api/v1")
@OpenAPIDefinition(
        info = @Info(
                title = "Book Store API",
                version = "1",
                description = "Catalog, accounts, carts, orders and reviews of the Book Store. Errors are RFC 9457 "
                        + "problems (application/problem+json) with a stable code. Endpoints under /me, /cart, "
                        + "/orders and /admin need a signed-in caller: HTTP Basic, or the session cookie of POST /session "
                        + "(state-changing cookie requests also send the X-Bookstore-Client header)."))
@SecurityScheme(securitySchemeName = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
public class BookstoreApplication extends Application {
}
