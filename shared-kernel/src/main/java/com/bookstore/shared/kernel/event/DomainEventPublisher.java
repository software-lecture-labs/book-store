package com.bookstore.shared.kernel.event;

import java.util.Collection;

/**
 * Hands domain events to whoever reacts to them (ADR-0007). Use cases call it only after the
 * aggregate that raised the events has been stored: first save, then publish.
 *
 * <p>The publisher hides how events travel. In Phase 4 it is the {@link InProcessEventPublisher};
 * in Phase 6 events become CDI events observed after the transaction commits (ADR-0010).
 */
public interface DomainEventPublisher {

    /**
     * Publishes one event.
     *
     * @param event the event
     */
    void publish(DomainEvent event);

    /**
     * Publishes events in order.
     *
     * @param events the events, for example what {@code Entity.pullEvents()} returned
     */
    default void publishAll(Collection<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
