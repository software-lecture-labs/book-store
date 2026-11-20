package com.bookstore.app;

import com.bookstore.catalog.api.CatalogFacade;
import com.bookstore.catalog.persistence.CatalogDb;
import com.bookstore.customer.application.CustomerShop;
import com.bookstore.customer.persistence.CustomerDb;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.CheckoutFacade;
import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.sales.persistence.JdbcCartRepository;
import com.bookstore.sales.persistence.JdbcOrderRepository;
import com.bookstore.shared.jdbc.ModuleMigrations;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.event.Notifier;
import com.bookstore.shared.kernel.testing.TestClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The whole application, wired by {@link BookstoreModule} on one H2 database, for end-to-end
 * tests. Tests drive it through the modules' façades; the catalog and customer fixtures and two
 * repositories are there to set up data and to look at what was stored.
 */
final class JdbcBookstore {

    final TestDatabase database;
    final CatalogDb catalogDb;
    final CustomerDb customerDb;

    final List<DomainEvent> notified = Collections.synchronizedList(new ArrayList<>());
    final AtomicBoolean declineCards = new AtomicBoolean();
    final AtomicBoolean notificationsFail = new AtomicBoolean();
    private final Notifier notifier = event -> {
        if (notificationsFail.get()) {
            throw new IllegalStateException("Mail server unreachable");
        }
        notified.add(event);
    };
    private final PaymentGateway payments = (number, amount) -> declineCards.get()
            ? new PaymentGateway.PaymentResult.Declined("Card declined")
            : new PaymentGateway.PaymentResult.Approved("TX-" + number.value());

    final BookstoreModule module;
    final CatalogFacade catalog;
    final CartFacade cart;
    final CheckoutFacade checkout;
    final OrderAdminFacade orderAdmin;
    final ReviewFacade reviews;
    final JdbcOrderRepository orders;
    final JdbcCartRepository carts;

    /** Wires the application on a fresh H2 database. */
    JdbcBookstore() {
        this(TestDatabase.withModules(ModuleMigrations.ALL.toArray(String[]::new)));
    }

    /**
     * Wires the application on a migrated database.
     *
     * @param database H2 or PostgreSQL, with every module's tables
     */
    JdbcBookstore(TestDatabase database) {
        this.database = database;
        this.catalogDb = new CatalogDb(database);
        this.customerDb = new CustomerDb(database);
        this.module = new BookstoreModule(database.dataSource(),
                new BookstoreModule.Adapters(TestClock.CLOCK, payments, new CustomerShop.PrefixPasswordHasher(), notifier));
        this.catalog = module.catalog();
        this.cart = module.cart();
        this.checkout = module.checkout();
        this.orderAdmin = module.orderAdmin();
        this.reviews = module.reviews();
        this.orders = new JdbcOrderRepository(database.jdbc());
        this.carts = new JdbcCartRepository(database.jdbc());
    }
}
