package com.bookstore.customer.rest;

import com.bookstore.customer.api.FavoriteFacade;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("me/favorites")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class FavoriteResource {
    private final FavoriteFacade favorites;

    @Inject
    public FavoriteResource(FavoriteFacade favorites) {
        this.favorites = favorites;
    }

    protected FavoriteResource() {
        this(null);
    }

    @GET
    public PageResponse<Long> list(@Context SecurityContext security,
                                   @QueryParam("page") String page,
                                   @QueryParam("size") String size) {
        return PageResponse.of(favorites.favorites(me(security), PageResponse.pageOf(page, size))
                .map(BookId::value));
    }

    @PUT
    @Path("{bookId}")
    public Response add(@Context SecurityContext security, @PathParam("bookId") String bookId) {
        favorites.add(me(security), BookId.of(Params.id("bookId", bookId)));
        return Response.noContent().build();
    }

    @DELETE
    @Path("{bookId}")
    public Response remove(@Context SecurityContext security, @PathParam("bookId") String bookId) {
        favorites.remove(me(security), BookId.of(Params.id("bookId", bookId)));
        return Response.noContent().build();
    }

    private static CustomerId me(SecurityContext security) {
        return Callers.customer(security).customerId();
    }
}
