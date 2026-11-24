package com.bookstore.shared.kernel.specification;

/**
 * A business rule about one kind of object, turned into data (Specification pattern). Rules can be
 * combined with {@link #and}, {@link #or} and {@link #not}, tested against a single object with
 * {@link #isSatisfiedBy}, and, because the combinations are plain records, translated into another
 * language such as SQL by walking the tree.
 *
 * @param <T> the type of object the rule is about
 */
public interface Specification<T> {

    /**
     * Tells whether an object satisfies the rule.
     *
     * @param candidate the object to test
     * @return {@code true} if it does
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * Returns a rule that every object satisfies. It is the neutral element of {@link #and}.
     *
     * @param <T> the type of object
     * @return the rule
     */
    static <T> Specification<T> all() {
        return new MatchAll<>();
    }

    /**
     * Returns a rule satisfied when both this rule and another are.
     *
     * @param other the other rule
     * @return the combined rule
     */
    default Specification<T> and(Specification<T> other) {
        if (other instanceof MatchAll<T>) {
            return this;
        }
        if (this instanceof MatchAll<T>) {
            return other;
        }
        return new AndSpecification<>(this, other);
    }

    /**
     * Returns a rule satisfied when this rule, another, or both are.
     *
     * @param other the other rule
     * @return the combined rule
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * Returns a rule satisfied exactly when this one is not.
     *
     * @return the negated rule
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
