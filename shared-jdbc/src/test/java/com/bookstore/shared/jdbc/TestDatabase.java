package com.bookstore.shared.jdbc;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

/**
 * A fresh in-memory H2 database in PostgreSQL mode, migrated with the given modules' migrations, plus
 * the JDBC toolkit wired to it. Each test creates its own, so tests never see each other's data.
 *
 * @param dataSource   the database
 * @param transactions transaction runner on that database
 * @param jdbc         executor using that runner
 */
public record TestDatabase(DataSource dataSource, JdbcTransactionRunner transactions, JdbcExecutor jdbc) {

    /**
     * Creates and migrates a database.
     *
     * @param modules modules whose migrations to apply, in dependency order
     * @return the database
     */
    public static TestDatabase withModules(String... modules) {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        h2.setUser("sa");
        ModuleMigrations.migrate(h2, List.of(modules));
        JdbcTransactionRunner transactions = new JdbcTransactionRunner(h2);
        return new TestDatabase(h2, transactions, new JdbcExecutor(transactions));
    }
}
