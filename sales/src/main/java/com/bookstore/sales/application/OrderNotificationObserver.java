package com.bookstore.sales.application;

import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.shared.kernel.event.Notifier;

import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Tells customers about their orders (ADR-0007): it observes the sales module's {@link OrderEvent}s
 * and hands them to the {@link Notifier} adapter. A new reaction to orders is a new observer like
 * this one; the use cases do not change.
 */
@ApplicationScoped
public class OrderNotificationObserver implements Consumer<OrderEvent> {

    private final Notifier notifier;

    /**
     * Creates the observer.
     *
     * @param notifier the adapter that reaches the customer
     */
    @Inject
    public OrderNotificationObserver(Notifier notifier) {
        this.notifier = notifier;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderNotificationObserver() {
        this(null);
    }

    @Override
    public void accept(OrderEvent event) {
        notifier.publish(event);
    }

    /**
     * Receives order events as CDI events, after the transaction that stored the order committed
     * (ADR-0010). A rolled-back order never reaches the customer.
     *
     * @param event the event
     */
    void onOrderEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderEvent event) {
        accept(event);
    }
}
