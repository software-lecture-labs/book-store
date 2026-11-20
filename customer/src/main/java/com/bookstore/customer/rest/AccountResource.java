package com.bookstore.customer.rest;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.rest.AccountRequests.AddressRequest;
import com.bookstore.customer.rest.AccountRequests.PasswordChangeRequest;
import com.bookstore.customer.rest.AccountRequests.ProfileRequest;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.Params;

import java.net.URI;
import java.util.Comparator;

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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * The signed-in customer's account (ACC-3 – ACC-5). The customer is never named in the path, so
 * nobody can reach another customer's account.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET|PUT /api/v1/me}</td><td>the profile</td></tr>
 *   <tr><td>{@code PUT /api/v1/me/password}</td><td>change the password</td></tr>
 *   <tr><td>{@code POST /api/v1/me/addresses}</td><td>save an address</td></tr>
 *   <tr><td>{@code GET|PUT|DELETE /api/v1/me/addresses/{id}}</td><td>one saved address</td></tr>
 *   <tr><td>{@code POST /api/v1/me/addresses/{id}/default}</td><td>make it the default</td></tr>
 * </table>
 */
@Path("me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AccountResource {

    private final AccountFacade accounts;

    /**
     * Creates the resource.
     *
     * @param accounts the account use cases
     */
    @Inject
    public AccountResource(AccountFacade accounts) {
        this.accounts = accounts;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AccountResource() {
        this(null);
    }

    /** @param security the request's security @return the profile */
    @GET
    public CustomerView profile(@Context SecurityContext security) {
        return accounts.profile(me(security));
    }

    /** @param security the request's security @param profile the new name and address @return the profile */
    @PUT
    public CustomerView updateProfile(@Context SecurityContext security, @Valid @NotNull ProfileRequest profile) {
        return accounts.updateProfile(me(security), profile.firstName(), profile.lastName(), profile.email());
    }

    /** @param security the request's security @param change the current and the new password */
    @PUT
    @Path("password")
    public void changePassword(@Context SecurityContext security, @Valid @NotNull PasswordChangeRequest change) {
        accounts.changePassword(me(security), change.currentPassword(), change.newPassword());
    }

    /** @param security the request's security @param address the address @return 201 with the profile */
    @POST
    @Path("addresses")
    public Response addAddress(@Context SecurityContext security, @Valid @NotNull AddressRequest address) {
        CustomerView profile = accounts.addAddress(me(security), address.label(), address.toAddress(),
                address.makeDefault());
        long newest = profile.addresses().stream()
                .max(Comparator.comparingLong(CustomerView.AddressView::id))
                .orElseThrow()
                .id();
        return Response.created(URI.create("me/addresses/" + newest)).entity(profile).build();
    }

    /** @param security the request's security @param id the address @return the address */
    @GET
    @Path("addresses/{id}")
    public CustomerView.AddressView address(@Context SecurityContext security, @PathParam("id") String id) {
        return accounts.address(me(security), Params.id("id", id));
    }

    /** @param security the request's security @param id the address @param address the new address @return the profile */
    @PUT
    @Path("addresses/{id}")
    public CustomerView changeAddress(@Context SecurityContext security, @PathParam("id") String id,
                                      @Valid @NotNull AddressRequest address) {
        return accounts.changeAddress(me(security), Params.id("id", id), address.label(), address.toAddress());
    }

    /** @param security the request's security @param id the address */
    @DELETE
    @Path("addresses/{id}")
    public void removeAddress(@Context SecurityContext security, @PathParam("id") String id) {
        accounts.removeAddress(me(security), Params.id("id", id));
    }

    /** @param security the request's security @param id the address @return the profile */
    @POST
    @Path("addresses/{id}/default")
    public CustomerView makeDefault(@Context SecurityContext security, @PathParam("id") String id) {
        return accounts.makeDefaultAddress(me(security), Params.id("id", id));
    }

    private static CustomerId me(SecurityContext security) {
        return Callers.customer(security).customerId();
    }
}
