package com.bookstore.customer.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The customer tables' constraints on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresCustomerSchemaConstraintsTest extends CustomerSchemaConstraintsTest {

    @Override
    protected CustomerDb newDb() {
        return new CustomerDb(PostgresTestDatabase.withModules("customer"));
    }
}
