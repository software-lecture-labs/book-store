package com.bookstore.catalog.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The catalog repository contract on the JDBC implementations and PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresCatalogRepositoryTest extends CatalogRepositoryContract {

    @Override
    protected CatalogDb newDb() {
        return new CatalogDb(PostgresTestDatabase.withModules("catalog"));
    }
}
