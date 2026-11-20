package com.bookstore.catalog.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The catalog tables' constraints on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresCatalogSchemaConstraintsTest extends CatalogSchemaConstraintsTest {

    @Override
    protected CatalogDb newDb() {
        return new CatalogDb(PostgresTestDatabase.withModules("catalog"));
    }
}
