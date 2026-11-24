package com.bookstore.shared.kernel.paging;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One page of a larger, ordered result.
 *
 * @param content       the elements on this page
 * @param request       the page that was requested
 * @param totalElements number of elements across all pages
 * @param <T>           element type
 */
public record Page<T>(List<T> content, PageRequest request, long totalElements) {

    /** Copies the content and validates the totals. */
    public Page {
        Objects.requireNonNull(request, "request");
        content = List.copyOf(content);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        if (content.size() > request.size()) {
            throw new IllegalArgumentException("content is larger than the page size");
        }
    }

    /**
     * Returns an empty page.
     *
     * @param request the page that was requested
     * @param <T>     element type
     * @return a page with no elements and a total of zero
     */
    public static <T> Page<T> empty(PageRequest request) {
        return new Page<>(List.of(), request, 0);
    }

    /**
     * Cuts one page out of a complete, already sorted list. Used by in-memory repositories.
     *
     * @param all     every matching element, in order
     * @param request the page to return
     * @param <T>     element type
     * @return the requested page
     */
    public static <T> Page<T> fromList(List<? extends T> all, PageRequest request) {
        Objects.requireNonNull(all, "all");
        int from = (int) Math.min(request.offset(), all.size());
        int to = Math.min(from + request.size(), all.size());
        return new Page<>(List.copyOf(all.subList(from, to)), request, all.size());
    }

    /**
     * Converts every element, keeping the paging information.
     *
     * @param mapper conversion applied to each element
     * @param <R>    new element type
     * @return a page of converted elements
     */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, request, totalElements);
    }

    /**
     * Returns the zero-based page number.
     *
     * @return the page number
     */
    public int number() {
        return request.number();
    }

    /**
     * Returns the number of pages needed for all elements. An empty result has zero pages.
     *
     * @return the page count
     */
    public int totalPages() {
        return (int) ((totalElements + request.size() - 1) / request.size());
    }

    /**
     * Tells whether a page follows this one.
     *
     * @return {@code true} if there are more elements after this page
     */
    public boolean hasNext() {
        return number() + 1 < totalPages();
    }

    /**
     * Tells whether a page precedes this one.
     *
     * @return {@code true} if this is not the first page
     */
    public boolean hasPrevious() {
        return number() > 0;
    }

    /**
     * Tells whether this page has no elements.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }
}
