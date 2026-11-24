package com.bookstore.sales.persistence;

/** The sales repository contract on the in-memory implementations. */
class InMemorySalesRepositoryTest extends SalesRepositoryContract {

    @Override
    protected SalesDb newDb() {
        return SalesDb.inMemory();
    }
}
