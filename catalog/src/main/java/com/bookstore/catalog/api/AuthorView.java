package com.bookstore.catalog.api;

/**
 * An author, as the administrator manages them (ADM-2).
 *
 * @param id   the author's id
 * @param name the name
 */
public record AuthorView(long id, String name) {
}
