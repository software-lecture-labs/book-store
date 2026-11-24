package com.bookstore.app;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.domain.PasswordHasher;
import com.bookstore.shared.jdbc.ModuleMigrations;

import java.lang.System.Logger.Level;
import java.time.Clock;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Prepares the database when the application starts (ADR-0010): it applies every module's
 * migrations and, if {@code bookstore.demo-data} is {@code true}, loads the demo catalog and
 * accounts. If a migration fails, the exception stops the deployment, so the application never
 * runs on a schema it does not expect.
 */
@ApplicationScoped
public class BookstoreStartup {

    private static final System.Logger LOG = System.getLogger(BookstoreStartup.class.getName());

    private final DataSource dataSource;
    private final AccountFacade accounts;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final boolean demoData;
    private volatile boolean started;

    /**
     * Creates the startup step.
     *
     * @param dataSource the application's pool
     * @param accounts   the account use cases, for the demo customer
     * @param hasher     the password hasher, for the demo administrator
     * @param clock      the clock
     * @param demoData   whether to load the demo data
     */
    @Inject
    public BookstoreStartup(DataSource dataSource, AccountFacade accounts, PasswordHasher hasher, Clock clock,
                            @ConfigProperty(name = "bookstore.demo-data", defaultValue = "false") boolean demoData) {
        this.dataSource = dataSource;
        this.accounts = accounts;
        this.hasher = hasher;
        this.clock = clock;
        this.demoData = demoData;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected BookstoreStartup() {
        this(null, null, null, null, false);
    }

    /** @return whether the migrations and the demo data are done; the readiness check asks (ADR-0022) */
    public boolean isStarted() {
        return started;
    }

    /**
     * Runs when the container has started the application.
     *
     * @param startup the container's startup event
     */
    void onStart(@Observes Startup startup) {
        int applied = ModuleMigrations.migrate(dataSource, ModuleMigrations.ALL);
        LOG.log(Level.INFO, "Book Store started; {0} migrations applied", applied);
        if (demoData) {
            DemoData.load(dataSource, accounts, hasher, clock);
            LOG.log(Level.INFO, "Demo data ready; sign in as {0} or {1} with the password {2}",
                    DemoData.CUSTOMER_EMAIL, DemoData.ADMIN_EMAIL, DemoData.PASSWORD);
        }
        started = true;
    }
}
