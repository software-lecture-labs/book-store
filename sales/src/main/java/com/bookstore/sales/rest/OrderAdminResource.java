package com.bookstore.sales.rest;

import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderSearchQuery;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.rest.SalesRequests.StatusChangeRequest;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;
import com.bookstore.shared.web.WebException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Order management for administrators (ORD-6, ORD-7, ADM-5). Every endpoint needs a signed-in
 * {@code ADMIN}: anonymous callers get 401, customers 403.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/admin/orders}</td><td>filter by {@code status}, {@code orderNumber},
 *       {@code from} and {@code to}</td></tr>
 *   <tr><td>{@code GET /api/v1/admin/orders/{number}}</td><td>any order</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/orders/{number}/status}</td><td>move it to another status</td></tr>
 * </table>
 */
@Path("admin/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OrderAdminResource {

    private final OrderAdminFacade orders;

    /**
     * Creates the resource.
     *
     * @param orders the order management use cases
     */
    @Inject
    public OrderAdminResource(OrderAdminFacade orders) {
        this.orders = orders;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderAdminResource() {
        this(null);
    }

    /**
     * Lists orders.
     *
     * @param security the request's security
     * @param status   only orders in this status, for example {@code SHIPPED}
     * @param number   a fragment of the order number
     * @param from     the first order date, inclusive, in {@code YYYY-MM-DD}
     * @param to       the last order date, inclusive, in {@code YYYY-MM-DD}
     * @param page     the page
     * @param size     the size
     * @return the orders, newest first
     */
    @GET
    public PageResponse<OrderSummary> list(@Context SecurityContext security, @QueryParam("status") String status,
                                           @QueryParam("orderNumber") String number, @QueryParam("from") String from,
                                           @QueryParam("to") String to,
                                           @QueryParam("page") String page, @QueryParam("size") String size) {
        Callers.administrator(security);
        PageRequest request = PageResponse.pageOf(page, size);
        OrderSearchQuery query = new OrderSearchQuery(Params.optional(status), Params.optional(number),
                date("from", from), date("to", to));
        return PageResponse.of(orders.search(query, request));
    }

    /** @param security the request's security @param number the order number @return the order */
    @GET
    @Path("{number}")
    public OrderDetails one(@Context SecurityContext security, @PathParam("number") String number) {
        Callers.administrator(security);
        return orders.order(number);
    }

    /** @param security the request's security @param number the order @param change the status @return the order */
    @PUT
    @Path("{number}/status")
    public OrderDetails changeStatus(@Context SecurityContext security, @PathParam("number") String number,
                                     @Valid @NotNull StatusChangeRequest change) {
        Callers.administrator(security);
        return orders.changeStatus(number, change.status());
    }

    private static LocalDate date(String name, String value) {
        String optional = Params.optional(value);
        if (optional == null) {
            return null;
        }
        try {
            return LocalDate.parse(optional);
        } catch (DateTimeParseException e) {
            throw WebException.badRequest("request.bad-parameter", name + " must use YYYY-MM-DD format");
        }
    }
}
