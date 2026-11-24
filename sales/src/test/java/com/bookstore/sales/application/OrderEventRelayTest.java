package com.bookstore.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.persistence.InMemoryOrderEventOutbox;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/** The relay of the transactional outbox (ADR-0022). */
class OrderEventRelayTest {

    private final InMemoryOrderEventOutbox outbox = new InMemoryOrderEventOutbox();
    private final List<DomainEvent> received = new ArrayList<>();
    private boolean failing;
    private final OrderEventRelay relay = new OrderEventRelay(outbox, event -> {
        if (failing) {
            throw new IllegalStateException("Mail server unreachable");
        }
        received.add(event);
    }, new DirectTransactions(), TestClock.CLOCK);

    private static OrderPlaced placed(int n) {
        return new OrderPlaced(OrderNumber.of(Year.of(2026), n), CustomerId.of(10), Money.of("10.00"),
                TestClock.CLOCK.instant());
    }

    @Test
    void handsOnEveryWaitingEventOnceInOrder() {
        outbox.append(List.of(placed(1), placed(2)));

        assertThat(relay.relay()).isEqualTo(2);
        assertThat(relay.relay()).isZero();
        assertThat(received).containsExactly(placed(1), placed(2));
        assertThat(outbox.waitingCount()).isZero();
    }

    @Test
    void moreEventsThanABatchAreAllHandedOn() {
        outbox.append(IntStream.rangeClosed(1, OrderEventRelay.BATCH_SIZE * 2 + 3).mapToObj(n -> placed(n)).toList());

        assertThat(relay.relay()).isEqualTo(OrderEventRelay.BATCH_SIZE * 2 + 3);
        assertThat(outbox.waitingCount()).isZero();
    }

    @Test
    void anEventThatCouldNotBeHandedOnWaitsForTheNextRun() {
        outbox.append(List.of(placed(1)));
        failing = true;

        assertThatThrownBy(relay::relay).hasMessage("Mail server unreachable");
        assertThat(relay.relayQuietly()).isZero();
        assertThat(outbox.waitingCount()).isEqualTo(1);

        failing = false;
        assertThat(relay.relay()).isEqualTo(1);
        assertThat(received).containsExactly(placed(1));
    }
}
