package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderNumberSequence;

import java.time.Year;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** One counter per year in memory; safe to call from several threads, like the database counter. */
public final class InMemoryOrderNumberSequence implements OrderNumberSequence {

    private final Map<Year, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public OrderNumber next(Year year) {
        long sequence = counters.computeIfAbsent(year, y -> new AtomicLong()).incrementAndGet();
        return OrderNumber.of(year, sequence);
    }
}
