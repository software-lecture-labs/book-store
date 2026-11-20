package com.bookstore.review.persistence;

/** The review repository contract on the in-memory implementations. */
class InMemoryReviewRepositoryTest extends ReviewRepositoryContract {

    @Override
    protected ReviewDb newDb() {
        return ReviewDb.inMemory();
    }
}
