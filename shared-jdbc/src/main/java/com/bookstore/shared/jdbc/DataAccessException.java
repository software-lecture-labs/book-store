package com.bookstore.shared.jdbc;

import java.sql.SQLException;

/**
 * A database error that is not the caller's fault and cannot be fixed by changing the request,
 * such as a lost connection or a syntax error in our own SQL. Maps to HTTP 500.
 *
 * <p>It wraps the checked {@link SQLException} in an unchecked exception so that it does not leak
 * into the application layer's method signatures.
 */
public class DataAccessException extends RuntimeException {

    /**
     * Wraps a JDBC error.
     *
     * @param message what was being done
     * @param cause   the JDBC error
     */
    public DataAccessException(String message, SQLException cause) {
        super(message + ": " + cause.getMessage() + " [SQLState " + cause.getSQLState() + "]", cause);
    }

    /**
     * Returns the SQL state of the underlying error, used to recognise constraint violations.
     *
     * @return the five-character SQL state, or {@code null} if the driver gave none
     */
    public String sqlState() {
        return ((SQLException) getCause()).getSQLState();
    }

    /**
     * Tells whether the error was caused by a constraint, such as a duplicate unique key. Such
     * errors are usually the caller's fault and are turned into business errors by repositories.
     *
     * @return {@code true} for SQL states of class 23 (integrity constraint violation)
     */
    public boolean isConstraintViolation() {
        String state = sqlState();
        return state != null && state.startsWith("23");
    }

    /**
     * Tells whether a constraint violation concerns a constraint with the given name. Both PostgreSQL and
     * H2 name the violated constraint or index in their error message.
     *
     * @param constraintName for example {@code "uq_users_email"}
     * @return {@code true} if this is a violation of that constraint
     */
    public boolean violates(String constraintName) {
        return isConstraintViolation() && getMessage().toLowerCase().contains(constraintName.toLowerCase());
    }
}
