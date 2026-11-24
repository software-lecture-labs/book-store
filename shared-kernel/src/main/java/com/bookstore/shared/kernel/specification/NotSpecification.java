package com.bookstore.shared.kernel.specification;

import java.util.Objects;

/**
 * Satisfied exactly when another rule is not.
 *
 * @param negated the rule to negate
 * @param <T>     the type of object the rule is about
 */
public record NotSpecification<T>(Specification<T> negated) implements Specification<T> {

    /** Rejects a missing rule. */
    public NotSpecification {
        Objects.requireNonNull(negated, "negated");
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !negated.isSatisfiedBy(candidate);
    }
}
