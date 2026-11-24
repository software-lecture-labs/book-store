package com.bookstore.app;

import com.bookstore.shared.jdbc.EnabledIfDocker;
import com.bookstore.shared.jdbc.ModuleMigrations;
import com.bookstore.shared.jdbc.PostgresTestDatabase;

/**
 * Checkout across all modules on PostgreSQL 17, when Docker is available: rollback, the race for
 * the last copy and the dual-write problem on the real database.
 */
@EnabledIfDocker
class PostgresCheckoutWithDatabaseTest extends CheckoutWithDatabaseTest {

    @Override
    protected JdbcBookstore newStore() {
        return new JdbcBookstore(PostgresTestDatabase.withModules(ModuleMigrations.ALL.toArray(String[]::new)));
    }
}
