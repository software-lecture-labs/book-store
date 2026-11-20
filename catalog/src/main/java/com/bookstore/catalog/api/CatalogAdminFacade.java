package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;

/**
 * Managing the catalog (ADM-1 – ADM-3, ADM-8). Unlike {@link CatalogFacade} it sees deactivated
 * books. Callers check that only administrators reach it (ACC-6).
 */
public interface CatalogAdminFacade {

    /** The largest cover image accepted, in bytes (ADM-4): 2 MB. */
    int MAX_COVER_BYTES = 2 * 1024 * 1024;

    /**
     * Lists every book, deactivated ones too, by title.
     *
     * @param text a fragment of the title, an author's name or the ISBN; {@code null} for all books
     * @param page which page
     * @return the books
     */
    Page<AdminBookRow> books(String text, PageRequest page);

    /**
     * Shows a book, even a deactivated one.
     *
     * @param bookId the book
     * @return its details
     * @throws NotFoundException if there is no such book
     */
    BookDetails book(BookId bookId);

    /**
     * Adds a book; it is on sale at once.
     *
     * @param command the book
     * @return the stored book
     * @throws ValidationException if a value breaks a rule
     * @throws NotFoundException   if an author or the category does not exist
     * @throws ConflictException   {@code book.isbn-taken} if another book has the ISBN
     */
    BookDetails createBook(BookCommand command);

    /**
     * Changes a book's details, authors, category, price and stock. The ISBN never changes.
     *
     * @param bookId          the book
     * @param command         the new values; its ISBN is ignored
     * @param expectedVersion the version the caller read; {@code null} for no check
     * @return the changed book
     * @throws ConflictException {@link ConflictException#CONCURRENT_MODIFICATION} if the book has another
     *                           version by now (ADR-0022)
     */
    BookDetails updateBook(BookId bookId, BookCommand command, Integer expectedVersion);

    /**
     * Changes a book's details without checking which version the caller read.
     *
     * @param bookId  the book
     * @param command the new values; its ISBN is ignored
     * @return the changed book
     */
    default BookDetails updateBook(BookId bookId, BookCommand command) {
        return updateBook(bookId, command, null);
    }

    /**
     * Changes a book's price (ADM-3).
     *
     * @param bookId          the book
     * @param price           the new price
     * @param expectedVersion the version the caller read; {@code null} for no check
     * @return the changed book
     */
    BookDetails changePrice(BookId bookId, Money price, Integer expectedVersion);

    /**
     * Changes a book's price without checking which version the caller read.
     *
     * @param bookId the book
     * @param price  the new price
     * @return the changed book
     */
    default BookDetails changePrice(BookId bookId, Money price) {
        return changePrice(bookId, price, null);
    }

    /**
     * Sets the number of copies in stock (ADM-3).
     *
     * @param bookId          the book
     * @param quantity        the copies, zero or more
     * @param expectedVersion the version the caller read; {@code null} for no check
     * @return the changed book
     */
    BookDetails changeStock(BookId bookId, int quantity, Integer expectedVersion);

    /**
     * Sets the stock without checking which version the caller read.
     *
     * @param bookId   the book
     * @param quantity the copies, zero or more
     * @return the changed book
     */
    default BookDetails changeStock(BookId bookId, int quantity) {
        return changeStock(bookId, quantity, null);
    }

    /**
     * Takes a book off sale or puts it back (ADM-1, CAT-7). Orders keep showing it either way.
     *
     * @param bookId          the book
     * @param active          whether it is sold
     * @param expectedVersion the version the caller read; {@code null} for no check
     * @return the changed book
     */
    BookDetails setActive(BookId bookId, boolean active, Integer expectedVersion);

    /**
     * Takes a book off sale or puts it back without checking which version the caller read.
     *
     * @param bookId the book
     * @param active whether it is sold
     * @return the changed book
     */
    default BookDetails setActive(BookId bookId, boolean active) {
        return setActive(bookId, active, null);
    }

    /**
     * Uploads a book's cover image (ADM-4); the previous one is deleted.
     *
     * @param bookId  the book
     * @param content the image, a JPEG or PNG of at most {@link #MAX_COVER_BYTES} bytes
     * @return the changed book
     * @throws ValidationException if the file is not such an image
     */
    BookDetails attachCover(BookId bookId, byte[] content);

    /** @return every author, by name */
    List<AuthorView> authors();

    /**
     * Adds an author.
     *
     * @param name the name
     * @return the stored author
     */
    AuthorView addAuthor(String name);

    /**
     * Renames an author.
     *
     * @param authorId the author
     * @param name     the new name
     * @return the changed author
     */
    AuthorView renameAuthor(long authorId, String name);

    /**
     * Removes an author who has no books (ADM-2).
     *
     * @param authorId the author
     * @throws ConflictException {@code author.in-use} if a book names the author
     */
    void removeAuthor(long authorId);

    /**
     * Adds a category.
     *
     * @param name     the name
     * @param parentId the parent category; {@code null} for a top-level one
     * @return the new category
     */
    CategoryNode addCategory(String name, Long parentId);

    /**
     * Renames a category.
     *
     * @param categoryId the category
     * @param name       the new name
     * @return the changed category, with its subcategories
     */
    CategoryNode renameCategory(long categoryId, String name);

    /**
     * Removes a category without books and subcategories (ADM-2).
     *
     * @param categoryId the category
     * @throws ConflictException {@code category.in-use} if a book is in it, {@code category.has-children} if
     *                           it has subcategories
     */
    void removeCategory(long categoryId);

    /**
     * Counts the books for the overview (ADM-8).
     *
     * @param lowStockBelow books on sale with fewer copies than this are listed as running low
     * @return the counts and the books running low, fewest copies first
     */
    CatalogStats stats(int lowStockBelow);
}
