package com.bookstore.shared.kernel.transaction;

import java.util.function.Supplier;

/**
 * Runs work as one database transaction: either every change is stored, or, if the work throws,
 * none is.
 *
 * <p>Use cases decide where a transaction starts and ends; the persistence module decides how. The
 * JDBC implementation keeps one connection for the duration of {@code work} and commits or rolls
 * back at the end.
 */
public interface TransactionRunner {

    /**
     * Runs work in a transaction and returns its result.
     *
     * @param work the work; any exception it throws rolls the transaction back and is rethrown
     * @param <T>  result type
     * @return what {@code work} returned
     */
    <T> T inTransaction(Supplier<T> work);

    /**
     * Runs work that returns nothing in a transaction.
     *
     * @param work the work; any exception it throws rolls the transaction back and is rethrown
     */
    default void run(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
