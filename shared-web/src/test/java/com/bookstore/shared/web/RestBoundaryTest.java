package com.bookstore.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.net.URI;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The HTTP boundary on JAX-RS, end to end: JSON-B settings, Bean Validation, RFC 9457 problems and
 * HTTP Basic sign-in, with the same expectations the Phase 5 server met.
 */
class RestBoundaryTest {

    private final RestTestServer server = RestTestServer.start((user, password) -> switch (user + ":" + password) {
        case "ayse@example.com:secret" -> Optional.of(new Caller(CustomerId.of(1), "CUSTOMER"));
        case "admin@example.com:secret" -> Optional.of(new Caller(CustomerId.of(2), Caller.ADMIN));
        default -> Optional.empty();
    }, new Things());
    private final HttpTestClient anonymous = server.client();

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void writesIdsAsNumbersMoneyAsAmountAndTimesAsText() {
        HttpTestClient.Reply reply = anonymous.get("/api/v1/things/7");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.contentType()).startsWith("application/json");
        assertThat(reply.json().get("id").asLong()).isEqualTo(7);
        assertThat(reply.body()).contains("\"price\":{\"amount\":95.00,\"currency\":\"TRY\"}");
        assertThat(reply.json().get("year").asInt()).isEqualTo(1965);
        assertThat(reply.json().get("at").asText()).isEqualTo("2026-01-02T03:04:05Z");
    }

    @Test
    void businessErrorsBecomeProblems() {
        HttpTestClient.Reply reply = anonymous.get("/api/v1/things/404");

        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.contentType()).startsWith(Problem.MEDIA_TYPE);
        assertThat(reply.code()).isEqualTo("not-found");
        assertThat(reply.json().get("type").asText()).isEqualTo("/problems/not-found");
    }

    @Test
    void everyInvalidFieldIsReportedAtOnce() {
        HttpTestClient.Reply reply = anonymous.post("/api/v1/things", "{\"name\": \" \", \"quantity\": 11}");

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.code()).isEqualTo("request.invalid");
        assertThat(reply.json().get("errors").findValuesAsText("field")).containsExactly("name", "quantity");
    }

    @Test
    void validRequestsAreCreated() {
        HttpTestClient.Reply reply = anonymous.post("/api/v1/things", "{\"name\": \"Dune\", \"quantity\": 2}");

        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.location()).endsWith("/api/v1/things/9");
    }

    @Test
    void malformedJsonAndUnknownFieldsAre400() {
        assertThat(anonymous.post("/api/v1/things", "{\"name\": ").code()).isEqualTo("request.malformed-json");
        assertThat(anonymous.post("/api/v1/things", "{\"name\": \"Dune\", \"quantity\": 1, \"price\": 0}").code())
                .isEqualTo("request.malformed-json");
        assertThat(anonymous.get("/api/v1/things/abc").status()).isEqualTo(400);
    }

    @Test
    void unknownRoutesAre404AndWrongMethods405() {
        assertThat(anonymous.get("/api/v1/nothing").code()).isEqualTo("route.not-found");
        assertThat(anonymous.delete("/api/v1/things/1").code()).isEqualTo("route.method-not-allowed");
    }

    @Test
    void protectedEndpointsNeedTheRightCaller() {
        assertThat(anonymous.get("/api/v1/things/mine").status()).isEqualTo(401);
        assertThat(anonymous.get("/api/v1/things/mine").challenge()).isEqualTo(Callers.BASIC_CHALLENGE);
        assertThat(anonymous.asBrowser().get("/api/v1/things/mine").challenge()).isNull();
        assertThat(anonymous.as("ayse@example.com", "wrong").get("/api/v1/things/mine").status()).isEqualTo(401);
        assertThat(anonymous.as("ayse@example.com", "secret").get("/api/v1/things/mine").json().asLong())
                .isEqualTo(1);
        assertThat(anonymous.as("ayse@example.com", "secret").get("/api/v1/things/admin").status()).isEqualTo(403);
        assertThat(anonymous.as("admin@example.com", "secret").get("/api/v1/things/admin").status()).isEqualTo(200);
    }

    @Test
    void problemsCarryExtensionMembers() {
        HttpTestClient.Reply reply = anonymous.get("/api/v1/things/conflict");

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.json().get("adjustments").get(0).asText()).isEqualTo("x");
    }

    @Test
    void everyAnswerCarriesACorrelationIdAndProblemsRepeatIt() {
        HttpTestClient.Reply ok = anonymous.get("/api/v1/things/7");
        assertThat(ok.header(CorrelationIds.HEADER)).matches("[0-9a-f-]{36}");

        HttpTestClient.Reply missing = anonymous.withHeader(CorrelationIds.HEADER, "req-42").get("/api/v1/things/404");
        assertThat(missing.header(CorrelationIds.HEADER)).isEqualTo("req-42");
        assertThat(missing.json().get("correlationId").asText()).isEqualTo("req-42");

        HttpTestClient.Reply boom = anonymous.withHeader(CorrelationIds.HEADER, "bad id with spaces").get("/api/v1/things/boom");
        assertThat(boom.json().get("correlationId").asText())
                .isEqualTo(boom.header(CorrelationIds.HEADER))
                .isNotEqualTo("bad id with spaces");
        assertThat(anonymous.as("ayse.com", "wrong").get("/api/v1/things/mine").json().has("correlationId"))
                .isTrue();
        assertThat(CorrelationIds.current()).isNull();
    }

    @Test
    void unexpectedFailuresAreOpaque500s() {
        HttpTestClient.Reply reply = anonymous.get("/api/v1/things/boom");

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.body()).doesNotContain("hunter2");
    }

    /** A thing with every kind of value the API writes. */
    public record Thing(BookId id, String title, Money price, Year year, Instant at) {
    }

    /** A request with format rules. */
    public record NewThing(@NotBlank String name, @Min(1) @Max(10) int quantity) {
    }

    /** A resource that exercises the boundary. */
    @Path("things")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class Things {

        @GET
        @Path("{id}")
        public Thing get(@PathParam("id") String id) {
            long value = Params.id("id", id);
            if (value == 404) {
                throw new NotFoundException("Thing", value);
            }
            return new Thing(BookId.of(value), "Dune", Money.of("95.00"), Year.of(1965),
                    Instant.parse("2026-01-02T03:04:05Z"));
        }

        @POST
        public Response create(@Valid NewThing thing) {
            return Response.created(URI.create("things/9")).entity(thing).build();
        }

        @GET
        @Path("mine")
        public long mine(@Context SecurityContext security) {
            return Callers.customer(security).customerId().value();
        }

        @GET
        @Path("admin")
        public String admin(@Context SecurityContext security) {
            return Callers.administrator(security).role();
        }

        @GET
        @Path("conflict")
        public String conflict() {
            throw WebException.conflict("cart.changed", "The cart changed", Map.of("adjustments", List.of("x")));
        }

        @GET
        @Path("boom")
        public String boom() {
            throw new IllegalStateException("database password is hunter2");
        }
    }
}
