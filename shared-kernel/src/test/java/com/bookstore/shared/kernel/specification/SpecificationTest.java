package com.bookstore.shared.kernel.specification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpecificationTest {

    private final Specification<Integer> even = n -> n % 2 == 0;
    private final Specification<Integer> positive = n -> n > 0;

    @Test
    void combinesWithAndOrNot() {
        assertThat(even.and(positive).isSatisfiedBy(4)).isTrue();
        assertThat(even.and(positive).isSatisfiedBy(-4)).isFalse();
        assertThat(even.or(positive).isSatisfiedBy(3)).isTrue();
        assertThat(even.or(positive).isSatisfiedBy(-3)).isFalse();
        assertThat(even.not().isSatisfiedBy(3)).isTrue();
    }

    @Test
    void combinationsAreDataThatCanBeTranslated() {
        Specification<Integer> rule = even.and(positive.not());

        assertThat(rule).isEqualTo(new AndSpecification<>(even, new NotSpecification<>(positive)));
    }

    @Test
    void allIsTheNeutralElementOfAnd() {
        Specification<Integer> all = Specification.all();

        assertThat(all.isSatisfiedBy(-7)).isTrue();
        assertThat(all.and(even)).isSameAs(even);
        assertThat(even.and(all)).isSameAs(even);
    }
}
