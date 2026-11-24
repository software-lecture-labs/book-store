package com.bookstore.shared.jdbc;

import com.bookstore.shared.kernel.money.Money;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

/**
 * Maps a row to a {@code record} by reflection: each record component is read from the column with
 * the same name in {@code snake_case}. A component {@code postalCode} is read from
 * {@code postal_code}.
 *
 * <p>Used for the flat "row" records inside repositories, where writing a mapper by hand would only
 * repeat the component list. Supported component types: {@code String}, {@code int}, {@code long},
 * {@code boolean}, {@code BigDecimal}, {@link Money}, {@link Instant} and enums.
 *
 * <p>The reflective work (finding the components and the constructor) is done once, when the
 * mapper is created; mapping a row only reads the columns and calls the constructor.
 *
 * @param <R> the record type
 */
public final class RecordRowMapper<R extends Record> implements RowMapper<R> {

    private final Class<R> type;
    private final RecordComponent[] components;
    private final String[] columns;
    private final Constructor<R> constructor;

    private RecordRowMapper(Class<R> type) {
        this.type = type;
        this.components = type.getRecordComponents();
        this.columns = Arrays.stream(components).map(c -> toSnakeCase(c.getName())).toArray(String[]::new);
        Class<?>[] parameterTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        try {
            this.constructor = type.getDeclaredConstructor(parameterTypes);
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type + " has no canonical constructor", e);
        }
    }

    /**
     * Creates a mapper for a record type.
     *
     * @param type the record class
     * @param <R>  the record type
     * @return the mapper
     * @throws IllegalArgumentException if {@code type} is not a record
     */
    public static <R extends Record> RecordRowMapper<R> of(Class<R> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException(type + " is not a record");
        }
        return new RecordRowMapper<>(type);
    }

    @Override
    public R map(ResultSet row) throws SQLException {
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = read(row, columns[i], components[i].getType());
        }
        try {
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Could not create " + type.getSimpleName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create " + type.getSimpleName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object read(ResultSet row, String column, Class<?> type) throws SQLException {
        if (type == String.class) {
            return row.getString(column);
        } else if (type == int.class) {
            return row.getInt(column);
        } else if (type == long.class) {
            return row.getLong(column);
        } else if (type == boolean.class) {
            return row.getBoolean(column);
        } else if (type == BigDecimal.class) {
            return row.getBigDecimal(column);
        } else if (type == Money.class) {
            return Columns.money(row, column);
        } else if (type == Instant.class) {
            return Columns.instant(row, column);
        } else if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, row.getString(column));
        }
        throw new IllegalArgumentException("Unsupported component type " + type.getName() + " for column " + column);
    }

    /**
     * Converts {@code camelCase} into {@code snake_case}.
     *
     * @param name a Java name, for example {@code postalCode}
     * @return the column name, for example {@code postal_code}
     */
    static String toSnakeCase(String name) {
        StringBuilder column = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                column.append('_').append(Character.toLowerCase(c));
            } else {
                column.append(c);
            }
        }
        return column.toString().toLowerCase(Locale.ROOT);
    }
}
