package com.bookstore.shared.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fresh PostgreSQL 17 databases for the contract tests (ADR-0008). One container is started per
 * test run and shared; every call creates a new, empty database in it and applies the given
 * modules' migrations, so tests never see each other's rows.
 *
 * <p>Use it only from test classes annotated with {@link EnabledIfDocker}: without Docker there is
 * no container to start.
 */
public final class PostgresTestDatabase {

    private static final String IMAGE = "postgres:17";
    private static final AtomicInteger DATABASES = new AtomicInteger();
    private static PostgreSQLContainer container;

    private PostgresTestDatabase() {
    }

    /**
     * Creates and migrates a database.
     *
     * @param modules modules whose migrations to apply, in dependency order
     * @return the database
     */
    public static TestDatabase withModules(String... modules) {
        PostgreSQLContainer server = server();
        String name = "bookstore_" + DATABASES.incrementAndGet();
        try (Connection admin = DriverManager.getConnection(server.getJdbcUrl(), server.getUsername(),
                server.getPassword()); Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database " + name, e);
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] {server.getHost()});
        dataSource.setPortNumbers(new int[] {server.getMappedPort(5432)});
        dataSource.setDatabaseName(name);
        dataSource.setUser(server.getUsername());
        dataSource.setPassword(server.getPassword());
        ModuleMigrations.migrate(dataSource, List.of(modules));
        JdbcTransactionRunner transactions = new JdbcTransactionRunner(dataSource);
        return new TestDatabase(dataSource, transactions, new JdbcExecutor(transactions));
    }

    private static synchronized PostgreSQLContainer server() {
        if (container == null) {
            container = new PostgreSQLContainer(IMAGE);
            // Testcontainers' Ryuk container removes it when the test JVM exits.
            container.start();
        }
        return container;
    }
}
