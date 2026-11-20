package com.bookstore.shared.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Turns the current row of a result set into an object.
 *
 * @param <T> the object type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row. Implementations must not move the cursor.
     *
     * @param row the result set, positioned on the row to map
     * @return the object
     * @throws SQLException if a column cannot be read
     */
    T map(ResultSet row) throws SQLException;
}
