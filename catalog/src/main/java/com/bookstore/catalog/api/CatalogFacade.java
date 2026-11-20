package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * What customers can do with the catalog: browse, search and look at a book (CAT-1 – CAT-7).
 */
public interface CatalogFacade {

    /**
     * Searches the active books (CAT-1, CAT-3, CAT-5, CAT-6).
     *
     * @param query what to look for
     * @param page  which page to return
     * @return one page of matching books
     * @throws NotFoundException   if the query names a category that does not exist
     * @throws ValidationException if the query names an unknown language or sort field
     */
    Page<BookSummary> search(BookSearchQuery query, PageRequest page);

    /**
     * Returns a book's detail page (CAT-2).
     *
     * @param bookId the book's id
     * @return the details
     * @throws NotFoundException if the book does not exist or is no longer sold (CAT-7)
     */
    BookDetails book(BookId bookId);

    /**
     * Returns the category tree for the navigation menu (CAT-4).
     *
     * @return the top-level categories with their subcategories
     */
    List<CategoryNode> categoryTree();

    /**
     * Returns a book's cover image, if one was uploaded (CAT-2).
     *
     * @param bookId the book
     * @return the image; empty if the book has none
     * @throws NotFoundException if there is no such book
     */
    Optional<CoverFile> cover(BookId bookId);
}
