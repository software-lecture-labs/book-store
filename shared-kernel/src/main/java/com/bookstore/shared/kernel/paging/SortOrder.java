package com.bookstore.shared.kernel.paging;

import java.util.Comparator;

/**
 * Direction of a sort.
 */
public enum SortOrder {

    /** Smallest first. */
    ASC,

    /** Largest first. */
    DESC;

    /**
     * Applies this direction to an ascending comparator.
     *
     * @param ascending a comparator that sorts smallest first
     * @param <T>       the type being compared
     * @return {@code ascending} itself for {@link #ASC}, its reverse for {@link #DESC}
     */
    public <T> Comparator<T> apply(Comparator<T> ascending) {
        return this == ASC ? ascending : ascending.reversed();
    }
}
