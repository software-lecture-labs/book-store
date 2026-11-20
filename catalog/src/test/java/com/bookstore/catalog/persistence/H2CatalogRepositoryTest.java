package com.bookstore.catalog.persistence;

/** The catalog repository contract on the JDBC implementations and H2 in PostgreSQL mode. */
class H2CatalogRepositoryTest extends CatalogRepositoryContract {

    @Override
    protected CatalogDb newDb() {
        return new CatalogDb();
    }
}
