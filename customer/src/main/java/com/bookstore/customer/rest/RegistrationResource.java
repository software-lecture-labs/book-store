package com.bookstore.customer.rest;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.rest.AccountRequests.RegistrationRequest;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** {@code POST /api/v1/customers}: registration, open to everyone (ACC-1, ACC-2). */
@Path("customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class RegistrationResource {

    private final AccountFacade accounts;

    /**
     * Creates the resource.
     *
     * @param accounts the account use cases
     */
    @Inject
    public RegistrationResource(AccountFacade accounts) {
        this.accounts = accounts;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected RegistrationResource() {
        this(null);
    }

    /**
     * Registers a customer.
     *
     * @param registration the form
     * @return 201 with the new profile; its address is {@code /api/v1/me}
     */
    @POST
    public Response register(@Valid @NotNull RegistrationRequest registration) {
        CustomerView created = accounts.register(registration.toCommand());
        return Response.created(URI.create("me")).entity(created).build();
    }
}
