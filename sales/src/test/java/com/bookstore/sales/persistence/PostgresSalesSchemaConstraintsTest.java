package com.bookstore.sales.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The sales tables' constraints on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresSalesSchemaConstraintsTest extends SalesSchemaConstraintsTest {

    @Override
    protected SalesDb newDb() {
        return new SalesDb(PostgresTestDatabase.withModules("catalog", "customer", "sales"));
    }
}
