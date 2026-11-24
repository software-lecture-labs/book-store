package com.bookstore.shared.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

/**
 * Takes a connection from a container-managed data source for each piece of JDBC code and gives it
 * back afterwards (ADR-0010). Inside a JTA transaction the container enlists the connection and
 * hands the same one out again, so every statement of a use case commits or rolls back together;
 * this class never commits itself.
 */
public final class DataSourceConnections implements ConnectionProvider {

    private final DataSource dataSource;

    /**
     * Creates the provider.
     *
     * @param dataSource the container's data source
     */
    public DataSourceConnections(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public <T> T withConnection(ConnectionCallback<T> callback) {
        try (Connection connection = dataSource.getConnection()) {
            return callback.doWith(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Database access failed", e);
        }
    }
}
