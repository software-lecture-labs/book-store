package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.AuthorView;
import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.rest.CatalogAdminRequests.NameRequest;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.Params;

import java.net.URI;
import java.util.List;

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
 * Managing authors (ADM-2); for {@code ADMIN} only.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET|POST /api/v1/admin/authors}</td><td>every author; add one</td></tr>
 *   <tr><td>{@code PUT|DELETE /api/v1/admin/authors/{id}}</td><td>rename; remove one without books</td></tr>
 * </table>
 */
@Path("admin/authors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminAuthorResource {

    private final CatalogAdminFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog management use cases
     */
    @Inject
    public AdminAuthorResource(CatalogAdminFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminAuthorResource() {
        this(null);
    }

    /** @param security the request's security @return every author, by name */
    @GET
    public List<AuthorView> list(@Context SecurityContext security) {
        Callers.administrator(security);
        return catalog.authors();
    }

    /** @param security the request's security @param author the name @return 201 with the author */
    @POST
    public Response add(@Context SecurityContext security, @Valid @NotNull NameRequest author) {
        Callers.administrator(security);
        AuthorView added = catalog.addAuthor(author.name());
        return Response.created(URI.create("admin/authors/" + added.id())).entity(added).build();
    }

    /** @param security the request's security @param id the author @param author the new name @return the author */
    @PUT
    @Path("{id}")
    public AuthorView rename(@Context SecurityContext security, @PathParam("id") String id,
                             @Valid @NotNull NameRequest author) {
        Callers.administrator(security);
        return catalog.renameAuthor(Params.id("id", id), author.name());
    }

    /** @param security the request's security @param id the author @return 204 */
    @DELETE
    @Path("{id}")
    public Response remove(@Context SecurityContext security, @PathParam("id") String id) {
        Callers.administrator(security);
        catalog.removeAuthor(Params.id("id", id));
        return Response.noContent().build();
    }
}
