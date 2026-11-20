package com.bookstore.sales.application;

import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.shared.kernel.event.DomainEventPublisher;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Hands the events waiting in the {@link OrderEventOutbox} to the {@link DomainEventPublisher}
 * and marks them published, a batch per transaction (ADR-0022). If handing an event on fails, its
 * batch stays in the outbox and the next run tries again: every event reaches the publisher at
 * least once.
 *
 * <p>It runs right after an order is stored and, on the server, at regular intervals, which also
 * catches events left behind when the server stopped between the two. Runs inside one server take
 * turns.
 */
@ApplicationScoped
public class OrderEventRelay {

    /** How many events one transaction hands on at most. */
    static final int BATCH_SIZE = 50;

    private static final System.Logger LOG = System.getLogger(OrderEventRelay.class.getName());

    private final OrderEventOutbox outbox;
    private final DomainEventPublisher publisher;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final ReentrantLock turn = new ReentrantLock();

    /**
     * Creates the relay.
     *
     * @param outbox       where the events wait
     * @param publisher    who receives them
     * @param transactions runs each batch in a transaction
     * @param clock        the clock giving publication times
     */
    @Inject
    public OrderEventRelay(OrderEventOutbox outbox, DomainEventPublisher publisher, TransactionRunner transactions,
                           Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderEventRelay() {
        this(null, null, null, null);
    }

    /**
     * Hands on every waiting event.
     *
     * @return how many events were published
     * @throws RuntimeException what handing an event on threw; that batch stays in the outbox
     */
    public int relay() {
        turn.lock();
        try {
            int published = 0;
            int batch;
            do {
                batch = transactions.inTransaction(this::relayBatch);
                published += batch;
            } while (batch == BATCH_SIZE);
            return published;
        } finally {
            turn.unlock();
        }
    }

    /**
     * Hands on the waiting events, but only logs a failure; for callers whose own work is done
     * already, such as a stored order.
     *
     * @return how many events were published
     */
    public int relayQuietly() {
        try {
            return relay();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Order events stay in the outbox until the next relay run", e);
            return 0;
        }
    }

    private int relayBatch() {
        List<OrderEventOutbox.Pending> batch = outbox.pending(BATCH_SIZE);
        for (OrderEventOutbox.Pending pending : batch) {
            publisher.publish(pending.event());
            outbox.markPublished(pending.id(), clock.instant());
        }
        return batch.size();
    }
}
