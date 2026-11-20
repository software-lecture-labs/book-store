package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.AdminBookRow;
import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.api.CatalogStats;
import com.bookstore.catalog.rest.CatalogAdminRequests.ActiveRequest;
import com.bookstore.catalog.rest.CatalogAdminRequests.BookRequest;
import com.bookstore.catalog.rest.CatalogAdminRequests.PriceRequest;
import com.bookstore.catalog.rest.CatalogAdminRequests.StockRequest;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.web.Callers;
import com.bookstore.shared.web.EntityTags;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;
import com.bookstore.shared.web.WebException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Managing books (ADM-1, ADM-3, ADM-8). Every endpoint is for {@code ADMIN}: anonymous callers get
 * 401, customers 403 (ACC-6).
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/admin/books}</td><td>every book, deactivated ones too; {@code ?q=} filters</td></tr>
 *   <tr><td>{@code POST /api/v1/admin/books}</td><td>add a book</td></tr>
 *   <tr><td>{@code GET /api/v1/admin/books/stats}</td><td>counts and books running low; {@code ?lowStockBelow=}</td></tr>
 *   <tr><td>{@code GET|PUT /api/v1/admin/books/{id}}</td><td>a book; change it</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/books/{id}/price}</td><td>change the price</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/books/{id}/stock}</td><td>set the stock</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/books/{id}/active}</td><td>take off sale or put back</td></tr>
 *   <tr><td>{@code PUT /api/v1/admin/books/{id}/cover}</td><td>upload the cover: the JPEG or PNG itself, at most 2 MB</td></tr>
 * </table>
 */
