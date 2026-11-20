package com.bookstore.sales.domain;


import java.time.Year;

/**
 * Hands out order numbers. Numbers increase within a year and the sequence restarts at 1 every
 * year. Every stored order gets a different number; whether a number taken by a transaction that
 * later rolls back is handed out again depends on the implementation.
 */
public interface OrderNumberSequence {

    /**
     * Returns the next free order number for a year. Safe to call from several threads at once.
     *
     * @param year the year the order is placed in
     * @return a number no other caller has received
     */
    OrderNumber next(Year year);
}
