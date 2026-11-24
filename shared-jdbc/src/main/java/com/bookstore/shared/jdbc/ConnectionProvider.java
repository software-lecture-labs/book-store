package com.bookstore.shared.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Where {@link JdbcExecutor} gets its connection from. Outside a container the
 * {@link JdbcTransactionRunner} hands out the connection of the current transaction; inside one,
 * {@link DataSourceConnections} asks the container's data source, which enlists the connection in
 * the JTA transaction (ADR-0010).
 */
public interface ConnectionProvider {

    /**
     * Runs JDBC code with a connection.
     *
     * @param callback the JDBC code
     * @param <T>      result type
     * @return what the callback returned
     * @throws DataAccessException if the database reports an error
     */
    <T> T withConnection(ConnectionCallback<T> callback);

    /**
     * JDBC code that needs a connection.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    interface ConnectionCallback<T> {

        /**
         * Runs the code.
         *
         * @param connection an open connection; do not close it
         * @return the result
         * @throws SQLException passed on to the provider, which wraps it
         */
        T doWith(Connection connection) throws SQLException;
    }
}
