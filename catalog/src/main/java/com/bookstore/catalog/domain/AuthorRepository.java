package com.bookstore.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stores and finds authors.
 */
public interface AuthorRepository {

    /**
     * Finds an author by id.
     *
     * @param id the id
     * @return the author, or empty
     */
    Optional<Author> findById(AuthorId id);

    /**
     * Finds several authors at once. Ids with no author are skipped.
     *
     * @param ids the ids
     * @return the authors found, in no particular order
     */
    List<Author> findAllById(Collection<AuthorId> ids);

    /**
     * Returns every author, sorted by name.
     *
     * @return the authors
     */
    List<Author> findAll();

    /**
     * Inserts a new author or updates a stored one.
     *
     * @param author the author
     * @return the same author, with its id and version updated
     */
    Author save(Author author);

    /**
     * Deletes an author. The caller must first check that the author has no books.
     *
     * @param author a stored author
     */
    void delete(Author author);
}
