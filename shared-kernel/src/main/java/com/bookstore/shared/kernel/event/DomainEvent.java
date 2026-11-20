package com.bookstore.shared.kernel.event;

import java.time.Instant;

/**
 * Something of business interest that has happened.
 *
 * <p>Each business module declares its own {@code sealed} event interface that extends this one
 * and lists its events (for example {@code OrderEvent} in the sales module), so a {@code switch}
 * over one module's events can still be checked for completeness by the compiler (ADR-0007).
 */
public interface DomainEvent {

    /**
     * Returns when the event happened.
     *
     * @return the moment of the event
     */
    Instant occurredAt();
}
