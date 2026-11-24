package com.bookstore.customer.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.customer.application.CustomerShop;
import com.bookstore.customer.rest.FavoriteResource;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The account endpoints over HTTP, with HTTP Basic sign-in, on the in-memory accounts. */
class AccountResourceTest {

    private static final String REGISTRATION = """
            {"email": "ayse@example.com", "firstName": "Ayşe", "lastName": "Yılmaz", "password": "%s"}
            """;
    private static final String ADDRESS = """
            {"label": "Ev", "recipient": "Ayşe Yılmaz", "phone": "05551234567", "line": "Kitap Sk. 5",
             "district": "Kadıköy", "city": "İstanbul", "postalCode": "34710", "makeDefault": true}
            """;

    private final CustomerShop shop = new CustomerShop();
    private final RestTestServer server = RestTestServer.start(new AccountAuthenticator(shop.accounts),
            new AccountResource(shop.accounts), new RegistrationResource(shop.accounts),
            new FavoriteResource(shop.favorites));
    private final HttpTestClient anonymous = server.client();
    private final HttpTestClient ayse = anonymous.as("ayse@example.com", CustomerShop.PASSWORD);

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void registeringCreatesAnAccountThatCanSignIn() {
        HttpTestClient.Reply created = anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.location()).endsWith("/api/v1/me");
        assertThat(created.json().get("role").asText()).isEqualTo("CUSTOMER");
        assertThat(created.body()).doesNotContain(CustomerShop.PASSWORD);
        assertThat(ayse.get("/api/v1/me").json().get("email").asText()).isEqualTo("ayse@example.com");
    }

    @Test
    void formatErrorsAre400AndRuleErrors422And409() {
        HttpTestClient.Reply malformed = anonymous.post("/api/v1/customers",
                "{\"email\": \"not-an-email\", \"firstName\": \"\", \"lastName\": \"Y\", \"password\": \"x\"}");
        assertThat(malformed.status()).isEqualTo(400);
        assertThat(malformed.json().get("errors").findValuesAsText("field")).containsExactly("email", "firstName");

        HttpTestClient.Reply weak = anonymous.post("/api/v1/customers", REGISTRATION.formatted("short"));
        assertThat(weak.status()).isEqualTo(422);

        anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));
        HttpTestClient.Reply twice = anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));
        assertThat(twice.status()).isEqualTo(409);
        assertThat(twice.code()).isEqualTo("email.taken");
    }

    @Test
    void theProfileNeedsTheRightPassword() {
        anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));

        assertThat(anonymous.get("/api/v1/me").status()).isEqualTo(401);
        assertThat(anonymous.as("ayse@example.com", "wrong-password").get("/api/v1/me").status()).isEqualTo(401);
    }

    @Test
    void addressBookOverHttp() {
        anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));

        HttpTestClient.Reply added = ayse.post("/api/v1/me/addresses", ADDRESS);
        assertThat(added.status()).isEqualTo(201);
        String location = added.location();
        JsonNode address = ayse.get(location).json();
        assertThat(address.get("label").asText()).isEqualTo("Ev");
        assertThat(address.get("isDefault").asBoolean()).isTrue();
        assertThat(address.get("address").get("postalCode").asText()).isEqualTo("34710");

        assertThat(ayse.post("/api/v1/me/addresses", ADDRESS.replace("34710", "347")).status()).isEqualTo(400);
        assertThat(ayse.delete(location).status()).isEqualTo(204);
        assertThat(ayse.get(location).status()).isEqualTo(404);
    }

    @Test
    void passwordChangeTakesEffectAtOnce() {
        anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));

        HttpTestClient.Reply changed = ayse.put("/api/v1/me/password",
                "{\"currentPassword\": \"" + CustomerShop.PASSWORD + "\", \"newPassword\": \"yeni-parola-2026\"}");

        assertThat(changed.status()).isEqualTo(204);
        assertThat(ayse.get("/api/v1/me").status()).isEqualTo(401);
        assertThat(anonymous.as("ayse@example.com", "yeni-parola-2026").get("/api/v1/me").status()).isEqualTo(200);
    }

    @Test
    void favoritesAreCustomerSpecificAndCanBeAddedRemovedAndListed() {
        anonymous.post("/api/v1/customers", REGISTRATION.formatted(CustomerShop.PASSWORD));

        assertThat(anonymous.get("/api/v1/me/favorites").status()).isEqualTo(401);
        assertThat(ayse.put("/api/v1/me/favorites/3", "").status()).isEqualTo(204);
        assertThat(ayse.put("/api/v1/me/favorites/3", "").status()).isEqualTo(409);
        assertThat(ayse.get("/api/v1/me/favorites").json().get("content").get(0).asLong()).isEqualTo(3);
        assertThat(ayse.delete("/api/v1/me/favorites/3").status()).isEqualTo(204);
        assertThat(ayse.get("/api/v1/me/favorites").json().get("totalElements").asLong()).isZero();
    }
}
