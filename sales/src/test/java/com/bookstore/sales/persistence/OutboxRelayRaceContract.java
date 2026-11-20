package com.bookstore.sales.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.sales.application.OrderEventRelay;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.JdbcTransactionRunner;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Year;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Two servers relaying from the same outbox at the same time (ADR-0022): each waiting event is
 * handed on exactly once between them, because a relay locks the rows it takes and skips those
 * another one holds.
 */
abstract class OutboxRelayRaceContract {

    private static final int EVENTS = 200;

    /** @return a fresh database with the sales tables */
    protected abstract TestDatabase newDatabase();

    @Test
    void twoServersHandOnEveryEventOnceBetweenThem() throws Exception {
        TestDatabase database = newDatabase();
        new JdbcOrderEventOutbox(database.jdbc()).append(IntStream.rangeClosed(1, EVENTS)
                .mapToObj(n -> new OrderPlaced(OrderNumber.of(Year.of(2026), n), CustomerId.of(1), Money.of("10.00"),
                        TestClock.CLOCK.instant()))
                .toList());
        List<DomainEvent> received = new CopyOnWriteArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> servers = List.of(server(database, received, start), server(database, received, start));

        int published = 0;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> runs = servers.stream().map(pool::submit).toList();
            start.countDown();
            for (Future<Integer> run : runs) {
                published += run.get();
            }
        }

        assertThat(published).isEqualTo(EVENTS);
        assertThat(received).hasSize(EVENTS).doesNotHaveDuplicates();
        assertThat(database.jdbc().queryForLong("SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL"))
                .isZero();
    }

    /** A server of its own: its own transactions, outbox and relay over the shared database. */
    private static Callable<Integer> server(TestDatabase database, List<DomainEvent> received, CountDownLatch start) {
        JdbcTransactionRunner transactions = new JdbcTransactionRunner(database.dataSource());
        JdbcOrderEventOutbox outbox = new JdbcOrderEventOutbox(new JdbcExecutor(transactions));
        OrderEventRelay relay = new OrderEventRelay(outbox, event -> {
            received.add(event);
            Thread.onSpinWait();
        }, transactions, TestClock.CLOCK);
        return () -> {
            start.await();
            return relay.relay();
        };
    }
}
