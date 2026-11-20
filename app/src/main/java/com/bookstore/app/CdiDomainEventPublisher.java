package com.bookstore.app;

import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.event.DomainEventPublisher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * The {@link DomainEventPublisher} port on the server: each domain event becomes a CDI event
 * (ADR-0010). Observers declared with {@code @Observes(during = TransactionPhase.AFTER_SUCCESS)},
 * such as the order notification observer, run only once the transaction has committed.
 */
@ApplicationScoped
public class CdiDomainEventPublisher implements DomainEventPublisher {

    private final Event<DomainEvent> events;

    /**
     * Creates the publisher.
     *
     * @param events the container's event bus
     */
    @Inject
    public CdiDomainEventPublisher(Event<DomainEvent> events) {
        this.events = events;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CdiDomainEventPublisher() {
        this(null);
    }

    @Override
    public void publish(DomainEvent event) {
        events.fire(event);
    }
}
