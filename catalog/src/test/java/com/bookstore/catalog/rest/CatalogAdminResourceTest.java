package com.bookstore.catalog.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.application.CatalogShop;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.CatalogTestData;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Caller;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The catalog's administration endpoints over HTTP (ADM-1 – ADM-3, ADM-8, ACC-6). */
class CatalogAdminResourceTest {

    private final CatalogShop shop = new CatalogShop();
    private final Author ali = shop.storedAuthor("Sabahattin Ali");
    private final Book dune = shop.book("Dune", "350.00", 4);
    private final RestTestServer server = RestTestServer.start((user, password) -> switch (user) {
        case "admin" -> Optional.of(new Caller(CustomerId.of(1), Caller.ADMIN));
        case "ayse" -> Optional.of(new Caller(CustomerId.of(2), Caller.CUSTOMER));
        default -> Optional.empty();
    }, new AdminBookResource(shop.admin), new AdminAuthorResource(shop.admin), new AdminCategoryResource(shop.admin),
            new CatalogResource(shop.catalog), new AuthorResource(shop.catalog), new CategoryResource(shop.catalog));
    private final HttpTestClient anonymous = server.client();
    private final HttpTestClient customer = anonymous.as("ayse", "pw");
    private final HttpTestClient admin = anonymous.as("admin", "pw");

    @AfterEach
    void stop() {
        server.close();
    }

    private String madonna(String isbn) {
        return """
                {"isbn": "%s", "title": "Kürk Mantolu Madonna", "authorIds": [%d], "categoryId": %d,
                 "publisher": "YKY", "publicationYear": 1943, "language": "tr", "pageCount": 160,
                 "description": null, "price": 95.00, "stock": 12}
                """.formatted(isbn, ali.id().value(), shop.novels.id().value());
    }

    @Test
    void onlyAdministratorsGetIn() {
        assertThat(anonymous.get("/api/v1/admin/books").status()).isEqualTo(401);
        assertThat(customer.get("/api/v1/admin/books").status()).isEqualTo(403);
        assertThat(customer.post("/api/v1/admin/authors", "{\"name\": \"X\"}").status()).isEqualTo(403);
        assertThat(admin.get("/api/v1/admin/books").status()).isEqualTo(200);
    }

    @Test
    void aBookIsAddedChangedAndTakenOffSale() {
        HttpTestClient.Reply created = admin.post("/api/v1/admin/books", madonna("9789753638029"));
        assertThat(created.status()).isEqualTo(201);
        long id = created.json().get("id").asLong();
        assertThat(created.location()).endsWith("/api/v1/admin/books/" + id);
        assertThat(anonymous.get("/api/v1/books/" + id).json().get("title").asText()).isEqualTo("Kürk Mantolu Madonna");

        assertThat(admin.put("/api/v1/admin/books/" + id + "/price", "{\"price\": 99.90}").json()
                .get("price").get("amount").decimalValue()).isEqualByComparingTo("99.90");
        assertThat(admin.put("/api/v1/admin/books/" + id + "/stock", "{\"stock\": 0}").json()
                .get("inStock").asBoolean()).isFalse();
        assertThat(admin.put("/api/v1/admin/books/" + id + "/active", "{\"active\": false}").json()
                .get("active").asBoolean()).isFalse();

        assertThat(anonymous.get("/api/v1/books/" + id).status()).isEqualTo(404);
        JsonNode list = admin.get("/api/v1/admin/books?q=madonna").json();
        assertThat(list.get("content").get(0).get("active").asBoolean()).isFalse();
        assertThat(admin.get("/api/v1/admin/books/" + id).json().get("active").asBoolean()).isFalse();
    }

    @Test
    void theEntityTagGuardsAgainstLostUpdates() {
        HttpTestClient.Reply created = admin.post("/api/v1/admin/books", madonna("9789753638029"));
        String book = "/api/v1/admin/books/" + created.json().get("id").asLong();
        String tag = admin.get(book).header("ETag");
        assertThat(tag).isEqualTo(created.header("ETag")).matches("\"\\d+\"");

        HttpTestClient.Reply first = admin.withHeader("If-Match", tag).put(book + "/price", "{\"price\": 90}");
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.header("ETag")).isNotEqualTo(tag);

