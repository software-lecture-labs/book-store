package com.bookstore.shared.web;

import java.net.URI;

import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Serves JAX-RS resources on Jersey's JDK HTTP container, without an application server: the same
 * runtime JerseyTest uses (ADR-0010). The resources are the objects the test built, so they can sit
 * on in-memory use cases; the problem mappers, the JSON-B settings and the HTTP Basic filter are
 * the ones the server uses. The API is mounted at {@code /api/v1}, as on the server.
 */
public final class RestTestServer implements AutoCloseable {

    /** Where the API lives, as on the server. */
    public static final String API_PATH = "/api/v1/";

    private final HttpServer server;
    private final URI root;

    private RestTestServer(HttpServer server, URI root) {
        this.server = server;
        this.root = root;
    }

    /**
     * Starts a server on a free port.
     *
     * @param authenticator checks HTTP Basic credentials
     * @param resources     the resource objects to serve
     * @return the running server
     */
    public static RestTestServer start(Authenticator authenticator, Object... resources) {
        ResourceConfig config = new ResourceConfig()
                .registerInstances(resources)
                .registerInstances(new CorrelationIdFilter(), new BasicAuthenticationFilter(authenticator), new JsonbConfiguration(),
                        new ProblemExceptionMappers.AnyFailure(), new ProblemExceptionMappers.InvalidRequest())
                // Bean Validation errors reach the problem mapper instead of Jersey's plain-text answer.
                .property("jersey.config.beanValidation.enableOutputValidationErrorEntity.server", false);
        HttpServer server = JdkHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0" + API_PATH), config);
        URI root = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        return new RestTestServer(server, root);
    }

    /** @return {@code http://127.0.0.1:<port>/}; paths such as {@code /api/v1/books} resolve against it */
    public URI root() {
        return root;
    }

    /** @return a client for this server */
    public HttpTestClient client() {
        return new HttpTestClient(root);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
