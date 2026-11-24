package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.testing.TestClock;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

/** The readiness check of ADR-0022: ready once the migrations ran and while the database answers. */
class BookstoreReadinessTest {

    private final TestDatabase database = TestDatabase.withModules();

    @Test
    void notReadyUntilTheMigrationsRanThenReady() {
        BookstoreStartup startup = new BookstoreStartup(database.dataSource(), null, null, TestClock.CLOCK, false);
        BookstoreReadiness readiness = new BookstoreReadiness(database.dataSource(), startup);

        HealthCheckResponse before = readiness.call();
        assertThat(before.getName()).isEqualTo(BookstoreReadiness.NAME);
        assertThat(before.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(before.getData()).hasValueSatisfying(data -> assertThat(data).containsEntry("migrations", "pending"));

        startup.onStart(null);

        HealthCheckResponse after = readiness.call();
        assertThat(after.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(after.getData()).hasValueSatisfying(data -> assertThat(data)
                .containsEntry("migrations", "done").containsEntry("database", "reachable"));
    }

    @Test
    void notReadyWhenTheDatabaseDoesNotAnswer() {
        BookstoreStartup startup = new BookstoreStartup(database.dataSource(), null, null, TestClock.CLOCK, false);
        startup.onStart(null);
        DataSource broken = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    throw new SQLException("connection refused");
                });

        HealthCheckResponse answer = new BookstoreReadiness(broken, startup).call();

        assertThat(answer.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(answer.getData()).hasValueSatisfying(data -> assertThat(data).containsEntry("database", "unreachable"));
    }
}
