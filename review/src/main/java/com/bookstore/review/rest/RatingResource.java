package com.bookstore.review.rest;

import com.bookstore.review.api.RatingBreakdown;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.web.Params;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code GET /api/v1/books/{id}/ratings}: how a book's ratings are spread (REV-6). A class of its
 * own because JAX-RS picks the resource class by its path alone: under {@code books/{id}} it would
 * also take {@code /books/{id}} away from the catalog.
 */
@Path("books/{id}/ratings")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class RatingResource {

    private final ReviewFacade reviews;

    /**
     * Creates the resource.
     *
     * @param reviews the review use cases
     */
    @Inject
    public RatingResource(ReviewFacade reviews) {
        this.reviews = reviews;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected RatingResource() {
        this(null);
    }

    /** @param id the book @return the breakdown */
    @GET
    public RatingBreakdown breakdown(@PathParam("id") String id) {
        return reviews.ratingBreakdown(BookId.of(Params.id("id", id)));
    }
}
