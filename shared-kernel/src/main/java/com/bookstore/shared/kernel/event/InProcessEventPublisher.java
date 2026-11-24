package com.bookstore.shared.kernel.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Delivers events to observers in the same process, on the caller's thread, in the order the
 * observers subscribed (ADR-0007). An observer receives the events of the type it subscribed to,
 * including subtypes.
 *
 * <p>Delivery is synchronous: an observer that throws stops the delivery and the exception
 * reaches the use case, after the aggregate was already stored. This is the dual-write problem;
 * the Transactional Outbox of Phase 9 solves it.
 */
public final class InProcessEventPublisher implements DomainEventPublisher {

    private final List<Subscription<?>> subscriptions = new CopyOnWriteArrayList<>();

    /**
     * Adds an observer.
     *
     * @param type     the events it wants, for example {@code OrderEvent.class}
     * @param observer what to do with each event
     * @param <E>      the event type
     */
    public <E extends DomainEvent> void subscribe(Class<E> type, Consumer<? super E> observer) {
        subscriptions.add(new Subscription<>(Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(observer, "observer")));
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event");
        for (Subscription<?> subscription : subscriptions) {
            subscription.deliver(event);
        }
    }

    private record Subscription<E extends DomainEvent>(Class<E> type, Consumer<? super E> observer) {

        void deliver(DomainEvent event) {
            if (type.isInstance(event)) {
                observer.accept(type.cast(event));
            }
        }
    }
}
