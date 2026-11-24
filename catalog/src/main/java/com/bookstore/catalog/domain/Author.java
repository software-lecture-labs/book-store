package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * A person who wrote one or more books.
 */
public class Author extends Entity<AuthorId> {

    /** Longest allowed name. */
    public static final int MAX_NAME_LENGTH = 200;

    private String name;

    private Author(String name) {
        this.name = validName(name);
    }

    private Author(AuthorId id, int version, String name) {
        super(id, version);
        this.name = validName(name);
    }

    /**
     * Creates a new author.
     *
     * @param name full name, for example {@code "Sabahattin Ali"}
     * @return the author, not yet stored
     */
    public static Author create(String name) {
        return new Author(name);
    }

    /**
     * Reconstitutes an author loaded from storage.
     *
     * @param id      the stored id
     * @param version the stored version
     * @param name    the stored name
     * @return the author
     */
    public static Author reconstitute(AuthorId id, int version, String name) {
        return new Author(id, version, name);
    }

    /**
     * Returns the full name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Changes the name.
     *
     * @param newName the new full name
     */
    public void rename(String newName) {
        this.name = validName(newName);
    }

    private static String validName(String name) {
        return Guard.requireText(name, "name", MAX_NAME_LENGTH);
    }
}
