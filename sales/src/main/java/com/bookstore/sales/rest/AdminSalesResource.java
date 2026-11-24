package com.bookstore.sales.rest;

import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.api.SalesReport;
import com.bookstore.shared.web.Callers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/** Sales reports for administrators. */
@Path("admin/sales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminSalesResource {

    private final OrderAdminFacade orders;

    /**
     * Creates the resource.
     *
     * @param orders the order management use cases
     */
    @Inject
    public AdminSalesResource(OrderAdminFacade orders) {
        this.orders = orders;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminSalesResource() {
        this(null);
    }

    /**
     * Returns the last 30 UTC days of paid, shipped and delivered sales.
     *
     * @param security the request's security
     * @return the report
     */
    @GET
    public SalesReport report(@Context SecurityContext security) {
        Callers.administrator(security);
        return orders.salesReport();
    }
}
