package com.bookstore.sales.persistence;

/** The sales repository contract on the JDBC implementations and H2 in PostgreSQL mode. */
class H2SalesRepositoryTest extends SalesRepositoryContract {

    @Override
    protected SalesDb newDb() {
        return new SalesDb();
    }
}
