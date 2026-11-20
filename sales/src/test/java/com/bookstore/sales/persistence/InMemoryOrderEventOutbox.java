package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.shared.kernel.event.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** Keeps order events in memory, for tests; ids grow like the table's identity column. */
public final class InMemoryOrderEventOutbox implements OrderEventOutbox {

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, OrderEvent> waiting = new ConcurrentSkipListMap<>();
    private final List<OrderEvent> published = new ArrayList<>();

    @Override
    public void append(List<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            if (!(event instanceof OrderEvent orderEvent)) {
                throw new IllegalArgumentException("Not an order event: " + event);
            }
            waiting.put(ids.incrementAndGet(), orderEvent);
        }
    }

    @Override
    public List<Pending> pending(int max) {
        return waiting.entrySet().stream().limit(max).map(entry -> new Pending(entry.getKey(), entry.getValue())).toList();
    }

    @Override
    public synchronized void markPublished(long id, Instant publishedAt) {
        OrderEvent event = waiting.remove(id);
        if (event != null) {
            published.add(event);
        }
    }

    /** @return how many events wait to be published */
    public int waitingCount() {
        return waiting.size();
    }

    /** @return the published events, in the order they were published */
    public synchronized List<OrderEvent> published() {
        return List.copyOf(published);
    }
}
