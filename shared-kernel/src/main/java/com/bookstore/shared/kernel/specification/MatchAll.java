package com.bookstore.shared.kernel.specification;

/**
 * Satisfied by every object. Returned by {@link Specification#all()}; an SQL translation turns it
 * into no condition at all.
 *
 * @param <T> the type of object
 */
public record MatchAll<T>() implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return true;
    }
}
