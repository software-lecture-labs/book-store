package com.bookstore.sales.rest;

import com.bookstore.sales.api.CartChangedException;
import com.bookstore.sales.api.CheckoutFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderFacade;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.rest.SalesRequests.PlaceOrderRequest;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.WebException;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Placing orders and the customer's order history (ORD-1 – ORD-5, ORD-8, ORD-9). The customer
 * comes from the sign-in, so nobody can order for or read the orders of someone else.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code POST /api/v1/orders}</td><td>order everything in the cart</td></tr>
 *   <tr><td>{@code GET /api/v1/orders}</td><td>the customer's orders, newest first</td></tr>
 *   <tr><td>{@code GET /api/v1/orders/{number}}</td><td>one of them</td></tr>
 *   <tr><td>{@code POST /api/v1/orders/{number}/cancel}</td><td>cancel one of the customer's orders</td></tr>
 * </table>
 *
 * <p>If the cart had to be corrected at checkout, the answer is a 409 problem with the code
 * {@code cart.changed} and the corrections as its {@code adjustments} member; nothing was ordered.
 */
@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OrderResource {

    /** The header that makes placing an order safe to repeat. */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final Pattern WELL_FORMED_KEY = Pattern.compile("[!-~]{1,100}");

    private final CheckoutFacade checkout;
    private final OrderFacade orders;

    /**
     * Creates the resource.
     *
     * @param checkout placing orders
     * @param orders   the customer's orders
     */
    @Inject
    public OrderResource(CheckoutFacade checkout, OrderFacade orders) {
        this.checkout = checkout;
        this.orders = orders;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderResource() {
        this(null, null);
    }

    /**
     * Places an order. With an {@code Idempotency-Key}, sending the same request again answers the
     * same order instead of placing a second one (ADR-0022).
     *
     * @param security       the request's security
     * @param idempotencyKey the client's key for this order, or {@code null}
     * @param form           the address and payment method
     * @return 201 with the order
     */
    @POST
    public Response place(@Context SecurityContext security, @HeaderParam(IDEMPOTENCY_KEY) String idempotencyKey,
                          @Valid @NotNull PlaceOrderRequest form) {
        if (idempotencyKey != null && !WELL_FORMED_KEY.matcher(idempotencyKey).matches()) {
            throw WebException.badRequest("request.bad-parameter",
                    IDEMPOTENCY_KEY + " must be 1 to 100 visible ASCII characters");
        }
        try {
            OrderDetails order = checkout.placeOrder(
                    new PlaceOrderCommand(me(security), form.addressId(), form.paymentMethod(), idempotencyKey));
            return Response.created(URI.create("orders/" + order.orderNumber())).entity(order).build();
        } catch (CartChangedException changed) {
            throw WebException.conflict(changed.code(), changed.getMessage(),
                    Map.of("adjustments", CartResponse.notices(changed.adjustments())));
        }
    }

    /** @param security the request's security @param page the page @param size the size @return the orders */
    @GET
    public PageResponse<OrderSummary> mine(@Context SecurityContext security, @QueryParam("page") String page,
                                           @QueryParam("size") String size) {
        return PageResponse.of(orders.myOrders(me(security), PageResponse.pageOf(page, size)));
    }

    /** @param security the request's security @param number the order number @return the order */
    @GET
    @Path("{number}")
    public OrderDetails one(@Context SecurityContext security, @PathParam("number") String number) {
        return orders.myOrder(me(security), number);
    }

    /** Cancels a customer's cancellable order and returns its stock. */
    @POST
    @Path("{number}/cancel")
    public OrderDetails cancel(@Context SecurityContext security, @PathParam("number") String number) {
        return orders.cancel(me(security), number);
    }

    private static CustomerId me(SecurityContext security) {
        return Callers.customer(security).customerId();
    }
}
