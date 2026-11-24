package com.bookstore.review.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/** The review repository contract on the JPA implementation and PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class JpaPostgresReviewRepositoryTest extends ReviewRepositoryContract {

    @Override
    protected ReviewDb newDb() {
        return ReviewDb.jpa(PostgresTestDatabase.withModules("catalog", "customer", "sales", "review"));
    }
}
