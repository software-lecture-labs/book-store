package com.bookstore.sales.rest;

import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.rest.SalesRequests.AddToCartRequest;
import com.bookstore.sales.rest.SalesRequests.QuantityRequest;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.Params;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * The cart (CRT-1 – CRT-5): a signed-in customer's stored cart, or a visitor's cart in the HTTP
 * session (ADR-0020), which is merged into the stored one when the visitor signs in. Every answer
 * is the whole cart, checked against the catalog.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/cart}</td><td>the cart</td></tr>
 *   <tr><td>{@code DELETE /api/v1/cart}</td><td>empty it</td></tr>
 *   <tr><td>{@code POST /api/v1/cart/items}</td><td>add copies of a book</td></tr>
 *   <tr><td>{@code PUT /api/v1/cart/items/{bookId}}</td><td>set the quantity; zero removes</td></tr>
 *   <tr><td>{@code DELETE /api/v1/cart/items/{bookId}}</td><td>remove a book</td></tr>
 * </table>
 */
@Path("cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CartResource {

    private final CartFacade cart;
    private final GuestCartHolder guestCart;

    /**
     * Creates the resource.
     *
     * @param cart      the cart use cases
     * @param guestCart the visitor's cart
     */
    @Inject
    public CartResource(CartFacade cart, GuestCartHolder guestCart) {
        this.cart = cart;
        this.guestCart = guestCart;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CartResource() {
        this(null, null);
    }

    /** @param security the request's security @return the cart */
    @GET
    public CartResponse view(@Context SecurityContext security) {
        return CartResponse.of(cart.view(mine(security)));
    }

    /** @param security the request's security @return the empty cart */
    @DELETE
    public CartResponse clear(@Context SecurityContext security) {
        return CartResponse.of(cart.clear(mine(security)));
    }

    /** @param security the request's security @param add the book and quantity @return the cart */
    @POST
    @Path("items")
    public CartResponse add(@Context SecurityContext security, @Valid @NotNull AddToCartRequest add) {
        return CartResponse.of(cart.add(mine(security), BookId.of(add.bookId()), add.quantity()));
    }

    /** @param security the request's security @param bookId the book @param change the quantity @return the cart */
    @PUT
    @Path("items/{bookId}")
    public CartResponse changeQuantity(@Context SecurityContext security, @PathParam("bookId") String bookId,
                                       @Valid @NotNull QuantityRequest change) {
        return CartResponse.of(cart.changeQuantity(mine(security), book(bookId), change.quantity()));
    }

    /** @param security the request's security @param bookId the book @return the cart */
    @DELETE
    @Path("items/{bookId}")
    public CartResponse remove(@Context SecurityContext security, @PathParam("bookId") String bookId) {
        return CartResponse.of(cart.remove(mine(security), book(bookId)));
    }

    private CartRef mine(SecurityContext security) {
        return Callers.of(security)
                .<CartRef>map(caller -> CartRef.customer(caller.customerId()))
                .orElseGet(() -> CartRef.guest(guestCart.cart()));
    }

    private static BookId book(String bookId) {
        return BookId.of(Params.id("bookId", bookId));
    }
}
