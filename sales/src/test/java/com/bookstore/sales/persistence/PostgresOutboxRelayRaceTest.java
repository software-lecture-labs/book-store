package com.bookstore.sales.persistence;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.PostgresTestDatabase;
import com.bookstore.shared.jdbc.TestDatabase;

/** Two relays racing on PostgreSQL 17, when Docker is available. */
@EnabledIfDocker
class PostgresOutboxRelayRaceTest extends OutboxRelayRaceContract {

    @Override
    protected TestDatabase newDatabase() {
        return PostgresTestDatabase.withModules("catalog", "customer", "sales");
    }
}
