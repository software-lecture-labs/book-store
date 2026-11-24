package com.bookstore.shared.kernel.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.shared.kernel.error.DomainException.Kind;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Kinds and default codes are a public contract: the HTTP layer maps kinds to status codes and
 * clients branch on codes (ADR-0004, ADR-0009). Changing one of these values breaks this test on
 * purpose.
 */
class DomainExceptionKindTest {

    @Test
    void theKindsAreTheFourTheHttpLayerMaps() {
        assertThat(Arrays.stream(Kind.values()).map(Kind::name))
                .containsExactly("NOT_FOUND", "CONFLICT", "RULE_VIOLATION", "FORBIDDEN");
    }

    @Test
    void eachBaseTypeHasItsKind() {
        assertThat(new NotFoundException("Book", 1).kind()).isEqualTo(Kind.NOT_FOUND);
        assertThat(new ConflictException("email.taken", "taken").kind()).isEqualTo(Kind.CONFLICT);
        assertThat(new RuleViolationException("order.empty-cart", "empty").kind()).isEqualTo(Kind.RULE_VIOLATION);
        assertThat(new ValidationException("price", "must be positive").kind()).isEqualTo(Kind.RULE_VIOLATION);
        assertThat(new AccessDeniedException("not yours").kind()).isEqualTo(Kind.FORBIDDEN);
    }

    @Test
    void defaultCodesAreStable() {
        assertThat(new NotFoundException("Book", 1).code()).isEqualTo("not-found");
        assertThat(new ValidationException("price", "must be positive").code()).isEqualTo("validation.failed");
        assertThat(new AccessDeniedException("not yours").code()).isEqualTo("access-denied");
    }

    @Test
    void specificCodesAreKept() {
        assertThat(new ConflictException("email.taken", "taken").code()).isEqualTo("email.taken");
        assertThat(new AccessDeniedException("review.not-eligible", "no").code()).isEqualTo("review.not-eligible");
    }
}
