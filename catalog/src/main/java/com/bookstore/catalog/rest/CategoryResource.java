package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.CatalogFacade;
import com.bookstore.catalog.api.CategoryNode;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** {@code GET /api/v1/categories}: the category tree for the navigation menu (CAT-4). */
@Path("categories")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CategoryResource {

    private final CatalogFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog's use cases
     */
    @Inject
    public CategoryResource(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CategoryResource() {
        this(null);
    }

    /** @return the top-level categories with their subcategories */
    @GET
    public List<CategoryNode> tree() {
        return catalog.categoryTree();
    }
}
