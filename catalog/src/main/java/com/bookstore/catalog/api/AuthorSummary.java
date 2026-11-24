package com.bookstore.catalog.api;

/**
 * An author offered as a catalog search filter.
 *
 * @param id   the author's id
 * @param name the author's name
 */
public record AuthorSummary(long id, String name) {
}
