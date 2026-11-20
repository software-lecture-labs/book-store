package com.bookstore.sales.persistence;

import com.bookstore.shared.jdbc.TestDatabase;

/** Two relays racing on H2 in PostgreSQL mode. */
class H2OutboxRelayRaceTest extends OutboxRelayRaceContract {

    @Override
    protected TestDatabase newDatabase() {
        return TestDatabase.withModules("catalog", "customer", "sales");
    }
}
