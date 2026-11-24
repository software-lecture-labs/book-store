package com.bookstore.shared.jdbc;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.Identifier;
import com.bookstore.shared.kernel.money.Money;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Runs SQL with {@link PreparedStatement}s and maps the results.
 *
 * <p>Every method follows the same steps (Template Method pattern): get a connection from the
 * {@link JdbcTransactionRunner}, prepare the statement, bind the parameters, execute, map the
 * result, and close the statement and result set with try-with-resources. Callers supply only
 * the parts that differ: the SQL, the parameters and a {@link RowMapper}.
 *
 * <p>Parameters are always bound, never concatenated into the SQL, so user input cannot change
 * the meaning of a query (SQL injection). Values are converted on the way in: {@link Instant} to a
 * UTC {@code TIMESTAMP}, a typed {@link Identifier} to its value, {@link Money} to its amount,
 * enums to their name, {@link Year} to an integer.
 */
public class JdbcExecutor {

    /** Every table with a generated key calls it {@code id}; naming it works on H2 and PostgreSQL alike. */
    private static final String[] GENERATED_ID = {"id"};

    /** Error code for an update that lost the race against a concurrent update. */
    public static final String CONCURRENT_MODIFICATION = ConflictException.CONCURRENT_MODIFICATION;

    private final ConnectionProvider connections;

    /**
     * Creates the executor.
     *
     * @param connections supplies the connection: the current transaction's, or the container's
     */
    public JdbcExecutor(ConnectionProvider connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    /**
     * Runs a query and maps every row.
     *
     * @param sql    the SQL, with {@code ?} placeholders
     * @param mapper turns a row into an object
     * @param params values for the placeholders, in order
     * @param <T>    the object type
     * @return the mapped rows, in result order
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return execute(sql, params, statement -> {
            try (ResultSet rows = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(mapper.map(rows));
                }
                return result;
            }
        });
    }

    /**
     * Runs a query that returns at most one row.
     *
     * @param sql    the SQL
     * @param mapper turns the row into an object
     * @param params values for the placeholders
     * @param <T>    the object type
     * @return the mapped row, or empty if there was none
     * @throws IllegalStateException if the query returned more than one row
     */
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        if (rows.size() > 1) {
            throw new IllegalStateException("Expected at most one row but got " + rows.size() + ": " + sql);
        }
        return rows.stream().findFirst();
    }

    /**
     * Runs a query that returns a single number, such as {@code COUNT(*)}.
     *
     * @param sql    the SQL
     * @param params values for the placeholders
     * @return the number in the first column of the first row
     */
    public long queryForLong(String sql, Object... params) {
        return queryOne(sql, row -> row.getLong(1), params)
                .orElseThrow(() -> new IllegalStateException("Query returned no rows: " + sql));
    }

    /**
     * Runs a query that tests whether some row exists.
     *
     * @param sql    a {@code SELECT} that returns rows only when the answer is yes
     * @param params values for the placeholders
     * @return {@code true} if the query returned a row
     */
    public boolean exists(String sql, Object... params) {
        return !query(sql, row -> Boolean.TRUE, params).isEmpty();
    }

    /**
     * Runs an {@code INSERT}, {@code UPDATE} or {@code DELETE}.
     *
     * @param sql    the SQL
     * @param params values for the placeholders
     * @return the number of rows changed
     */
    public int update(String sql, Object... params) {
        return execute(sql, params, PreparedStatement::executeUpdate);
    }

    /**
     * Runs an {@code UPDATE} guarded by a version check and fails if no row matched, which means
     * someone else changed or deleted the row since it was loaded (optimistic locking).
     *
     * @param what   describes the row, for the error message
     * @param sql    an {@code UPDATE ... WHERE id = ? AND version = ?}
     * @param params values for the placeholders
     * @throws ConflictException if no row was updated
     */
    public void updateVersioned(String what, String sql, Object... params) {
        if (update(sql, params) == 0) {
            throw new ConflictException(CONCURRENT_MODIFICATION,
                    what + " was changed by someone else in the meantime; please reload and try again");
        }
    }

    /**
     * Runs an {@code INSERT} into a table whose {@code id} column is generated and returns the new id.
     *
     * @param sql    the SQL
     * @param params values for the placeholders
     * @return the generated id
     */
    public long insert(String sql, Object... params) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, GENERATED_ID)) {
                bind(statement, params);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new IllegalStateException("No key was generated: " + sql);
                    }
                    return keys.getLong(1);
                }
            }
        });
    }

    /**
     * Runs the same {@code INSERT} or {@code UPDATE} for many rows in one round trip.
     *
     * @param sql  the SQL
     * @param rows one parameter array per row
     */
    public void batch(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Object[] params : rows) {
                    bind(statement, params);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        });
    }

    /**
     * Returns {@code count} comma-separated placeholders for an {@code IN (...)} list.
     *
     * @param count how many values the list has, at least one
     * @return for example {@code "?, ?, ?"}
     */
    public static String placeholders(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("an IN list needs at least one value");
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < count; i++) {
            joiner.add("?");
        }
        return joiner.toString();
    }

    /**
     * Turns a collection into a parameter array, for use with {@link #placeholders(int)}.
     *
     * @param values the values
     * @return the values as an array
     */
    public static Object[] params(Collection<?> values) {
        return values.toArray();
    }

    // ---------------------------------------------------------------- the fixed steps

    private <T> T execute(String sql, Object[] params, StatementCallback<T> callback) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = prepare(connection, sql)) {
                bind(statement, params);
                return callback.doWith(statement);
            }
        });
    }

    private static PreparedStatement prepare(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, toJdbc(params[i]));
        }
    }

    /**
     * Converts a Java value into one every JDBC driver understands.
     *
     * @param value a parameter value, may be {@code null}
     * @return the value to bind
     */
    static Object toJdbc(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            case Identifier id -> id.value();
            case Money money -> money.amount();
            case Year year -> year.getValue();
            case Enum<?> constant -> constant.name();
            case Optional<?> optional -> toJdbc(optional.orElse(null));
            default -> value;
        };
    }

    @FunctionalInterface
    private interface StatementCallback<T> {
        T doWith(PreparedStatement statement) throws SQLException;
    }
}
