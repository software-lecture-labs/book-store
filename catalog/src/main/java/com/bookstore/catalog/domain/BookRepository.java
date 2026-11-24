package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.specification.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stores and finds books.
 */
public interface BookRepository {

    /**
     * Finds a book by id, active or not.
     *
     * @param id the id
     * @return the book, or empty
     */
    Optional<Book> findById(BookId id);

    /**
     * Finds a book by ISBN, active or not.
     *
     * @param isbn the ISBN
     * @return the book, or empty
     */
    Optional<Book> findByIsbn(Isbn isbn);

    /**
     * Finds several books at once. Ids with no book are skipped.
     *
     * @param ids the ids
     * @return the books found, in no particular order
     */
    List<Book> findAllById(Collection<BookId> ids);

    /**
     * Returns one page of the books that satisfy a rule, in an order.
     *
     * @param specification what to look for, built from {@link BookSpecifications}
     * @param sort          the order of the results
     * @param page          which page to return
     * @return the page
     */
    Page<Book> search(Specification<Book> specification, BookSort sort, PageRequest page);

    /**
     * Tells whether any book, active or not, was written by the author.
     *
     * @param authorId the author's id
     * @return {@code true} if the author has books
     */
    boolean existsByAuthorId(AuthorId authorId);

    /**
     * Tells whether any book, active or not, is in the category.
     *
     * @param categoryId the category's id
     * @return {@code true} if the category has books
     */
    boolean existsByCategoryId(CategoryId categoryId);

    /**
     * Inserts a new book or updates a stored one.
     *
     * @param book the book
     * @return the same book, with its id and version updated
     * @throws com.bookstore.shared.kernel.error.ConflictException if the ISBN is taken or the book was
     *                                                      changed by someone else in the meantime
     */
    Book save(Book book);
}
