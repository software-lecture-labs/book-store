package com.bookstore.shared.kernel.domain;

import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for objects that have an identity which outlives changes to their state.
 *
 * <p>The identity is a typed id (ADR-0004), for example a {@code BookId}. A new entity has no id
 * until a repository stores it and calls {@link #markPersisted(Identifier, int)}. Two entities are
 * equal when they are of the same class and have the same id; an entity without an id is only
 * equal to itself.
 *
 * <p>The {@link #version() version} is used by repositories for optimistic locking: an update
 * succeeds only if the stored version still matches the version that was loaded.
 *
 * <p>Entities can also record {@link DomainEvent domain events}. The application layer
 * {@link #pullEvents() collects} them after a successful save and publishes them.
 *
 * @param <I> the type of the id
 */
public abstract class Entity<I extends Identifier> {

    private I id;
    private int version;
    private final List<DomainEvent> events = new ArrayList<>();

    /** Creates a new, not yet stored entity. */
    protected Entity() {
    }

    /**
     * Reconstitutes an entity that was loaded from storage.
     *
     * @param id      the stored id
     * @param version the stored version, zero or greater
     */
    protected Entity(I id, int version) {
        markPersisted(id, version);
    }

    /**
     * Tells whether this entity has not been stored yet.
     *
     * @return {@code true} until a repository assigns an id
     */
    public final boolean isNew() {
        return id == null;
    }

    /**
     * Returns the id assigned by the repository.
     *
     * @return the id
     * @throws IllegalStateException if the entity has not been stored yet
     */
    public final I id() {
        if (id == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " has not been stored yet");
        }
        return id;
    }

    /**
     * Returns the version used for optimistic locking.
     *
     * @return the version, zero for a new entity
     */
    public final int version() {
        return version;
    }

    /**
     * Records the id and version after the entity has been stored. Intended for repositories only.
     *
     * @param id      the id; cannot change once assigned
     * @param version the new version, zero or greater
     */
    public final void markPersisted(I id, int version) {
        Objects.requireNonNull(id, "id");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("id is already " + this.id + ", cannot change it to " + id);
        }
        this.id = id;
        this.version = version;
    }

    /**
     * Records that something of business interest happened to this entity.
     *
     * @param event the event
     */
    protected final void raise(DomainEvent event) {
        events.add(Objects.requireNonNull(event, "event"));
    }

    /**
     * Returns the recorded events and forgets them, so each event is published once.
     *
     * @return the events in the order they were raised
     */
    public final List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Entity<?> that = (Entity<?>) other;
        return id != null && id.equals(that.id);
    }

    /**
     * Returns a hash code that does not change when the id is assigned, so an entity can be put in
     * a {@code HashSet} before it is stored.
     */
    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + (id == null ? "new" : id) + "]";
    }
}
