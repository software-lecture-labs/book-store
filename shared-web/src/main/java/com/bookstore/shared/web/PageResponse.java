package com.bookstore.shared.web;

import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;

/**
 * How a page of results is written as JSON.
 *
 * @param content       the items
 * @param number        the page number, starting at zero
 * @param size          the page size that was asked for
 * @param totalElements the number of items on all pages
 * @param totalPages    the number of pages
 * @param <T>           the item type
 */
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) {

    /** Copies the items. */
    public PageResponse {
        content = List.copyOf(content);
    }

    /**
     * Copies a page.
     *
     * @param page the page
     * @param <T>  the item type
     * @return the response body
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.content(), page.number(), page.request().size(), page.totalElements(),
                page.totalPages());
    }

    /**
     * Reads the {@code page} and {@code size} query parameters.
     *
     * @param page the page number as text, may be {@code null}
     * @param size the page size as text, may be {@code null}
     * @return the page request; the first page of the default size if they are missing
     */
    public static PageRequest pageOf(String page, String size) {
        PageRequest first = PageRequest.first();
        return new PageRequest(Params.integer("page", page, first.number()), Params.integer("size", size, first.size()));
    }
}
