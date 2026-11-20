package com.bookstore.shared.kernel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.Identifier;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EntityTest {

    @Test
    void newEntityHasNoIdUntilStored() {
        Thing thing = new Thing();

        assertThat(thing.isNew()).isTrue();
        assertThat(thing.version()).isZero();
        assertThatThrownBy(thing::id).isInstanceOf(IllegalStateException.class);

        thing.markPersisted(new ThingId(5), 1);

        assertThat(thing.isNew()).isFalse();
        assertThat(thing.id()).isEqualTo(new ThingId(5));
        assertThat(thing.version()).isEqualTo(1);
    }

    @Test
    void idCannotChangeOnceAssignedButVersionCan() {
        Thing thing = new Thing(new ThingId(5), 0);

        thing.markPersisted(new ThingId(5), 1);
        assertThat(thing.version()).isEqualTo(1);

        assertThatThrownBy(() -> thing.markPersisted(new ThingId(6), 2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> thing.markPersisted(new ThingId(5), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ThingId(0)).isInstanceOf(ValidationException.class);
    }

    @Test
    void storedEntitiesAreEqualByIdAndNewOnesOnlyToThemselves() {
        Thing first = new Thing(new ThingId(5), 0);
        Thing sameId = new Thing(new ThingId(5), 3);
        Thing newOne = new Thing();
        Thing otherNewOne = new Thing();

        assertThat(first).isEqualTo(sameId);
        assertThat(newOne).isEqualTo(newOne).isNotEqualTo(otherNewOne).isNotEqualTo(first);
    }

    @Test
    void hashCodeDoesNotChangeWhenTheIdIsAssigned() {
        Thing thing = new Thing();
        Set<Thing> set = new HashSet<>(Set.of(thing));

        thing.markPersisted(new ThingId(9), 0);

        assertThat(set).contains(thing);
    }

    @Test
    void eventsArePulledOnce() {
        Thing thing = new Thing();
        Happened event = new Happened(Instant.EPOCH);

        thing.happen(event);

        assertThat(thing.pullEvents()).containsExactly(event);
        assertThat(thing.pullEvents()).isEmpty();
    }

    private static final class Thing extends Entity<ThingId> {

        Thing() {
        }

        Thing(ThingId id, int version) {
            super(id, version);
        }

        void happen(DomainEvent event) {
            raise(event);
        }
    }

    private record ThingId(long value) implements Identifier {

        ThingId {
            Identifier.requireValid(value, "ThingId");
        }
    }

    private record Happened(Instant occurredAt) implements DomainEvent {
    }
}
