package com.bookstore.review.persistence;

/** The review repository contract on the JDBC implementations and H2 in PostgreSQL mode. */
class H2ReviewRepositoryTest extends ReviewRepositoryContract {

    @Override
    protected ReviewDb newDb() {
        return new ReviewDb();
    }
}
