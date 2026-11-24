package com.bookstore.shared.web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Talks to a running API with the JDK's {@link HttpClient}, the way a browser or {@code curl}
 * would. Shared with the modules' {@code rest} tests through the test-jar.
 */
public final class HttpTestClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final URI baseUri;
    private final String authorization;
    private final boolean browser;
    private final Map<String, String> extraHeaders;

    /**
     * Creates an anonymous client.
     *
     * @param root the server's root, for example {@code http://127.0.0.1:8080/}
     */
    public HttpTestClient(URI root) {
        this(root, null, false, Map.of());
    }

    private HttpTestClient(URI baseUri, String authorization, boolean browser, Map<String, String> extraHeaders) {
        this.baseUri = baseUri;
        this.authorization = authorization;
        this.browser = browser;
        this.extraHeaders = Map.copyOf(extraHeaders);
    }

    /**
     * Returns a client that signs in with HTTP Basic.
     *
     * @param email    the user name
     * @param password the password
     * @return the client
     */
    public HttpTestClient as(String email, String password) {
        String token = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return new HttpTestClient(baseUri, "Basic " + token, browser, extraHeaders);
    }

    /**
     * Returns a client that sends {@link Callers#CLIENT_HEADER}, as the browser pages do.
     *
     * @return the client
     */
    public HttpTestClient asBrowser() {
        return new HttpTestClient(baseUri, authorization, true, extraHeaders);
    }

    /**
     * Returns a client that also sends a header with every request.
     *
     * @param name  the header
     * @param value its value
     * @return the client
     */
    public HttpTestClient withHeader(String name, String value) {
        Map<String, String> headers = new HashMap<>(extraHeaders);
        headers.put(name, value);
        return new HttpTestClient(baseUri, authorization, browser, headers);
    }

    /** Sends a {@code GET}. @param path the path and query @return the reply */
    public Reply get(String path) {
        return send("GET", path, null);
    }

    /** Sends a {@code POST}. @param path the path @param json the body, or {@code null} @return the reply */
    public Reply post(String path, String json) {
        return send("POST", path, json);
    }

    /** Sends a {@code PUT}. @param path the path @param json the body, or {@code null} @return the reply */
    public Reply put(String path, String json) {
        return send("PUT", path, json);
    }

    /** Sends a {@code DELETE}. @param path the path @return the reply */
    public Reply delete(String path) {
        return send("DELETE", path, null);
    }

    /**
     * Sends a request.
     *
     * @param method the method
     * @param path   the path and query
     * @param json   the body, or {@code null}
     * @return the reply
     */
    public Reply send(String method, String path, String json) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (json != null) {
            request.header("Content-Type", "application/json");
        }
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (browser) {
            request.header(Callers.CLIENT_HEADER, "web");
        }
        extraHeaders.forEach(request::header);
        try {
            HttpResponse<String> response = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Reply(response.statusCode(), response.body(),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.headers().firstValue("Location").orElse(null),
                    response.headers().firstValue("WWW-Authenticate").orElse(null), response.headers());
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(method + " " + path + " was interrupted", e);
        }
    }

    /**
     * Sends a file as the body of a {@code PUT}, as a browser's {@code fetch} does with a file input.
     *
     * @param path        the path
     * @param contentType the file's media type
     * @param content     the bytes
     * @return the reply
     */
    public Reply upload(String path, String contentType, byte[] content) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .header("Content-Type", contentType);
        HttpResponse<String> response = exchange(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reply(response.statusCode(), response.body(),
                response.headers().firstValue("Content-Type").orElse(""),
                response.headers().firstValue("Location").orElse(null),
                response.headers().firstValue("WWW-Authenticate").orElse(null), response.headers());
    }

    /**
     * Downloads a file.
     *
     * @param path        the path
     * @param ifNoneMatch an entity tag for {@code If-None-Match}, or {@code null}
     * @return the reply with the raw bytes
     */
    public FileReply download(String path, String ifNoneMatch) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path)).GET();
        if (ifNoneMatch != null) {
            request.header("If-None-Match", ifNoneMatch);
        }
        HttpResponse<byte[]> response = exchange(request, HttpResponse.BodyHandlers.ofByteArray());
        return new FileReply(response.statusCode(), response.body(),
                response.headers().firstValue("Content-Type").orElse(""),
                response.headers().firstValue("ETag").orElse(null));
    }

    private <T> HttpResponse<T> exchange(HttpRequest.Builder request, HttpResponse.BodyHandler<T> body) {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (browser) {
            request.header(Callers.CLIENT_HEADER, "web");
        }
        extraHeaders.forEach(request::header);
        try {
            return client.send(request.build(), body);
        } catch (IOException e) {
            throw new IllegalStateException(request.build().uri() + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(request.build().uri() + " was interrupted", e);
        }
    }

    /**
     * A reply with a file.
     *
     * @param status      the status code
     * @param body        the bytes
     * @param contentType the {@code Content-Type} header
     * @param etag        the {@code ETag} header, or {@code null}
     */
    public record FileReply(int status, byte[] body, String contentType, String etag) {
    }

    /**
     * An HTTP reply.
     *
     * @param status      the status code
     * @param body        the body as text
     * @param contentType the {@code Content-Type} header
     * @param location    the {@code Location} header, or {@code null}
     * @param challenge   the {@code WWW-Authenticate} header, or {@code null}
     * @param headers     every header of the reply
     */
    public record Reply(int status, String body, String contentType, String location, String challenge,
                        HttpHeaders headers) {

        /** @param name a header @return its first value, or {@code null} */
        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        private static final ObjectMapper JSON = new ObjectMapper();

        /** @return the body as a JSON tree */
        public JsonNode json() {
            try {
                return JSON.readTree(body);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Not JSON: " + body, e);
            }
        }

        /** @return the problem's {@code code}, for error replies */
        public String code() {
            return json().path("code").asText();
        }
    }
}
