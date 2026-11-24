package com.bookstore.sales.persistence;

import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.persistence.CatalogDb;
import com.bookstore.customer.persistence.CustomerDb;
import com.bookstore.sales.domain.CartRepository;
import com.bookstore.sales.domain.OrderNumberSequence;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.sales.domain.OrderRequestLog;
import com.bookstore.sales.domain.ProductSnapshot;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.TestDatabase;

/**
 * The sales repositories on a test database (H2 or PostgreSQL) with the catalog, customer and
 * sales tables, or in memory. Books and users that sales rows refer to are created through the
 * other modules' fixtures. {@link #jdbc} is {@code null} in memory.
 */
public final class SalesDb {

    public final JdbcExecutor jdbc;
    public final CatalogDb catalog;
    public final CustomerDb customers;
    public final CartRepository carts;
    public final OrderRepository orders;
    public final OrderNumberSequence orderNumbers;
    public final OrderRequestLog requests;
    public final OrderEventOutbox outbox;

    /** Creates a fresh H2 database. */
    public SalesDb() {
        this(TestDatabase.withModules("catalog", "customer", "sales"));
    }

    /**
     * Uses a database that has the catalog, customer and sales tables.
     *
     * @param database the database
     */
    public SalesDb(TestDatabase database) {
        this.jdbc = database.jdbc();
        this.catalog = new CatalogDb(database);
        this.customers = new CustomerDb(database);
        this.carts = new JdbcCartRepository(jdbc);
        this.orders = new JdbcOrderRepository(jdbc);
        this.orderNumbers = new JdbcOrderNumberSequence(jdbc, database.transactions());
        this.requests = new JdbcOrderRequestLog(jdbc);
        this.outbox = new JdbcOrderEventOutbox(jdbc);
    }

    private SalesDb(CatalogDb catalog, CustomerDb customers) {
        this.jdbc = null;
        this.catalog = catalog;
        this.customers = customers;
        this.carts = new InMemoryCartRepository();
        this.orders = new InMemoryOrderRepository();
        this.orderNumbers = new InMemoryOrderNumberSequence();
        this.requests = new InMemoryOrderRequestLog();
        this.outbox = new InMemoryOrderEventOutbox();
    }

    /**
     * Creates the sales module's in-memory repositories, with in-memory catalog and customers.
     *
     * @return the fixture
     */
    public static SalesDb inMemory() {
        return new SalesDb(CatalogDb.inMemory(), CustomerDb.inMemory());
    }

    /**
     * Returns the sales module's view of a stored book.
     *
     * @param book a stored book
     * @return the snapshot
     */
    public static ProductSnapshot snapshotOf(Book book) {
        return new ProductSnapshot(book.id(), book.title(), book.authorNames(), book.price(), book.stock().quantity(),
                book.isActive());
    }
}
