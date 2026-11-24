package com.bookstore.shared.kernel.error;

/**
 * Signals that a request breaks a business rule that is not about a single field, for example
 * placing an order with an empty cart. Its kind is {@link Kind#RULE_VIOLATION}.
 *
 * <p>A broken rule about one input field is a {@link ValidationException} instead, so that a form
 * can show it next to that field.
 */
public class RuleViolationException extends DomainException {

    /**
     * Creates an exception with a specific error code.
     *
     * @param code    stable machine-readable code, for example {@code "order.empty-cart"}
     * @param message human-readable description for logs
     */
    public RuleViolationException(String code, String message) {
        super(Kind.RULE_VIOLATION, code, message);
    }
}
