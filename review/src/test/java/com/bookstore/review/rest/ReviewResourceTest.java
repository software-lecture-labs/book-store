package com.bookstore.review.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.review.application.ReviewAdminService;
import com.bookstore.review.application.ReviewFakes;
import com.bookstore.review.application.ReviewService;
import com.bookstore.review.persistence.InMemoryReviewRepository;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestClock;
import com.bookstore.shared.web.Caller;
import com.bookstore.shared.web.HttpTestClient;
import com.bookstore.shared.web.RestTestServer;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The review endpoints over HTTP, with the other modules faked. */
class ReviewResourceTest {

    private static final BookId DUNE = BookId.of(1);
    private static final CustomerId AYSE = CustomerId.of(10);
    private static final String REVIEWS = "/api/v1/books/1/reviews";

    private final ReviewFakes.FakePurchases purchases = new ReviewFakes.FakePurchases();
    private final InMemoryReviewRepository repository = new InMemoryReviewRepository();
    private final ReviewFakes.FakeRatings ratings = new ReviewFakes.FakeRatings(DUNE);
    private final ReviewFakes.FakeNames names = new ReviewFakes.FakeNames(Map.of(AYSE, "Ayşe Y."));
    private final ReviewService service = new ReviewService(repository, ratings, purchases, names,
            new DirectTransactions(), TestClock.CLOCK);
    private final RestTestServer server = RestTestServer.start((user, password) -> switch (user) {
        case "ayse" -> Optional.of(new Caller(AYSE, Caller.CUSTOMER));
        case "admin" -> Optional.of(new Caller(CustomerId.of(1), Caller.ADMIN));
        default -> Optional.empty();
    }, new ReviewResource(service), new RatingResource(service),
            new AdminReviewResource(new ReviewAdminService(repository, ratings, names, new DirectTransactions())));
    private final HttpTestClient anonymous = server.client();
    private final HttpTestClient ayse = anonymous.as("ayse", "pw");

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void onlyCustomersWhoReceivedTheBookMayReviewIt() {
        assertThat(anonymous.post(REVIEWS, "{\"stars\": 5}").status()).isEqualTo(401);
        assertThat(ayse.get(REVIEWS + "/eligibility").json().get("canReview").asBoolean()).isFalse();
        HttpTestClient.Reply refused = ayse.post(REVIEWS, "{\"stars\": 5}");
        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.code()).isEqualTo("review.not-eligible");
    }

    @Test
    void writingReadingEditingAndDeletingAReview() {
        purchases.received(AYSE, DUNE);

        HttpTestClient.Reply written = ayse.post(REVIEWS, "{\"stars\": 4, \"comment\": \"Güzel\"}");
        assertThat(written.status()).isEqualTo(201);
        assertThat(written.location()).endsWith(REVIEWS + "/mine");
        assertThat(anonymous.get(REVIEWS).json().get("content").get(0).get("authorName").asText())
                .isEqualTo("Ayşe Y.");
        assertThat(anonymous.get("/api/v1/books/1/ratings").json().get("counts").get(3).asInt()).isEqualTo(1);
        assertThat(ayse.post(REVIEWS, "{\"stars\": 3}").status()).isEqualTo(409);

        assertThat(ayse.put(REVIEWS + "/mine", "{\"stars\": 5}").json().get("edited").asBoolean()).isFalse();
        assertThat(ayse.get(REVIEWS + "/mine").json().get("stars").asInt()).isEqualTo(5);
        assertThat(ayse.delete(REVIEWS + "/mine").status()).isEqualTo(204);
        assertThat(ayse.get(REVIEWS + "/mine").status()).isEqualTo(404);
    }

    @Test
    void theAdministratorRemovesAnUnsuitableReview() {
        purchases.received(AYSE, DUNE);
        long id = ayse.post(REVIEWS, "{\"stars\": 1, \"comment\": \"spam\"}").json().get("id").asLong();
        HttpTestClient admin = anonymous.as("admin", "pw");

        assertThat(ayse.get("/api/v1/admin/reviews").status()).isEqualTo(403);
        assertThat(admin.get("/api/v1/admin/reviews").json().get("content").findValuesAsText("comment"))
                .containsExactly("spam");
        assertThat(admin.delete("/api/v1/admin/reviews/" + id).status()).isEqualTo(204);

        assertThat(anonymous.get(REVIEWS).json().get("totalElements").asInt()).isZero();
        assertThat(ratings.count(DUNE)).isZero();
        assertThat(admin.delete("/api/v1/admin/reviews/" + id).status()).isEqualTo(404);
    }

    @Test
    void starsOutsideOneToFiveAre400() {
        purchases.received(AYSE, DUNE);

        assertThat(ayse.post(REVIEWS, "{\"stars\": 6}").status()).isEqualTo(400);
        assertThat(ayse.post(REVIEWS, "{\"stars\": 0}").status()).isEqualTo(400);
    }
}
