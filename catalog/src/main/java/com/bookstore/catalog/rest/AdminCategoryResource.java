package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.api.CategoryNode;
import com.bookstore.catalog.rest.CatalogAdminRequests.CategoryRequest;
import com.bookstore.catalog.rest.CatalogAdminRequests.NameRequest;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.Params;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
 * Managing categories (ADM-2); for {@code ADMIN} only. The tree itself is the public
 * {@code GET /api/v1/categories}.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code POST /api/v1/admin/categories}</td><td>add a category, at the top or under a parent</td></tr>
 *   <tr><td>{@code PUT|DELETE /api/v1/admin/categories/{id}}</td><td>rename; remove an empty one</td></tr>
 * </table>
 */
@Path("admin/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminCategoryResource {

    private final CatalogAdminFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog management use cases
     */
    @Inject
    public AdminCategoryResource(CatalogAdminFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminCategoryResource() {
        this(null);
    }

    /** @param security the request's security @param category the name and parent @return 201 with the category */
    @POST
    public Response add(@Context SecurityContext security, @Valid @NotNull CategoryRequest category) {
        Callers.administrator(security);
        CategoryNode added = catalog.addCategory(category.name(), category.parentId());
        return Response.created(URI.create("admin/categories/" + added.id())).entity(added).build();
    }

    /** @param security the request's security @param id the category @param category the new name @return it */
    @PUT
    @Path("{id}")
    public CategoryNode rename(@Context SecurityContext security, @PathParam("id") String id,
                               @Valid @NotNull NameRequest category) {
        Callers.administrator(security);
        return catalog.renameCategory(Params.id("id", id), category.name());
    }

    /** @param security the request's security @param id the category @return 204 */
    @DELETE
    @Path("{id}")
    public Response remove(@Context SecurityContext security, @PathParam("id") String id) {
        Callers.administrator(security);
        catalog.removeCategory(Params.id("id", id));
        return Response.noContent().build();
    }
}
