package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.sales.application.OrderEventRelay;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.persistence.InMemoryOrderEventOutbox;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** The server's timer for the outbox relay (ADR-0022): events left in the outbox go out on their own. */
class OutboxRelayTimerTest {

    @Test
    void eventsLeftInTheOutboxGoOutOnTheNextRun() throws Exception {
        InMemoryOrderEventOutbox outbox = new InMemoryOrderEventOutbox();
        List<DomainEvent> received = new CopyOnWriteArrayList<>();
        OrderEventRelay relay = new OrderEventRelay(outbox, received::add, new DirectTransactions(), TestClock.CLOCK);
        OutboxRelayTimer timer = new OutboxRelayTimer(relay, Duration.ofMillis(20));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            timer.start(scheduler);
            outbox.append(List.of(new OrderPlaced(OrderNumber.of(Year.of(2026), 1), CustomerId.of(10),
                    Money.of("10.00"), TestClock.CLOCK.instant())));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (received.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            assertThat(received).hasSize(1);
            assertThat(outbox.waitingCount()).isZero();
        } finally {
            timer.stop();
            scheduler.shutdownNow();
        }
    }
}
