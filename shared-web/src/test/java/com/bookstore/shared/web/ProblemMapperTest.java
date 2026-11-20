package com.bookstore.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.error.ValidationException;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The kind-to-status mapping of ADR-0009 and the RFC 9457 body. */
class ProblemMapperTest {

    private final ProblemMapper mapper = new ProblemMapper();

    @Test
    void theStatusComesFromTheKind() {
        assertThat(mapper.toProblem(new NotFoundException("Book", 7)).status()).isEqualTo(404);
        assertThat(mapper.toProblem(new ConflictException("email.taken", "taken")).status()).isEqualTo(409);
        assertThat(mapper.toProblem(new RuleViolationException("order.empty-cart", "empty")).status()).isEqualTo(422);
        assertThat(mapper.toProblem(new AccessDeniedException("not yours")).status()).isEqualTo(403);
    }

    @Test
    void theBodyFollowsRfc9457() {
        Problem problem = mapper.toProblem(new ConflictException("email.taken", "a@b.co is already registered"));

        assertThat(problem.type()).isEqualTo("/problems/email.taken");
        assertThat(problem.title()).isEqualTo("Conflict");
        assertThat(problem.detail()).isEqualTo("a@b.co is already registered");
        assertThat(problem.code()).isEqualTo("email.taken");
        assertThat(problem.errors()).isEmpty();
    }

    @Test
    void domainValidationErrorsAre422WithTheirFields() {
        Problem problem = mapper.toProblem(new ValidationException("price", "must be greater than zero"));

        assertThat(problem.status()).isEqualTo(422);
        assertThat(problem.errors()).containsExactly(new Problem.FieldError("price", "must be greater than zero"));
    }

    @Test
    void formatErrorsAre400() {
        Problem problem = mapper.toProblem(WebException.invalid(List.of(new Problem.FieldError("quantity", "bad"))));

        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.code()).isEqualTo("request.invalid");
    }

    @Test
    void unexpectedFailuresAreOpaque() {
        Problem problem = mapper.toProblem(new IllegalStateException("SELECT * FROM users failed: password=secret"));

        assertThat(problem.status()).isEqualTo(500);
        assertThat(problem.code()).isEqualTo(ProblemMapper.INTERNAL_ERROR);
        assertThat(problem.detail()).doesNotContain("SELECT", "password", "secret");
    }
}
