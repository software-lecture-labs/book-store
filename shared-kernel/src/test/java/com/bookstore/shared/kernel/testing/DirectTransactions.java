package com.bookstore.shared.kernel.testing;

import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.util.function.Supplier;

/**
 * Runs work directly and counts how many transactions were started. It cannot roll anything back:
 * in-memory objects stay changed after an exception. Rollback is tested against a real database.
 */
public final class DirectTransactions implements TransactionRunner {

    private int started;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        started++;
        return work.get();
    }

    /**
     * Returns the number of transactions started so far.
     *
     * @return the count
     */
    public int started() {
        return started;
    }
}