@Path("admin/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AdminBookResource {

    /** Books on sale with fewer copies than this run low, unless the request says otherwise. */
    static final int DEFAULT_LOW_STOCK = 5;

    /** The most bytes of an oversized upload read and dropped before the answer: 32 MB. */
    static final long DISCARD_LIMIT = 32L * 1024 * 1024;

    private final CatalogAdminFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog management use cases
     */
    @Inject
    public AdminBookResource(CatalogAdminFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AdminBookResource() {
        this(null);
    }

    /**
     * Lists books.
     *
     * @param security the request's security
     * @param text     a fragment of the title, an author or the ISBN
     * @param page     the page number
     * @param size     the page size
     * @return the books
     */
    @GET
    public PageResponse<AdminBookRow> list(@Context SecurityContext security, @QueryParam("q") String text,
                                           @QueryParam("page") String page, @QueryParam("size") String size) {
        Callers.administrator(security);
        return PageResponse.of(catalog.books(Params.optional(text), PageResponse.pageOf(page, size)));
    }

    /**
     * Adds a book.
     *
     * @param security the request's security
     * @param book     the book
     * @return 201 with the book
     */
    @POST
    public Response create(@Context SecurityContext security, @Valid @NotNull BookRequest book) {
        Callers.administrator(security);
        BookDetails created = catalog.createBook(book.toCommand());
        return Response.created(URI.create("admin/books/" + created.id().value())).entity(created)
                .tag(tagOf(created)).build();
    }

    /**
     * Counts the books for the overview.
     *
     * @param security      the request's security
     * @param lowStockBelow the threshold for books running low
     * @return the counts
     */
    @GET
    @Path("stats")
    public CatalogStats stats(@Context SecurityContext security, @QueryParam("lowStockBelow") String lowStockBelow) {
        Callers.administrator(security);
        return catalog.stats(Params.integer("lowStockBelow", lowStockBelow, DEFAULT_LOW_STOCK));
    }

    /**
     * Shows a book, even one off sale.
     *
     * @param security the request's security
     * @param id       the book
     * @return the book, with its version as {@code ETag}
     */
    @GET
    @Path("{id}")
    public Response one(@Context SecurityContext security, @PathParam("id") String id) {
        Callers.administrator(security);
        return answer(catalog.book(book(id)));
    }

    /**
     * Changes a book. Every change answers the new {@code ETag}; one whose {@code If-Match} names an
     * outdated version is refused with 412 (ADR-0022).
     *
     * @param security the request's security
     * @param id       the book
     * @param ifMatch  the version the caller read, or {@code null} for no check
     * @param book     the new values
     * @return the changed book
     */
    @PUT
    @Path("{id}")
    public Response update(@Context SecurityContext security, @PathParam("id") String id,
                           @HeaderParam(EntityTags.IF_MATCH) String ifMatch, @Valid @NotNull BookRequest book) {
        Callers.administrator(security);
        BookId bookId = book(id);
        return answer(catalog.updateBook(bookId, book.toCommand(), expectedVersion(bookId, ifMatch)));
    }

    /**
     * @param security the request's security
     * @param id       the book
     * @param ifMatch  the version the caller read, or {@code null}
     * @param change   the price
     * @return the changed book
     */
    @PUT
    @Path("{id}/price")
    public Response price(@Context SecurityContext security, @PathParam("id") String id,
                          @HeaderParam(EntityTags.IF_MATCH) String ifMatch, @Valid @NotNull PriceRequest change) {
        Callers.administrator(security);
        BookId bookId = book(id);
        return answer(catalog.changePrice(bookId, Money.of(change.price()), expectedVersion(bookId, ifMatch)));
    }

    /**
     * @param security the request's security
     * @param id       the book
     * @param ifMatch  the version the caller read, or {@code null}
     * @param change   the stock
     * @return the changed book
     */
    @PUT
    @Path("{id}/stock")
    public Response stock(@Context SecurityContext security, @PathParam("id") String id,
                          @HeaderParam(EntityTags.IF_MATCH) String ifMatch, @Valid @NotNull StockRequest change) {
        Callers.administrator(security);
        BookId bookId = book(id);
        return answer(catalog.changeStock(bookId, change.stock(), expectedVersion(bookId, ifMatch)));
    }

    /**
     * @param security the request's security
     * @param id       the book
     * @param ifMatch  the version the caller read, or {@code null}
     * @param change   on or off sale
     * @return the changed book
     */
    @PUT
    @Path("{id}/active")
    public Response active(@Context SecurityContext security, @PathParam("id") String id,
                           @HeaderParam(EntityTags.IF_MATCH) String ifMatch, @Valid @NotNull ActiveRequest change) {
        Callers.administrator(security);
        BookId bookId = book(id);
        return answer(catalog.setActive(bookId, change.active(), expectedVersion(bookId, ifMatch)));
    }

    /**
     * Uploads the cover image (ADM-4). The body is the image itself; at most one byte more than the
     * limit is kept, so a huge upload cannot fill the memory; the rest is read and dropped.
     *
     * @param security the request's security
     * @param id       the book
     * @param image    the JPEG or PNG
     * @return the book
     */
    @PUT
    @Path("{id}/cover")
    @Consumes({"image/jpeg", "image/png"})
    public BookDetails cover(@Context SecurityContext security, @PathParam("id") String id, InputStream image) {
        Callers.administrator(security);
        byte[] content;
        try {
            content = image.readNBytes(CatalogAdminFacade.MAX_COVER_BYTES + 1);
            if (content.length > CatalogAdminFacade.MAX_COVER_BYTES) {
                discard(image);
            }
        } catch (IOException e) {
            throw WebException.badRequest("request.unreadable", "The image could not be read");
        }
        return catalog.attachCover(book(id), content);
    }

    /**
     * Reads and drops what is left of a body that is too large, up to {@link #DISCARD_LIMIT} bytes, so the
     * client receives the 422 instead of a reset connection. Nothing is kept in memory.
     */
    private static void discard(InputStream rest) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long dropped = 0;
        int read;
        while (dropped < DISCARD_LIMIT && (read = rest.read(buffer)) != -1) {
            dropped += read;
        }
    }

    /**
     * Checks {@code If-Match} against the stored version. The use case checks the same version again
     * inside its transaction, so a change in between is still caught (409).
     */
    private Integer expectedVersion(BookId bookId, String ifMatch) {
        return ifMatch == null ? null : EntityTags.expectedVersion(ifMatch, catalog.book(bookId).version());
    }

    private static Response answer(BookDetails book) {
        return Response.ok(book).tag(tagOf(book)).build();
    }

    private static EntityTag tagOf(BookDetails book) {
        return new EntityTag(Integer.toString(book.version()));
    }

    private static BookId book(String id) {
        return BookId.of(Params.id("id", id));
    }
}
