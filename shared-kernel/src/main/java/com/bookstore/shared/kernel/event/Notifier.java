package com.bookstore.shared.kernel.event;

import com.bookstore.shared.kernel.event.DomainEvent;

/**
 * Tells customers about things that happened to them, such as a placed or shipped order. It is the
 * outbound adapter behind the notification observers (ADR-0007): use cases publish events to the
 * {@link DomainEventPublisher}, and an observer hands the events customers care about to this port.
 *
 * <p>Events are published only after the transaction committed, so customers are never told about
 * something that was rolled back. Implementations must return quickly.
 */
public interface Notifier {

    /**
     * Hands over an event for notification.
     *
     * @param event the event
     */
    void publish(DomainEvent event);
}
