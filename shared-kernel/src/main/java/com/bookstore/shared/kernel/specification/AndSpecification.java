package com.bookstore.shared.kernel.specification;

import java.util.Objects;

/**
 * Satisfied when both of two rules are satisfied.
 *
 * @param left  the first rule
 * @param right the second rule
 * @param <T>   the type of object the rules are about
 */
public record AndSpecification<T>(Specification<T> left, Specification<T> right) implements Specification<T> {

    /** Rejects missing rules. */
    public AndSpecification {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate);
    }
}
