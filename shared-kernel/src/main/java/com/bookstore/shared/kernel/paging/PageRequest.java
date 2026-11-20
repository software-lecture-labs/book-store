package com.bookstore.shared.kernel.paging;

import com.bookstore.shared.kernel.validation.Guard;

/**
 * Which page of a result to return.
 *
 * @param number zero-based page number
 * @param size   number of elements per page, between 1 and {@value #MAX_SIZE}
 */
public record PageRequest(int number, int size) {

    /** Page size used when the caller does not choose one. */
    public static final int DEFAULT_SIZE = 20;

    /** Largest allowed page size. */
    public static final int MAX_SIZE = 100;

    /** Validates the page number and size. */
    public PageRequest {
        Guard.requireNonNegative(number, "page");
        Guard.requireBetween(size, 1, MAX_SIZE, "size");
    }

    /**
     * Returns the given page with the default size.
     *
     * @param number zero-based page number
     * @return the request
     */
    public static PageRequest of(int number) {
        return new PageRequest(number, DEFAULT_SIZE);
    }

    /**
     * Returns the first page with the default size.
     *
     * @return the request
     */
    public static PageRequest first() {
        return of(0);
    }

    /**
     * Returns the index of the first element on this page, as used by SQL {@code OFFSET}.
     *
     * @return the offset
     */
    public long offset() {
        return (long) number * size;
    }

    /**
     * Returns the request for the following page.
     *
     * @return the next page request
     */
    public PageRequest next() {
        return new PageRequest(number + 1, size);
    }
}
