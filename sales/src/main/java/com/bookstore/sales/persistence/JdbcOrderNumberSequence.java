package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderNumberSequence;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.time.Year;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Hands out order numbers from a counter row per year in {@code order_number_sequences}.
 *
 * <p>Incrementing the counter with an {@code UPDATE} locks its row until the transaction ends, so
 * two checkouts running at the same time cannot get the same number: the second waits for the
 * first. If the transaction rolls back, its number is handed out again.
 */
@ApplicationScoped
public class JdbcOrderNumberSequence implements OrderNumberSequence {

    private final JdbcExecutor jdbc;
    private final TransactionRunner transactions;

    /**
     * Creates the sequence.
     *
     * @param jdbc         runs the SQL
     * @param transactions keeps the increment and the read in one transaction
     */
    @Inject
    public JdbcOrderNumberSequence(JdbcExecutor jdbc, TransactionRunner transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcOrderNumberSequence() {
        this(null, null);
    }

    @Override
    public OrderNumber next(Year year) {
        return transactions.inTransaction(() -> {
            // PostgreSQL aborts a transaction after any failed statement, so a duplicate row must not
            // raise an error: the first order of a year creates the counter, every order then bumps it.
            jdbc.update("INSERT INTO order_number_sequences (order_year, last_number) VALUES (?, 0)"
                    + " ON CONFLICT DO NOTHING", year);
            jdbc.update("UPDATE order_number_sequences SET last_number = last_number + 1 WHERE order_year = ?", year);
            long number = jdbc.queryForLong("SELECT last_number FROM order_number_sequences WHERE order_year = ?",
                    year);
            return OrderNumber.of(year, number);
        });
    }
}
