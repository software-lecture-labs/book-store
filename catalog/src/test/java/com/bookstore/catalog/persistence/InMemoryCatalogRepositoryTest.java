package com.bookstore.catalog.persistence;

/** The catalog repository contract on the in-memory implementations. */
class InMemoryCatalogRepositoryTest extends CatalogRepositoryContract {

    @Override
    protected CatalogDb newDb() {
        return CatalogDb.inMemory();
    }
}
