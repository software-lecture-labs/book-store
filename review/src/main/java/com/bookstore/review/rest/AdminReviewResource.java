package com.bookstore.review.rest;

import com.bookstore.review.api.ReviewAdminFacade;
import com.bookstore.review.api.ReviewView;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Moderating reviews (ADM-7); for {@code ADMIN} only (ACC-6).
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/admin/reviews}</td><td>every review, newest first</td></tr>
 *   <tr><td>{@code DELETE /api/v1/admin/reviews/{id}}</td><td>remove an unsuitable review</td></tr>
 * </table>
 */
@Path("admin/reviews")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminReviewResource {

    private final ReviewAdminFacade reviews;

    /**
     * Creates the resource.
     *
     * @param reviews the review moderation use cases
     */
    @Inject
    public AdminReviewResource(ReviewAdminFacade reviews) {
        this.reviews = reviews;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminReviewResource() {
        this(null);
    }

    /** @param security the request's security @param page the page @param size its size @return the reviews */
    @GET
    public PageResponse<ReviewView> list(@Context SecurityContext security, @QueryParam("page") String page,
                                         @QueryParam("size") String size) {
        Callers.administrator(security);
        return PageResponse.of(reviews.reviews(PageResponse.pageOf(page, size)));
    }

    /** @param security the request's security @param id the review @return 204 */
    @DELETE
    @Path("{id}")
    public Response remove(@Context SecurityContext security, @PathParam("id") String id) {
        Callers.administrator(security);
        reviews.remove(Params.id("id", id));
        return Response.noContent().build();
    }
}
