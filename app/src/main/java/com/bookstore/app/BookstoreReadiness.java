package com.bookstore.app;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Tells a load balancer whether the shop can take requests (ADR-0022): the startup step has run
 * the migrations and the database answers. MicroProfile Health serves it at {@code /health/ready}.
 */
@Readiness
@ApplicationScoped
public class BookstoreReadiness implements HealthCheck {

    /** The name of the check in the health answer. */
    static final String NAME = "bookstore";

    private static final int TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final BookstoreStartup startup;

    /**
     * Creates the check.
     *
     * @param dataSource the application's pool
     * @param startup    the startup step, which knows whether the migrations are done
     */
    @Inject
    public BookstoreReadiness(DataSource dataSource, BookstoreStartup startup) {
        this.dataSource = dataSource;
        this.startup = startup;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected BookstoreReadiness() {
        this(null, null);
    }

    @Override
    public HealthCheckResponse call() {
        boolean migrated = startup.isStarted();
        boolean database = databaseAnswers();
        Map<String, Object> data = Map.of(
                "migrations", migrated ? "done" : "pending",
                "database", database ? "reachable" : "unreachable");
        // The constructor rather than the builder: the builder needs the server's provider, which tests lack.
        return new HealthCheckResponse(NAME, migrated && database ? HealthCheckResponse.Status.UP
                : HealthCheckResponse.Status.DOWN, Optional.of(data));
    }

    private boolean databaseAnswers() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }
}
