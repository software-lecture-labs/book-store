package com.bookstore.shared.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ConflictException;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

/** The transaction runner, the executor and the reflective row mapper against a real database. */
class JdbcToolkitTest {

    private final TestDatabase db = newDatabase();

    /** @return the database the tests run on: H2 here, PostgreSQL in {@link PostgresJdbcToolkitTest} */
    protected TestDatabase newDatabase() {
        return TestDatabase.withModules("toolkit");
    }

    @Test
    void migrationsRunOnlyOnce() {
        assertThat(ModuleMigrations.migrate(db.dataSource(), java.util.List.of("toolkit"))).isZero();
        assertThat(db.jdbc().queryForLong("SELECT COUNT(*) FROM labels")).isZero();
    }

    @Test
    void committedWorkIsVisibleAfterwards() {
        db.transactions().run(() -> insertSequence(2026, 5));

        assertThat(lastNumber(2026)).isEqualTo(5);
        assertThat(db.transactions().isInTransaction()).isFalse();
    }

    @Test
    void exceptionRollsBackEverythingInTheTransaction() {
        assertThatThrownBy(() -> db.transactions().run(() -> {
            insertSequence(2026, 1);
            insertSequence(2027, 1);
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(db.jdbc().queryForLong("SELECT COUNT(*) FROM counters")).isZero();
        assertThat(db.transactions().isInTransaction()).isFalse();
    }

    @Test
    void innerTransactionJoinsTheOuterOne() {
        assertThatThrownBy(() -> db.transactions().run(() -> {
            db.transactions().run(() -> insertSequence(2026, 1));
            throw new IllegalStateException("outer fails");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(db.jdbc().queryForLong("SELECT COUNT(*) FROM counters"))
                .as("the inner work is rolled back with the outer transaction")
                .isZero();
    }

    @Test
    void callsOutsideATransactionAutoCommit() {
        insertSequence(2030, 3);

        assertThat(lastNumber(2030)).isEqualTo(3);
    }

    @Test
    void constraintViolationsAreRecognisedByName() {
        insertSequence(2026, 1);

        assertThatThrownBy(() -> insertSequence(2026, 2))
                .isInstanceOfSatisfying(DataAccessException.class, e -> {
                    assertThat(e.isConstraintViolation()).isTrue();
                    assertThat(e.sqlState()).startsWith("23");
                });
    }

    @Test
    void versionedUpdateFailsWhenTheRowChanged() {
        long id = db.jdbc().insert("INSERT INTO labels (name, name_key) VALUES (?, ?)", "A", "a");

        db.jdbc().updateVersioned("Label", "UPDATE labels SET version = version + 1 WHERE id = ? AND version = ?", id, 0);

        assertThatThrownBy(() -> db.jdbc().updateVersioned("Label",
                "UPDATE labels SET version = version + 1 WHERE id = ? AND version = ?", id, 0))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(JdbcExecutor.CONCURRENT_MODIFICATION));
    }

    @Test
    void parametersAreBoundNotConcatenated() {
        String hostile = "x'; DROP TABLE labels; --";
        db.jdbc().insert("INSERT INTO labels (name, name_key) VALUES (?, ?)", hostile, hostile);

        assertThat(db.jdbc().queryOne("SELECT name FROM labels WHERE name = ?", row -> row.getString(1), hostile))
                .contains(hostile);
    }

    @Test
    void recordRowMapperReadsSnakeCaseColumnsIntoComponents() {
        db.jdbc().update("INSERT INTO counters (counter_year, last_number) VALUES (?, ?)", 2026, 42);

        SequenceRow row = db.jdbc().queryOne("SELECT counter_year, last_number, CAST(? AS DECIMAL(12, 2)) AS some_amount, "
                        + "CAST(? AS TIMESTAMP(6)) AS seen_at FROM counters",
                RecordRowMapper.of(SequenceRow.class), new BigDecimal("1.50"), Instant.parse("2026-01-02T03:04:05Z"))
                .orElseThrow();

        assertThat(row).isEqualTo(new SequenceRow(2026, 42, new BigDecimal("1.50"),
                Instant.parse("2026-01-02T03:04:05Z")));
    }

    @Test
    void snakeCaseConversion() {
        assertThat(RecordRowMapper.toSnakeCase("postalCode")).isEqualTo("postal_code");
        assertThat(RecordRowMapper.toSnakeCase("id")).isEqualTo("id");
        assertThat(RecordRowMapper.toSnakeCase("shipPostalCode")).isEqualTo("ship_postal_code");
    }

    @Test
    void placeholdersForInLists() {
        assertThat(JdbcExecutor.placeholders(3)).isEqualTo("?, ?, ?");
        assertThatThrownBy(() -> JdbcExecutor.placeholders(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private void insertSequence(int year, long number) {
        db.jdbc().update("INSERT INTO counters (counter_year, last_number) VALUES (?, ?)", year, number);
    }

    private long lastNumber(int year) {
        return db.jdbc().queryForLong("SELECT last_number FROM counters WHERE counter_year = ?", year);
    }

    record SequenceRow(int counterYear, long lastNumber, BigDecimal someAmount, Instant seenAt) {
    }
}
