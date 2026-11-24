package com.bookstore.shared.kernel.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class InProcessEventPublisherTest {

    private final InProcessEventPublisher publisher = new InProcessEventPublisher();
    private final List<String> seen = new ArrayList<>();

    @Test
    void observersGetTheEventsOfTheirTypeInSubscriptionOrder() {
        publisher.subscribe(Placed.class, event -> seen.add("first " + event.number()));
        publisher.subscribe(DomainEvent.class, event -> seen.add("any " + event.getClass().getSimpleName()));
        publisher.subscribe(Shipped.class, event -> seen.add("shipped " + event.number()));

        publisher.publishAll(List.of(new Placed(1, Instant.EPOCH), new Shipped(1, Instant.EPOCH)));

        assertThat(seen).containsExactly("first 1", "any Placed", "any Shipped", "shipped 1");
    }

    @Test
    void aFailingObserverStopsTheDeliveryAndReachesTheCaller() {
        publisher.subscribe(Placed.class, event -> {
            throw new IllegalStateException("mail server down");
        });
        publisher.subscribe(Placed.class, event -> seen.add("never"));

        assertThatThrownBy(() -> publisher.publish(new Placed(1, Instant.EPOCH)))
                .hasMessage("mail server down");
        assertThat(seen).isEmpty();
    }

    @Test
    void withoutObserversNothingHappens() {
        publisher.publish(new Placed(1, Instant.EPOCH));

        assertThat(seen).isEmpty();
    }

    private record Placed(int number, Instant occurredAt) implements DomainEvent {
    }

    private record Shipped(int number, Instant occurredAt) implements DomainEvent {
    }
}
