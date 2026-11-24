package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.api.CatalogFacade;
import com.bookstore.catalog.application.CatalogAdminService;
import com.bookstore.catalog.application.CatalogService;
import com.bookstore.catalog.application.RatingService;
import com.bookstore.catalog.application.StockService;
import com.bookstore.catalog.domain.FileStorage;
import com.bookstore.catalog.persistence.InMemoryFileStorage;
import com.bookstore.catalog.persistence.JdbcAuthorRepository;
import com.bookstore.catalog.persistence.JdbcBookRepository;
import com.bookstore.catalog.persistence.JdbcCategoryRepository;
import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.api.CustomerAdminFacade;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.application.AccountService;
import com.bookstore.customer.application.CustomerAdminService;
import com.bookstore.customer.application.CustomerDirectoryService;
import com.bookstore.customer.persistence.JdbcCustomerRepository;
import com.bookstore.review.api.ReviewAdminFacade;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.review.application.ReviewAdminService;
import com.bookstore.review.application.ReviewService;
import com.bookstore.review.persistence.JdbcReviewRepository;
import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.CheckoutFacade;
import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.application.CartService;
import com.bookstore.sales.application.CheckoutService;
import com.bookstore.sales.application.OrderAdminService;
import com.bookstore.sales.application.OrderEventRelay;
import com.bookstore.sales.application.OrderNotificationObserver;
import com.bookstore.sales.application.OrderService;
import com.bookstore.sales.application.PurchaseHistoryService;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.persistence.JdbcCartRepository;
import com.bookstore.sales.persistence.JdbcOrderNumberSequence;
import com.bookstore.sales.persistence.JdbcOrderEventOutbox;
import com.bookstore.sales.persistence.JdbcOrderRepository;
import com.bookstore.sales.persistence.JdbcOrderRequestLog;
import com.bookstore.shared.jdbc.ConnectionProvider;
import com.bookstore.shared.jdbc.JdbcTransactionRunner;
import com.bookstore.shared.jdbc.ModuleMigrations;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.event.Notifier;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The CDI wiring of ADR-0010 in a real CDI container (Weld SE), without an application server:
 * every bean's constructor dependencies resolve without ambiguity, the use cases work through
 * their façades, and the order notification observer receives the CDI events. Only the two
 * server-only parts are replaced: the JNDI data source and the JTA transaction runner.
 */
class CdiWiringTest {

    private static final List<Class<?>> APPLICATION_BEANS = List.of(
            CatalogService.class, CatalogAdminService.class, StockService.class, RatingService.class,
            JdbcBookRepository.class, JdbcAuthorRepository.class, JdbcCategoryRepository.class,
            AccountService.class, CustomerAdminService.class, CustomerDirectoryService.class,
            JdbcCustomerRepository.class,
            CartService.class, CheckoutService.class, OrderService.class, OrderAdminService.class,
            PurchaseHistoryService.class, OrderNotificationObserver.class,
            JdbcCartRepository.class, JdbcOrderRepository.class, JdbcOrderNumberSequence.class, JdbcOrderRequestLog.class,
            JdbcOrderEventOutbox.class, OrderEventRelay.class,
            ReviewService.class, ReviewAdminService.class, JdbcReviewRepository.class,
            BookstoreWiring.class, CdiDomainEventPublisher.class);

    private final WeldContainer container = new Weld()
            .disableDiscovery()
            .addBeanClasses(APPLICATION_BEANS.toArray(Class<?>[]::new))
            .addBeanClasses(TestPlatform.class)
            .initialize();

    @AfterEach
    void stop() {
        container.close();
    }

    @Test
    void everyFacadeResolvesAndTheUseCasesWorkTogether() {
        DataSource dataSource = container.select(DataSource.class).get();
        AccountFacade accounts = container.select(AccountFacade.class).get();
        DemoData.load(dataSource, accounts, new Pbkdf2PasswordHasher(1_000), container.select(Clock.class).get());

        List<BookSummary> dune = container.select(CatalogFacade.class).get()
                .search(BookSearchQuery.everything().withText("dune"), PageRequest.first())
                .content();
        CustomerView ayse = accounts.findLoginAccount(DemoData.CUSTOMER_EMAIL)
                .map(login -> accounts.profile(login.customerId()))
                .orElseThrow();
        container.select(CartFacade.class).get().add(CartRef.customer(ayse.id()), dune.getFirst().id(), 1);
        OrderDetails order = container.select(CheckoutFacade.class).get().placeOrder(
                new PlaceOrderCommand(ayse.id(), ayse.addresses().getFirst().id(), "CREDIT_CARD"));

        assertThat(order.status()).isEqualTo("PAID");
        assertThat(TestPlatform.NOTIFIED).hasAtLeastOneElementOfType(OrderPlaced.class);
        assertThat(container.select(OrderAdminFacade.class).get().order(order.orderNumber()).total())
                .isEqualTo(order.total());
        assertThat(container.select(ReviewFacade.class).get().canReview(ayse.id(), dune.getFirst().id())).isFalse();
        CatalogAdminFacade catalogAdmin = container.select(CatalogAdminFacade.class).get();
        assertThat(catalogAdmin.book(dune.getFirst().id()).stock())
                .isEqualTo(container.select(CatalogFacade.class).get().book(dune.getFirst().id()).stock());
        assertThat(catalogAdmin.stats(5).totalBooks()).isEqualTo(8);
        assertThat(container.select(CustomerAdminFacade.class).get().customers(PageRequest.first()).totalElements())
                .isEqualTo(2);
        assertThat(container.select(ReviewAdminFacade.class).get().reviews(PageRequest.first()).content()).isEmpty();
    }

    /** The test's stand-ins for the server: an H2 data source, JDBC transactions, a recording notifier, files in memory. */
    @ApplicationScoped
    public static class TestPlatform {

        static final List<DomainEvent> NOTIFIED = new CopyOnWriteArrayList<>();

        private final TestDatabase database = TestDatabase.withModules(ModuleMigrations.ALL.toArray(String[]::new));

        /** @return the H2 database */
        @Produces
        @Singleton
        public DataSource dataSource() {
            return database.dataSource();
        }

        /** @return JDBC transactions instead of JTA */
        @Produces
        @Singleton
        @Typed(JdbcTransactionRunner.class)
        public JdbcTransactionRunner transactions() {
            NOTIFIED.clear();
            return database.transactions();
        }

        /** @param runner the runner @return the runner, as the transaction port */
        @Produces
        @Singleton
        public TransactionRunner transactionRunner(JdbcTransactionRunner runner) {
            return runner;
        }

        /** @param runner the runner @return the runner, as the repositories' connection source */
        @Produces
        @Singleton
        public ConnectionProvider connections(JdbcTransactionRunner runner) {
            return runner;
        }

        /** @return a notifier that remembers what it was told */
        @Produces
        @Singleton
        @Alternative
        @Priority(1)
        public Notifier recordingNotifier() {
            return NOTIFIED::add;
        }

        /** @return files kept in memory instead of the configured directory */
        @Produces
        @Singleton
        public FileStorage files() {
            return new InMemoryFileStorage();
        }
    }
}
