package com.bookstore.catalog.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.application.CatalogShop;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.shared.web.Authenticator;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The catalog's endpoints over HTTP, on the in-memory catalog. */
class CatalogResourceTest {

    private final CatalogShop shop = new CatalogShop();
    private final Author herbert = shop.author("Frank Herbert");
    private final Book dune = shop.book("Dune", "350.00", 4, shop.novels, herbert);
    private final Book madonna = shop.book("Kürk Mantolu Madonna", "120.00", 0);
    private final RestTestServer server = RestTestServer.start(Authenticator.NOBODY,
            new CatalogResource(shop.catalog), new AuthorResource(shop.catalog), new CategoryResource(shop.catalog));
    private final HttpTestClient client = server.client();

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void searchReturnsAPageOfSummaries() {
        JsonNode page = client.get("/api/v1/books?sort=price&order=desc&size=10").json();

        assertThat(page.get("totalElements").asInt()).isEqualTo(2);
        assertThat(page.get("content").findValuesAsText("title")).containsExactly("Dune", "Kürk Mantolu Madonna");
        assertThat(page.get("content").get(0).get("id").asLong()).isEqualTo(dune.id().value());
        assertThat(page.get("content").get(0).get("price").get("amount").decimalValue())
                .isEqualByComparingTo("350.00");
    }

    @Test
    void searchFiltersByTextStockAndPrice() {
        assertThat(client.get("/api/v1/books?q=MANTOLU").json().get("content").findValuesAsText("title"))
                .containsExactly("Kürk Mantolu Madonna");
        assertThat(client.get("/api/v1/books?inStock=true").json().get("totalElements").asInt()).isEqualTo(1);
        assertThat(client.get("/api/v1/books?maxPrice=200").json().get("content").findValuesAsText("title"))
                .containsExactly("Kürk Mantolu Madonna");
        assertThat(client.get("/api/v1/books?minPrice=300&maxPrice=400").json().get("content").findValuesAsText("title"))
                .containsExactly("Dune");
    }

    @Test
    void searchFiltersByAuthorAndPriceRangeTogether() {
        String path = "/api/v1/books?author=" + herbert.id().value() + "&minPrice=300&maxPrice=400";

        assertThat(client.get(path).json().get("content").findValuesAsText("title")).containsExactly("Dune");
    }

    @Test
    void authorsArePubliclyAvailableForSearchFilters() {
        JsonNode authors = client.get("/api/v1/authors").json();

        assertThat(authors.findValuesAsText("name")).contains("Frank Herbert", "Author of Kürk Mantolu Madonna");
        assertThat(authors.findValues("id").stream().allMatch(JsonNode::isNumber)).isTrue();
    }

    @Test
    void badParametersAre400AndUnknownCodesAreRejected() {
        assertThat(client.get("/api/v1/books?maxPrice=cheap").status()).isEqualTo(400);
        assertThat(client.get("/api/v1/books?order=sideways").status()).isEqualTo(400);
        assertThat(client.get("/api/v1/books?inStock=maybe").status()).isEqualTo(400);
        assertThat(client.get("/api/v1/books?sort=colour").code()).isEqualTo("validation.failed");
        assertThat(client.get("/api/v1/books?category=999").status()).isEqualTo(404);
    }

    @Test
    void bookDetailsAndMissingBooks() {
        JsonNode details = client.get("/api/v1/books/" + madonna.id().value()).json();

        assertThat(details.get("title").asText()).isEqualTo("Kürk Mantolu Madonna");
        assertThat(details.get("language").asText()).isEqualTo("tr");
        assertThat(details.get("inStock").asBoolean()).isFalse();
        assertThat(client.get("/api/v1/books/999").status()).isEqualTo(404);
        assertThat(client.get("/api/v1/books/0").status()).isEqualTo(400);
    }

    @Test
    void categoryTreeHasChildren() {
        JsonNode tree = client.get("/api/v1/categories").json();

        assertThat(tree.findValuesAsText("name")).contains("Literature", "Novel", "Poetry", "Science");
        assertThat(tree.get(0).get("children").get(0).get("id").isNumber()).isTrue();
    }
}
