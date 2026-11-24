package com.bookstore.review.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The review repository contract on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresReviewRepositoryTest extends ReviewRepositoryContract {

    @Override
    protected ReviewDb newDb() {
        return new ReviewDb(PostgresTestDatabase.withModules("catalog", "customer", "sales", "review"));
    }
}
