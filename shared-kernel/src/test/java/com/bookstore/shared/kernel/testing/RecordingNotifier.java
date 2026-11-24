package com.bookstore.shared.kernel.testing;

import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.event.Notifier;

import java.util.ArrayList;
import java.util.List;

/** Remembers every published event. */
public final class RecordingNotifier implements Notifier {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    /**
     * Returns the events published so far.
     *
     * @return the events, in order
     */
    public List<DomainEvent> events() {
        return List.copyOf(events);
    }

    /** Forgets the events published so far. */
    public void clear() {
        events.clear();
    }
}