        HttpTestClient.Reply second = admin.withHeader("If-Match", tag).put(book + "/stock", "{\"stock\": 1}");
        assertThat(second.status()).isEqualTo(412);
        assertThat(second.code()).isEqualTo("precondition.failed");
        assertThat(admin.withHeader("If-Match", "W/\"1\"").put(book + "/stock", "{\"stock\": 1}").status())
                .isEqualTo(412);
        assertThat(admin.withHeader("If-Match", "*").put(book + "/stock", "{\"stock\": 1}").status()).isEqualTo(200);
        assertThat(admin.put(book + "/stock", "{\"stock\": 2}").status()).isEqualTo(200);
        assertThat(admin.get(book).json().get("stock").asInt()).isEqualTo(2);
    }

    @Test
    void mistakesComeBackAsProblems() {
        assertThat(admin.post("/api/v1/admin/books", "{\"title\": \"\"}").code()).isEqualTo("request.invalid");
        assertThat(admin.post("/api/v1/admin/books", madonna("12345")).code()).isEqualTo("validation.failed");
        admin.post("/api/v1/admin/books", madonna("9789753638029"));
        HttpTestClient.Reply twice = admin.post("/api/v1/admin/books", madonna("9789753638029"));
        assertThat(twice.status()).isEqualTo(409);
        assertThat(twice.code()).isEqualTo("book.isbn-taken");
        assertThat(admin.put("/api/v1/admin/books/999/price", "{\"price\": 1}").status()).isEqualTo(404);
    }

    @Test
    void authorsAndCategoriesAreManagedAndKeptWhileInUse() {
        long orhan = admin.post("/api/v1/admin/authors", "{\"name\": \"Orhan Pamuk\"}").json().get("id").asLong();
        assertThat(admin.get("/api/v1/admin/authors").json().findValuesAsText("name"))
                .contains("Orhan Pamuk", "Sabahattin Ali");
        assertThat(anonymous.get("/api/v1/authors").json().findValuesAsText("name"))
                .contains("Orhan Pamuk", "Sabahattin Ali");
        assertThat(admin.put("/api/v1/admin/authors/" + orhan, "{\"name\": \"O. Pamuk\"}").json().get("name").asText())
                .isEqualTo("O. Pamuk");
        assertThat(admin.delete("/api/v1/admin/authors/" + orhan).status()).isEqualTo(204);

        admin.post("/api/v1/admin/books", madonna("9789753638029"));
        assertThat(admin.delete("/api/v1/admin/authors/" + ali.id().value()).code()).isEqualTo("author.in-use");

        HttpTestClient.Reply art = admin.post("/api/v1/admin/categories", "{\"name\": \"Art\"}");
        assertThat(art.status()).isEqualTo(201);
        long artId = art.json().get("id").asLong();
        admin.post("/api/v1/admin/categories", "{\"name\": \"Painting\", \"parentId\": " + artId + "}");
        assertThat(anonymous.get("/api/v1/categories").json().findValuesAsText("name")).contains("Art", "Painting");
        assertThat(admin.delete("/api/v1/admin/categories/" + artId).code()).isEqualTo("category.has-children");
        assertThat(admin.delete("/api/v1/admin/categories/" + shop.novels.id().value()).code())
                .isEqualTo("category.in-use");
    }

    @Test
    void aCoverIsUploadedAndServedWithAnEntityTag() {
        String cover = "/api/v1/admin/books/" + dune.id().value() + "/cover";
        String publicCover = "/api/v1/books/" + dune.id().value() + "/cover";
        assertThat(anonymous.download(publicCover, null).status()).isEqualTo(404);
        assertThat(customer.upload(cover, "image/png", CatalogTestData.png()).status()).isEqualTo(403);

        HttpTestClient.Reply uploaded = admin.upload(cover, "image/png", CatalogTestData.png());
        assertThat(uploaded.status()).isEqualTo(200);
        assertThat(uploaded.json().get("coverImage").asText()).endsWith(".png");

        HttpTestClient.FileReply served = anonymous.download(publicCover, null);
        assertThat(served.status()).isEqualTo(200);
        assertThat(served.contentType()).isEqualTo("image/png");
        assertThat(served.body()).isEqualTo(CatalogTestData.png());
        assertThat(anonymous.download(publicCover, served.etag()).status()).isEqualTo(304);

        admin.upload(cover, "image/jpeg", CatalogTestData.jpeg());
        assertThat(anonymous.download(publicCover, served.etag()).status()).isEqualTo(200);
    }

    @Test
    void onlySmallJpegAndPngFilesAreAccepted() {
        String cover = "/api/v1/admin/books/" + dune.id().value() + "/cover";

        HttpTestClient.Reply gif = admin.upload(cover, "image/png", "GIF89a".getBytes());
        assertThat(gif.status()).isEqualTo(422);
        assertThat(gif.code()).isEqualTo("validation.failed");
        assertThat(admin.upload(cover, "image/gif", "GIF89a".getBytes()).status()).isEqualTo(415);
        byte[] large = Arrays.copyOf(CatalogTestData.png(), 3 * 1024 * 1024);
        assertThat(admin.upload(cover, "image/png", large).body()).contains("2 MB");
    }

    @Test
    void theOverviewListsBooksRunningLow() {
        JsonNode stats = admin.get("/api/v1/admin/books/stats?lowStockBelow=5").json();

        assertThat(stats.get("totalBooks").asInt()).isEqualTo(1);
        assertThat(stats.get("lowStock").findValuesAsText("title")).containsExactly("Dune");
        assertThat(admin.get("/api/v1/admin/books/stats").json().get("lowStockBelow").asInt()).isEqualTo(5);
        assertThat(dune.stock().quantity()).isEqualTo(4);
    }
}
