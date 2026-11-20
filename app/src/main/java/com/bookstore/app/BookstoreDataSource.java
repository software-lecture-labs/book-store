package com.bookstore.app;

import com.bookstore.shared.jdbc.ConnectionProvider;
import com.bookstore.shared.jdbc.DataSourceConnections;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * The application's own connection pool (ADR-0010). The connection details are MicroProfile Config
 * properties that Payara resolves when the application starts ({@code ${MPCONFIG=...}}): the WAR
 * carries only the in-memory H2 defaults, and a real database comes from the environment
 * ({@code BOOKSTORE_DB_URL}, {@code BOOKSTORE_DB_USER}, {@code BOOKSTORE_DB_PASSWORD}, and
 * {@code BOOKSTORE_DB_CLASS_NAME=org.postgresql.ds.PGSimpleDataSource}).
 */
@DataSourceDefinition(
        name = BookstoreDataSource.JNDI_NAME,
        className = "${MPCONFIG=bookstore.db.class-name}",
        url = "${MPCONFIG=bookstore.db.url}",
        user = "${MPCONFIG=bookstore.db.user}",
        password = "${MPCONFIG=bookstore.db.password}",
        transactional = true,
        minPoolSize = 1,
        maxPoolSize = 20)
@ApplicationScoped
public class BookstoreDataSource {

    /** Where the pool is bound. */
    public static final String JNDI_NAME = "java:app/jdbc/bookstore";

    @Resource(lookup = JNDI_NAME)
    private DataSource dataSource;

    /** @return the pool */
    @Produces
    @Singleton
    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * Returns where the repositories get their connections: from the pool, enlisted in the JTA
     * transaction of the current use case.
     *
     * @param pool the pool
     * @return the provider
     */
    @Produces
    @Singleton
    public ConnectionProvider connections(DataSource pool) {
        return new DataSourceConnections(pool);
    }
}
