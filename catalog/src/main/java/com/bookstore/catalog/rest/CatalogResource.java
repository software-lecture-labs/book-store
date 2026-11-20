package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.api.CatalogFacade;
import com.bookstore.catalog.api.CoverFile;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.web.PageResponse;
import com.bookstore.shared.web.Params;
import com.bookstore.shared.web.WebException;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * The catalog's books over HTTP (ADR-0009, ADR-0010). Everyone may use them; no sign-in is needed.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET /api/v1/books}</td><td>search; query parameters {@code q}, {@code category},
 *       {@code author}, {@code minPrice}, {@code maxPrice}, {@code language}, {@code inStock},
 *       {@code sort} ({@code title}, {@code price}, {@code publication_year}, {@code rating}),
 *       {@code order} ({@code asc}, {@code desc}), {@code page}, {@code size}</td></tr>
 *   <tr><td>{@code GET /api/v1/books/{id}}</td><td>a book's details</td></tr>
 *   <tr><td>{@code GET /api/v1/books/{id}/cover}</td><td>the cover image, with an entity tag</td></tr>
 * </table>
 */
@Path("books")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CatalogResource {

    private final CatalogFacade catalog;

    /**
     * Creates the resource.
     *
     * @param catalog the catalog's use cases
     */
    @Inject
    public CatalogResource(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CatalogResource() {
        this(null);
    }

    /**
     * Searches the active books.
     *
     * @param text     words to look for
     * @param category a category, with its subcategories
     * @param author   an author
     * @param minPrice the lowest price
     * @param maxPrice the highest price
     * @param language an ISO 639-1 code
     * @param inStock  {@code true} to leave out books with no stock
     * @param sort     what to sort by
     * @param order    {@code asc} or {@code desc}
     * @param page     the page number, from zero
     * @param size     the page size
     * @return one page of books
     */
    @GET
    public PageResponse<BookSummary> search(@QueryParam("q") String text, @QueryParam("category") String category,
                                            @QueryParam("author") String author,
                                            @QueryParam("minPrice") String minPrice,
                                            @QueryParam("maxPrice") String maxPrice,
                                            @QueryParam("language") String language,
                                            @QueryParam("inStock") String inStock, @QueryParam("sort") String sort,
                                            @QueryParam("order") String order, @QueryParam("page") String page,
                                            @QueryParam("size") String size) {
        BookSearchQuery query = BookSearchQuery.everything()
                .withText(Params.optional(text))
                .priced(money("minPrice", minPrice), money("maxPrice", maxPrice))
                .inLanguage(Params.optional(language))
                .sortedBy(sort == null ? BookSearchQuery.DEFAULT_SORT : sort.toUpperCase(Locale.ROOT), sortOrder(order));
        Long categoryId = Params.optionalId("category", category);
        if (categoryId != null) {
            query = query.inCategory(categoryId);
        }
        Long authorId = Params.optionalId("author", author);
        if (authorId != null) {
            query = query.byAuthor(authorId);
        }
        if (Params.flag("inStock", inStock)) {
            query = query.onlyInStock();
        }
        return PageResponse.of(catalog.search(query, PageResponse.pageOf(page, size)));
    }

    /**
     * Returns a book's details.
     *
     * @param id the book's id
     * @return the details
     */
    @GET
    @Path("{id}")
    public BookDetails book(@PathParam("id") String id) {
        return catalog.book(BookId.of(Params.id("id", id)));
    }

    /**
     * Returns a book's cover image (CAT-2). The storage key is the entity tag: browsers check it with
     * {@code If-None-Match} and get a 304 until a new cover is uploaded.
     *
     * @param id      the book's id
     * @param request the request, for its preconditions
     * @return the image, or 304
     */
    @GET
    @Path("{id}/cover")
    @Produces({"image/jpeg", "image/png"})
    public Response cover(@PathParam("id") String id, @Context Request request) {
        BookId bookId = BookId.of(Params.id("id", id));
        CoverFile cover = catalog.cover(bookId).orElseThrow(() -> new NotFoundException("Cover of book", bookId));
        EntityTag tag = new EntityTag(cover.key());
        CacheControl revalidate = new CacheControl();
        revalidate.setNoCache(true);
        Response.ResponseBuilder notModified = request.evaluatePreconditions(tag);
        if (notModified != null) {
            return notModified.cacheControl(revalidate).build();
        }
        return Response.ok(cover.content(), cover.mediaType()).tag(tag).cacheControl(revalidate).build();
    }

    private static Money money(String name, String value) {
        if (Params.optional(value) == null) {
            return null;
        }
        try {
            return Money.of(new BigDecimal(value));
        } catch (NumberFormatException e) {
            throw WebException.badRequest("request.bad-parameter", name + " must be an amount such as 120.50");
        }
    }

    private static SortOrder sortOrder(String order) {
        if (Params.optional(order) == null) {
            return SortOrder.ASC;
        }
        return switch (order.toLowerCase(Locale.ROOT)) {
            case "asc" -> SortOrder.ASC;
            case "desc" -> SortOrder.DESC;
            default -> throw WebException.badRequest("request.bad-parameter", "order must be asc or desc");
        };
    }
}
