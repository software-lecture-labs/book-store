package com.bookstore.shared.jdbc;

import org.flywaydb.core.Flyway;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.sql.DataSource;

/**
 * Runs the Flyway migrations of several business modules, one module after the other.
 *
 * <p>Each module owns its migrations in {@code classpath:db/migration/<module>} and its own history
 * table {@code flyway_history_<module>} (ADR-0018). Versions therefore only have to be unique within
 * a module, and a module's schema can evolve without knowing the others' version numbers. Modules
 * are migrated in the order given; a module whose tables refer to another module's tables (for
 * example {@code sales} to {@code catalog}) must come after it.
 *
 * <p>When migrations run is decided by the composition root, not by the business modules.
 */
public final class ModuleMigrations {

    /** Modules of the book store in dependency order. */
    public static final List<String> ALL = List.of("catalog", "customer", "sales", "review");

    private static final Pattern MODULE_NAME = Pattern.compile("[a-z][a-z0-9_]*");

    private ModuleMigrations() {
    }

    /**
     * Applies every pending migration of the given modules.
     *
     * @param dataSource the database to migrate
     * @param modules    module names in dependency order, for example {@code catalog, sales}
     * @return how many migrations were applied in total
     */
    public static int migrate(DataSource dataSource, List<String> modules) {
        Objects.requireNonNull(dataSource, "dataSource");
        int applied = 0;
        for (String module : modules) {
            if (!MODULE_NAME.matcher(module).matches()) {
                throw new IllegalArgumentException("Invalid module name: " + module);
            }
            applied += Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/" + module)
                    .table("flyway_history_" + module)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate()
                    .migrationsExecuted;
        }
        return applied;
    }
}
