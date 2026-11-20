package com.bookstore.shared.jdbc;

/** The JDBC toolkit's tests on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresJdbcToolkitTest extends JdbcToolkitTest {

    @Override
    protected TestDatabase newDatabase() {
        return PostgresTestDatabase.withModules("toolkit");
    }
}
