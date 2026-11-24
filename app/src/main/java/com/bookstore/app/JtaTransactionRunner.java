package com.bookstore.app;

import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * The {@link TransactionRunner} port on the server: {@code @Transactional} opens a JTA transaction
 * around the work, commits it when the work returns and rolls it back when the work throws
 * (ADR-0010). A call inside a running transaction joins it, exactly like the
 * {@code JdbcTransactionRunner} the tests use, so the use cases behave the same with and without
 * a container.
 */
@ApplicationScoped
public class JtaTransactionRunner implements TransactionRunner {

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void run(Runnable work) {
        work.run();
    }
}
