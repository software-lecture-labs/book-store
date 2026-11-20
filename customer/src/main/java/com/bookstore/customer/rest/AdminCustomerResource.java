package com.bookstore.customer.rest;

import com.bookstore.customer.api.CustomerAdminFacade;
import com.bookstore.customer.api.CustomerSummary;
import com.bookstore.customer.rest.AccountRequests.ActiveRequest;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;

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
 * Managing user accounts (ADM-6); for {@code ADMIN} only (ACC-6).
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/admin/customers}</td><td>every account, newest first</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/customers/{id}/active}</td><td>let an account sign in or stop it</td></tr>
 * </table>
 */
@Path("admin/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminCustomerResource {

    private final CustomerAdminFacade customers;

    /**
     * Creates the resource.
     *
     * @param customers the account management use cases
     */
    @Inject
    public AdminCustomerResource(CustomerAdminFacade customers) {
        this.customers = customers;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminCustomerResource() {
        this(null);
    }

    /** @param security the request's security @param page the page @param size its size @return the accounts */
    @GET
    public PageResponse<CustomerSummary> list(@Context SecurityContext security, @QueryParam("page") String page,
                                              @QueryParam("size") String size) {
        Callers.administrator(security);
        return PageResponse.of(customers.customers(PageResponse.pageOf(page, size)));
    }

    /** @param security the request's security @param id the account @param change the state @return the account */
    @PUT
    @Path("{id}/active")
    public CustomerSummary active(@Context SecurityContext security, @PathParam("id") String id,
                                  @Valid @NotNull ActiveRequest change) {
        Callers.administrator(security);
        return customers.setActive(CustomerId.of(Params.id("id", id)), change.active());
    }
}
