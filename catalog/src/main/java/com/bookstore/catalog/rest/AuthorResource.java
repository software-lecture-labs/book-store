package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.AuthorSummary;
import com.bookstore.catalog.api.CatalogFacade;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** {@code GET /api/v1/authors}: authors available as catalog search filters. */
@Path("authors")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AuthorResource {

    private final CatalogFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog's use cases
     */
    @Inject
    public AuthorResource(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AuthorResource() {
        this(null);
    }

    /** @return authors sorted by name */
    @GET
    public List<AuthorSummary> list() {
        return catalog.authors();
    }
}
