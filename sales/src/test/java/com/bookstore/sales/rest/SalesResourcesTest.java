package com.bookstore.sales.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.application.FakeCatalog;
import com.bookstore.sales.application.SalesShop;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.web.Caller;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The cart, order and order management endpoints over HTTP, on the in-memory sales module. */
class SalesResourcesTest {

    private final SalesShop shop = new SalesShop();
    private final FakeCatalog.Item dune = shop.book("Dune", "100.00", 5);
    private final CustomerId ayseId = shop.customer("ayse@example.com");
    private final CustomerId mehmetId = shop.customer("mehmet@example.com");
    private final RestTestServer server;
    private final HttpTestClient anonymous;
    private final HttpTestClient ayse;
    private final HttpTestClient mehmet;
    private final HttpTestClient admin;
    private final GuestCart visitorCart = new GuestCart();

    SalesResourcesTest() {
        server = RestTestServer.start((user, password) -> switch (user) {
            case "ayse" -> Optional.of(new Caller(ayseId, "CUSTOMER"));
            case "mehmet" -> Optional.of(new Caller(mehmetId, "CUSTOMER"));
            case "admin" -> Optional.of(new Caller(CustomerId.of(999), Caller.ADMIN));
            default -> Optional.empty();
        }, new CartResource(shop.cart, () -> visitorCart), new OrderResource(shop.checkout, shop.myOrders),
                new OrderAdminResource(shop.orderAdmin));
        anonymous = server.client();
        ayse = anonymous.as("ayse", "pw");
        mehmet = anonymous.as("mehmet", "pw");
        admin = anonymous.as("admin", "pw");
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void aVisitorHasACartOfTheirOwnThatSigningInDoesNotShow() {
        JsonNode cart = anonymous.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}")
                .json();

        assertThat(cart.get("totalQuantity").asInt()).isEqualTo(1);
        assertThat(visitorCart.quantityOf(dune.id())).isEqualTo(1);
        assertThat(anonymous.get("/api/v1/cart").json().get("totalQuantity").asInt()).isEqualTo(1);
        assertThat(ayse.get("/api/v1/cart").json().get("totalQuantity").asInt()).isZero();
        assertThat(anonymous.delete("/api/v1/cart").json().get("lines").isEmpty()).isTrue();
    }

    @Test
    void theCartBelongsToTheSignedInCustomer() {
        JsonNode cart = ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 2}").json();
        assertThat(cart.get("totalQuantity").asInt()).isEqualTo(2);
        assertThat(cart.get("lines").get(0).get("bookId").asLong()).isEqualTo(dune.id().value());
        assertThat(cart.get("total").get("amount").decimalValue()).isEqualByComparingTo("249.90");

