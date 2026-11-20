package com.bookstore.customer.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The customer repository contract on the JDBC implementation and PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresCustomerRepositoryTest extends CustomerRepositoryContract {

    @Override
    protected CustomerDb newDb() {
        return new CustomerDb(PostgresTestDatabase.withModules("customer"));
    }
}
