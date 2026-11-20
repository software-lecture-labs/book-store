package com.bookstore.review.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The review tables' constraints on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresReviewSchemaConstraintsTest extends ReviewSchemaConstraintsTest {

    @Override
    protected ReviewDb newDb() {
        return new ReviewDb(PostgresTestDatabase.withModules("catalog", "customer", "sales", "review"));
    }
}