        assertThat(mehmet.get("/api/v1/cart").json().get("totalQuantity").asInt()).isZero();
        assertThat(ayse.put("/api/v1/cart/items/" + dune.id().value(), "{\"quantity\": 0}").json()
                .get("lines").isEmpty()).isTrue();
    }

    @Test
    void cartRequestsAreValidated() {
        HttpTestClient.Reply reply = ayse.post("/api/v1/cart/items", "{\"quantity\": 11}");

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json().get("errors").findValuesAsText("field")).containsExactly("bookId", "quantity");
        assertThat(ayse.post("/api/v1/cart/items", "{\"bookId\": 999, \"quantity\": 1}").status()).isEqualTo(404);
    }

    @Test
    void placingAnOrderAndReadingItBack() {
        ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");

        HttpTestClient.Reply placed = ayse.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId)));

        assertThat(placed.status()).isEqualTo(201);
        String number = placed.json().get("orderNumber").asText();
        assertThat(placed.location()).endsWith("/api/v1/orders/" + number);
        assertThat(placed.json().get("status").asText()).isEqualTo("PAID");
        assertThat(ayse.get("/api/v1/orders").json().get("totalElements").asInt()).isEqualTo(1);
        assertThat(ayse.get(placed.location()).json().get("lines").get(0).get("bookTitle").asText()).isEqualTo("Dune");
        assertThat(mehmet.get(placed.location()).status()).isEqualTo(403);
    }

    @Test
    void theSameIdempotencyKeyPlacesOneOrder() {
        ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");
        HttpTestClient once = ayse.withHeader("Idempotency-Key", "checkout-7f3a");

        HttpTestClient.Reply first = once.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId)));
        HttpTestClient.Reply repeat = once.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId)));

        assertThat(first.status()).isEqualTo(201);
        assertThat(repeat.status()).isEqualTo(201);
        assertThat(repeat.location()).isEqualTo(first.location());
        assertThat(repeat.json().get("orderNumber")).isEqualTo(first.json().get("orderNumber"));
        assertThat(ayse.get("/api/v1/orders").json().get("totalElements").asInt()).isEqualTo(1);
        assertThat(once.post("/api/v1/orders", "{\"addressId\": " + shop.defaultAddressId(ayseId)
                + ", \"paymentMethod\": \"BANK_TRANSFER\"}").code()).isEqualTo("idempotency.key-reused");
        assertThat(ayse.withHeader("Idempotency-Key", "x".repeat(101))
                .post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId))).code())
                .isEqualTo("request.bad-parameter");
    }

    @Test
    void checkoutRulesComeBackAsProblems() {
        assertThat(ayse.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId))).status()).isEqualTo(422);
        assertThat(ayse.post("/api/v1/orders", "{\"addressId\": 1, \"paymentMethod\": \"GOLD\"}").status())
                .isEqualTo(422);
        assertThat(ayse.post("/api/v1/orders", "{\"paymentMethod\": \"CREDIT_CARD\"}").status()).isEqualTo(400);
    }

    @Test
    void aCartThatChangedAtCheckoutIsA409WithItsAdjustments() {
        ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");
        dune.changePrice(Money.of("90.00"));

        HttpTestClient.Reply reply = ayse.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId)));

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.code()).isEqualTo("cart.changed");
        JsonNode adjustment = reply.json().get("adjustments").get(0);
        assertThat(adjustment.get("type").asText()).isEqualTo("PRICE_CHANGED");
        assertThat(adjustment.get("toPrice").get("amount").decimalValue()).isEqualByComparingTo("90.00");
    }

    @Test
    void onlyAdministratorsManageOrders() {
        ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");
        String number = ayse.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId))).json()
                .get("orderNumber").asText();
        String statusPath = "/api/v1/admin/orders/" + number + "/status";

        assertThat(anonymous.get("/api/v1/admin/orders").status()).isEqualTo(401);
        assertThat(ayse.get("/api/v1/admin/orders").status()).isEqualTo(403);
        assertThat(ayse.put(statusPath, "{\"status\": \"SHIPPED\"}").status()).isEqualTo(403);

        assertThat(admin.put(statusPath, "{\"status\": \"SHIPPED\"}").json().get("status").asText())
                .isEqualTo("SHIPPED");
        assertThat(admin.get("/api/v1/admin/orders?status=SHIPPED").json().get("totalElements").asInt()).isEqualTo(1);
        assertThat(admin.put(statusPath, "{\"status\": \"CREATED\"}").status()).isEqualTo(409);
        assertThat(admin.put(statusPath, "{\"status\": \"LOST\"}").status()).isEqualTo(422);
    }

    @Test
    void administratorsFilterOrdersByNumberAndInclusiveDateRange() {
        ayse.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");
        HttpTestClient.Reply placed = ayse.post("/api/v1/orders", placeOrder(shop.defaultAddressId(ayseId)));
        String number = placed.json().get("orderNumber").asText();
        String date = placed.json().get("placedAt").asText().substring(0, 10);
        mehmet.post("/api/v1/cart/items", "{\"bookId\": " + dune.id().value() + ", \"quantity\": 1}");
        mehmet.post("/api/v1/orders", placeOrder(shop.defaultAddressId(mehmetId)));

        JsonNode filtered = admin.get("/api/v1/admin/orders?status=PAID&orderNumber="
                + number.substring(number.length() - 2) + "&from=" + date + "&to=" + date).json();

        assertThat(filtered.get("totalElements").asInt()).isEqualTo(1);
        assertThat(filtered.get("content").get(0).get("orderNumber").asText()).isEqualTo(number);
    }

    @Test
    void administratorsCannotPassMalformedOrInvertedDates() {
        assertThat(admin.get("/api/v1/admin/orders?from=not-a-date").status()).isEqualTo(400);
        assertThat(admin.get("/api/v1/admin/orders?from=2026-11-25&to=2026-11-24").status()).isEqualTo(422);
    }

    private static String placeOrder(long addressId) {
        return "{\"addressId\": " + addressId + ", \"paymentMethod\": \"CREDIT_CARD\"}";
    }
}
