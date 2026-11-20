package com.bookstore.shared.kernel.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** A clock stopped at a fixed moment, shared by every module's tests. */
public final class TestClock {

    /** The clock. */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC);

    /** The moment {@link #CLOCK} is stopped at. */
    public static final Instant NOW = CLOCK.instant();

    private TestClock() {
    }
}
