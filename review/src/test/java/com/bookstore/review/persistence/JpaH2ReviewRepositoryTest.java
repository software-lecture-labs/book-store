package com.bookstore.review.persistence;

import com.bookstore.shared.jdbc.TestDatabase;

/** The review repository contract on the JPA implementation and H2 in PostgreSQL mode (ADR-0023). */
class JpaH2ReviewRepositoryTest extends ReviewRepositoryContract {

    @Override
    protected ReviewDb newDb() {
        return ReviewDb.jpa(TestDatabase.withModules("catalog", "customer", "sales", "review"));
    }
}
