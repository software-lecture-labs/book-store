package com.bookstore.shared.jdbc;

import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.sql.DataSource;

/**
 * Runs work in a JDBC transaction and gives repositories the transaction's connection.
 *
 * <p>The connection is kept in a {@link ThreadLocal}: every repository call made by the same
 * thread during {@code work} uses the same connection, so all of them commit or roll back
 * together, without the connection being passed around as a parameter.
 *
 * <p>A transaction started inside another one joins it (the outer transaction decides). Calls made
 * outside any transaction get a short-lived connection in auto-commit mode.
 */
public class JdbcTransactionRunner implements TransactionRunner, ConnectionProvider {

    private final DataSource dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    /**
     * Creates the runner.
     *
     * @param dataSource where connections come from, normally a connection pool
     */
    public JdbcTransactionRunner(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        Connection connection = open();
        try {
            connection.setAutoCommit(false);
            current.set(connection);
            T result = work.get();
            connection.commit();
            return result;
        } catch (RuntimeException | Error e) {
            rollback(connection, e);
            throw e;
        } catch (SQLException e) {
            rollback(connection, e);
            throw new DataAccessException("Could not commit", e);
        } finally {
            current.remove();
            close(connection);
        }
    }

    /**
     * Tells whether the calling thread is inside a transaction.
     *
     * @return {@code true} inside {@link #inTransaction}
     */
    public boolean isInTransaction() {
        return current.get() != null;
    }

    /**
     * Runs JDBC code with the current transaction's connection, or with a new auto-commit connection
     * that is closed afterwards.
     *
     * @param callback the JDBC code
     * @param <T>      result type
     * @return what the callback returned
     */
    @Override
    public <T> T withConnection(ConnectionCallback<T> callback) {
        Connection transactional = current.get();
        try {
            if (transactional != null) {
                return callback.doWith(transactional);
            }
            try (Connection connection = open()) {
                return callback.doWith(connection);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database access failed", e);
        }
    }

    private Connection open() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Could not open a connection", e);
        }
    }

    private static void rollback(Connection connection, Throwable reason) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            reason.addSuppressed(rollbackFailure);
        }
    }

    private static void close(Connection connection) {
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException ignored) {
            // The pool discards broken connections; nothing useful can be done here.
        }
    }

}
