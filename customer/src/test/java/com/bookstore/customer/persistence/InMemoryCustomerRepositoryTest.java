package com.bookstore.customer.persistence;

/** The customer repository contract on the in-memory implementation. */
class InMemoryCustomerRepositoryTest extends CustomerRepositoryContract {

    @Override
    protected CustomerDb newDb() {
        return CustomerDb.inMemory();
    }
}
