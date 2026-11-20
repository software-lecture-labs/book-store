package com.bookstore.shared.jdbc;

import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Reads columns into the types the domain uses. The counterpart of the conversions in
 * {@link JdbcExecutor}.
 */
public final class Columns {

    private Columns() {
    }

    /**
     * Reads a UTC {@code TIMESTAMP} column.
     *
     * @param row    the result set
     * @param column the column name
     * @return the instant
     * @throws SQLException if the column cannot be read
     */
    public static Instant instant(ResultSet row, String column) throws SQLException {
        LocalDateTime value = row.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /**
     * Reads a {@code DECIMAL} column as money in the store's currency.
     *
     * @param row    the result set
     * @param column the column name
     * @return the amount
     * @throws SQLException if the column cannot be read
     */
    public static Money money(ResultSet row, String column) throws SQLException {
        BigDecimal amount = row.getBigDecimal(column);
        return amount == null ? null : Money.of(amount);
    }

    /**
     * Reads a nullable {@code INT} column holding a year.
     *
     * @param row    the result set
     * @param column the column name
     * @return the year, or empty for {@code NULL}
     * @throws SQLException if the column cannot be read
     */
    public static Optional<Year> year(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? Optional.empty() : Optional.of(Year.of(value));
    }

    /**
     * Reads a {@code VARCHAR} column holding an enum constant's name.
     *
     * @param row    the result set
     * @param column the column name
     * @param type   the enum type
     * @param <E>    the enum type
     * @return the constant
     * @throws SQLException if the column cannot be read
     */
    public static <E extends Enum<E>> E enumValue(ResultSet row, String column, Class<E> type) throws SQLException {
        return Enum.valueOf(type, row.getString(column));
    }

    /**
     * Reads a nullable {@code VARCHAR} column.
     *
     * @param row    the result set
     * @param column the column name
     * @return the text, or empty for {@code NULL}
     * @throws SQLException if the column cannot be read
     */
    public static Optional<String> optionalString(ResultSet row, String column) throws SQLException {
        return Optional.ofNullable(row.getString(column));
    }
}
