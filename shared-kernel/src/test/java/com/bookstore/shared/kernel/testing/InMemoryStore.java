package com.bookstore.shared.kernel.testing;

import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/**
 * Shared storage for the in-memory repositories, behaving like the JDBC ones (ADR-0008):
 *
 * <ul>
 *   <li>ids are assigned like an identity column, and every save bumps the version;</li>
 *   <li>it stores a <em>copy</em> of the entity and hands out a new copy on every read, so changing
 *       a loaded object without saving it changes nothing, just as with a database;</li>
 *   <li>saving a copy whose version is older than the stored one fails with the same conflict the
 *       JDBC repositories raise (optimistic locking).</li>
 * </ul>
 *
 * <p>Subclasses say how to copy their aggregate, usually through its {@code reconstitute} factory.
 *
 * @param <I> the id type
 * @param <T> the entity type
 */
public abstract class InMemoryStore<I extends Identifier, T extends Entity<I>> {

    /** The error code the JDBC repositories use for a stale update. */
    public static final String CONCURRENT_MODIFICATION = ConflictException.CONCURRENT_MODIFICATION;

    private final Map<I, T> byId = new ConcurrentHashMap<>();
    private final Map<I, Long> insertionOrder = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final LongFunction<I> newId;

    /**
     * Creates an empty store.
     *
     * @param newId turns the next sequence number into an id, for example {@code BookId::new}
     */
    protected InMemoryStore(LongFunction<I> newId) {
        this.newId = Objects.requireNonNull(newId, "newId");
    }

    /**
     * Returns an independent copy of an entity, with the same id and version.
     *
     * @param entity a stored entity
     * @return the copy
     */
    protected abstract T copyOf(T entity);

    /** Returns a copy of the entity with an id, if stored. */
    public Optional<T> findById(I id) {
        return Optional.ofNullable(byId.get(id)).map(this::copyOf);
    }

    /** Returns copies of the stored entities among some ids. */
    public List<T> findAllById(Collection<I> ids) {
        return ids.stream().distinct().map(byId::get).filter(Objects::nonNull).map(this::copyOf).toList();
    }

    /**
     * Inserts or updates an entity: assigns an id on insert and a new version every time, then
     * stores a copy.
     *
     * @throws ConflictException if the entity is a stale copy of a stored one
     */
    public synchronized T save(T entity) {
        beforeSave(entity);
        if (entity.isNew()) {
            I id = newId.apply(sequence.incrementAndGet());
            entity.markPersisted(id, 0);
            insertionOrder.put(id, sequence.get());
        } else {
            T stored = byId.get(entity.id());
            if (stored == null || stored.version() != entity.version()) {
                throw new ConflictException(CONCURRENT_MODIFICATION,
                        entity + " was changed or deleted by someone else; reload it and try again");
            }
            entity.markPersisted(entity.id(), entity.version() + 1);
        }
        afterSave(entity);
        byId.put(entity.id(), copyOf(entity));
        return entity;
    }

    /** Removes an entity. */
    public synchronized void delete(T entity) {
        byId.remove(entity.id());
        insertionOrder.remove(entity.id());
    }

    /** Hook for uniqueness checks before an entity is stored. */
    protected void beforeSave(T entity) {
    }

    /** Hook for updating indexes once the entity has its id, before the copy is stored. */
    protected void afterSave(T entity) {
    }

    /** Returns copies of every stored entity, in insertion order. */
    protected List<T> all() {
        return where(entity -> true);
    }

    /** Returns copies of every stored entity that matches, in insertion order. */
    protected List<T> where(Predicate<? super T> condition) {
        return byId.values().stream()
                .sorted((a, b) -> Long.compare(insertionOrder.get(a.id()), insertionOrder.get(b.id())))
                .filter(condition)
                .map(this::copyOf)
                .toList();
    }

    /** Returns the number of stored entities. */
    public int count() {
        return byId.size();
    }
}
