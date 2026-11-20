package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The transactional outbox of order events (ADR-0022): the events of a stored order are appended
 * in the same transaction as the order, so they are kept exactly when the order is. A relay hands
 * them on later and marks them published.
 */
public interface OrderEventOutbox {

    /**
     * Appends events in the caller's transaction.
     *
     * @param events the events of an order, in the order they happened, as {@code Entity.pullEvents()} gives them
     * @throws IllegalArgumentException for an event that is not an {@link OrderEvent}
     */
    void append(List<? extends DomainEvent> events);

    /**
     * Returns the events not published yet, oldest first. Inside a transaction they stay reserved for
     * the caller until it ends: another relay running at the same time gets the next ones instead.
     *
     * @param max how many at most
     * @return the events with their outbox ids
     */
    List<Pending> pending(int max);

    /**
     * Marks an event as handed on.
     *
     * @param id          the outbox id
     * @param publishedAt when
     */
    void markPublished(long id, Instant publishedAt);

    /**
     * An event waiting in the outbox.
     *
     * @param id    the outbox id
     * @param event the event
     */
    record Pending(long id, OrderEvent event) {

        /** Rejects a missing event. */
        public Pending {
            Objects.requireNonNull(event, "event");
        }
    }
}
