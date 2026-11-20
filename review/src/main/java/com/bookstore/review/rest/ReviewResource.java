package com.bookstore.review.rest;

import com.bookstore.review.api.ReviewCommand;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.review.api.ReviewView;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;

import java.net.URI;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * A book's reviews (REV-1 – REV-5). Reading is open to everyone; writing needs a signed-in customer
 * who received the book. A customer has at most one review per book, so it is addressed as
 * {@code reviews/mine}. The rating breakdown is served by {@link RatingResource}.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/books/{id}/reviews}</td><td>the reviews, newest first</td></tr>
 *   <tr><td>{@code POST /api/v1/books/{id}/reviews}</td><td>write a review</td></tr>
 *   <tr><td>{@code GET|PUT|DELETE /api/v1/books/{id}/reviews/mine}</td><td>the customer's review</td></tr>
 *   <tr><td>{@code GET /api/v1/books/{id}/reviews/eligibility}</td><td>whether the customer may write one</td></tr>
 * </table>
 */
@Path("books/{id}/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ReviewResource {

    private final ReviewFacade reviews;

    /**
     * Creates the resource.
     *
     * @param reviews the review use cases
     */
    @Inject
    public ReviewResource(ReviewFacade reviews) {
        this.reviews = reviews;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected ReviewResource() {
        this(null);
    }

    /**
     * The body of {@code POST .../reviews} and {@code PUT .../reviews/mine}.
     *
     * @param stars   the rating, from 1 to 5
     * @param comment the comment, may be {@code null}
     */
    public record ReviewRequest(@Min(1) @Max(5) int stars, @Size(max = 2000) String comment) {
    }

    /** @param id the book @param page the page @param size the size @return the reviews */
    @GET
    public PageResponse<ReviewView> list(@PathParam("id") String id, @QueryParam("page") String page,
                                         @QueryParam("size") String size) {
        return PageResponse.of(reviews.reviewsOf(book(id), PageResponse.pageOf(page, size)));
    }

    /** @param security the request's security @param id the book @param review the review @return 201 with it */
    @POST
    public Response write(@Context SecurityContext security, @PathParam("id") String id,
                          @Valid @NotNull ReviewRequest review) {
        BookId bookId = book(id);
        ReviewView written = reviews.write(new ReviewCommand(me(security), bookId, review.stars(), review.comment()));
        return Response.created(URI.create("books/" + bookId + "/reviews/mine")).entity(written).build();
    }

    /** @param security the request's security @param id the book @return the customer's review */
    @GET
    @Path("mine")
    public ReviewView mine(@Context SecurityContext security, @PathParam("id") String id) {
        BookId bookId = book(id);
        return reviews.myReview(me(security), bookId)
                .orElseThrow(() -> new NotFoundException("Review", "of book " + bookId + " by you"));
    }

    /** @param security the request's security @param id the book @param review the new review @return it */
    @PUT
    @Path("mine")
    public ReviewView edit(@Context SecurityContext security, @PathParam("id") String id,
                           @Valid @NotNull ReviewRequest review) {
        return reviews.edit(new ReviewCommand(me(security), book(id), review.stars(), review.comment()));
    }

    /** @param security the request's security @param id the book */
    @DELETE
    @Path("mine")
    public void delete(@Context SecurityContext security, @PathParam("id") String id) {
        reviews.delete(me(security), book(id));
    }

    /** @param security the request's security @param id the book @return {@code {"canReview": …}} */
    @GET
    @Path("eligibility")
    public Map<String, Boolean> eligibility(@Context SecurityContext security, @PathParam("id") String id) {
        return Map.of("canReview", reviews.canReview(me(security), book(id)));
    }

    private static BookId book(String id) {
        return BookId.of(Params.id("id", id));
    }

    private static CustomerId me(SecurityContext security) {
        return Callers.customer(security).customerId();
    }
}
