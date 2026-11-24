package com.bookstore.customer.persistence;

/** The customer repository contract on the JDBC implementation and H2 in PostgreSQL mode. */
class H2CustomerRepositoryTest extends CustomerRepositoryContract {

    @Override
    protected CustomerDb newDb() {
        return new CustomerDb();
    }
}
