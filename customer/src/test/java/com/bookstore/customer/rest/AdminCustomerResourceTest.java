package com.bookstore.customer.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.customer.application.CustomerShop;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Caller;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Managing accounts over HTTP (ADM-6, ACC-6, ACC-7). */
class AdminCustomerResourceTest {

    private final CustomerShop shop = new CustomerShop();
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final RestTestServer server = RestTestServer.start((user, password) -> switch (user) {
        case "admin" -> Optional.of(new Caller(CustomerId.of(999), Caller.ADMIN));
        default -> new AccountAuthenticator(shop.accounts).authenticate(user, password);
    }, new AdminCustomerResource(shop.admin));
    private final HttpTestClient anonymous = server.client();
    private final HttpTestClient admin = anonymous.as("admin", "pw");

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void theAdministratorListsAccountsAndSwitchesThemOff() {
        assertThat(anonymous.get("/api/v1/admin/customers").status()).isEqualTo(401);
        assertThat(anonymous.as("ayse@example.com", CustomerShop.PASSWORD).get("/api/v1/admin/customers").status())
                .isEqualTo(403);

        assertThat(admin.get("/api/v1/admin/customers").json().get("content").findValuesAsText("email"))
                .containsExactly("ayse@example.com");
        assertThat(admin.put("/api/v1/admin/customers/" + ayse.value() + "/active", "{\"active\": false}").json()
                .get("active").asBoolean()).isFalse();

        assertThat(anonymous.as("ayse@example.com", CustomerShop.PASSWORD).get("/api/v1/admin/customers").status())
                .isEqualTo(401);
        assertThat(admin.put("/api/v1/admin/customers/" + ayse.value() + "/active", "{}").code())
                .isEqualTo("request.invalid");
        assertThat(admin.put("/api/v1/admin/customers/12345/active", "{\"active\": true}").status()).isEqualTo(404);
    }
}
