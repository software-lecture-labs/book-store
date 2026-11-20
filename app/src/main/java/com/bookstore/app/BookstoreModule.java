package com.bookstore.app;

import com.bookstore.catalog.api.CatalogAdminFacade;
import com.bookstore.catalog.api.CatalogFacade;
import com.bookstore.catalog.application.CatalogAdminService;
import com.bookstore.catalog.application.CatalogService;
import com.bookstore.catalog.application.RatingService;
import com.bookstore.catalog.application.StockService;
import com.bookstore.catalog.domain.FileStorage;
import com.bookstore.catalog.persistence.FileSystemFileStorage;
import com.bookstore.catalog.persistence.JdbcAuthorRepository;
import com.bookstore.catalog.persistence.JdbcBookRepository;
import com.bookstore.catalog.persistence.JdbcCategoryRepository;
import com.bookstore.customer.api.AccountFacade;
import com.bookstore.customer.api.CustomerAdminFacade;
import com.bookstore.customer.application.AccountService;
import com.bookstore.customer.application.CustomerAdminService;
import com.bookstore.customer.application.CustomerDirectoryService;
import com.bookstore.customer.domain.PasswordHasher;
import com.bookstore.customer.persistence.JdbcCustomerRepository;
import com.bookstore.review.api.ReviewAdminFacade;
import com.bookstore.review.api.ReviewFacade;
import com.bookstore.review.application.ReviewAdminService;
import com.bookstore.review.application.ReviewService;
import com.bookstore.review.persistence.JdbcReviewRepository;
import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.CheckoutFacade;
import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.api.OrderFacade;
import com.bookstore.sales.application.CartService;
import com.bookstore.sales.application.CheckoutService;
import com.bookstore.sales.application.OrderAdminService;
import com.bookstore.sales.application.OrderEventRelay;
import com.bookstore.sales.application.OrderNotificationObserver;
import com.bookstore.sales.application.OrderService;
import com.bookstore.sales.application.PurchaseHistoryService;
import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.sales.domain.ShippingPolicy;
import com.bookstore.sales.persistence.JdbcCartRepository;
import com.bookstore.sales.persistence.JdbcOrderEventOutbox;
import com.bookstore.sales.persistence.JdbcOrderNumberSequence;
import com.bookstore.sales.persistence.JdbcOrderRepository;
import com.bookstore.sales.persistence.JdbcOrderRequestLog;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.JdbcTransactionRunner;
import com.bookstore.shared.jdbc.ModuleMigrations;
import com.bookstore.shared.kernel.event.InProcessEventPublisher;
import com.bookstore.shared.kernel.event.Notifier;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * The composition root (ADR-0006): the one place that decides which implementation stands behind
 * each interface, and wires the modules together by hand. Everything outside it sees only the
 * façades of the modules' {@code api} packages.
 *
 * <p>The caller supplies what the application cannot make by itself: the database, the clock and
 * the adapters to the outside world. In Phase 6 CDI takes over this job and {@code BookstoreWiring}
 * produces only these few objects (ADR-0010).
 *
 * <pre>{@code
 * BookstoreModule bookstore = new BookstoreModule(dataSource, new Adapters(Clock.systemUTC(),
 *         paymentGateway, passwordHasher, notifier));
 * bookstore.migrate();
 * bookstore.catalog().search(BookSearchQuery.everything(), PageRequest.first());
 * }</pre>
 */
public final class BookstoreModule {

    private final DataSource dataSource;

    private final CatalogFacade catalog;
    private final CatalogAdminFacade catalogAdmin;
    private final AccountFacade accounts;
    private final CartFacade cart;
    private final CheckoutFacade checkout;
    private final OrderFacade orders;
    private final OrderAdminFacade orderAdmin;
    private final OrderEventRelay relay;
    private final ReviewFacade reviews;
    private final CustomerAdminFacade customerAdmin;
    private final ReviewAdminFacade reviewAdmin;

    /**
     * Wires the application.
     *
     * @param dataSource the database
     * @param adapters   the clock and the adapters to the outside world
     */
    public BookstoreModule(DataSource dataSource, Adapters adapters) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(adapters, "adapters");
        JdbcTransactionRunner transactions = new JdbcTransactionRunner(dataSource);
        JdbcExecutor jdbc = new JdbcExecutor(transactions);
        ShippingPolicy shipping = ShippingPolicy.standard();
        InProcessEventPublisher events = new InProcessEventPublisher();
        events.subscribe(OrderEvent.class, new OrderNotificationObserver(adapters.notifier()));

        // catalog
        JdbcCategoryRepository categoryRepository = new JdbcCategoryRepository(jdbc);
        JdbcBookRepository bookRepository = new JdbcBookRepository(jdbc, categoryRepository);
        StockService stock = new StockService(bookRepository, transactions);
        RatingService ratings = new RatingService(bookRepository, transactions);
        this.catalog = new CatalogService(bookRepository, categoryRepository, adapters.files());
        this.catalogAdmin = new CatalogAdminService(bookRepository, new JdbcAuthorRepository(jdbc), categoryRepository,
                transactions, adapters.files());

        // customer
        JdbcCustomerRepository customerRepository = new JdbcCustomerRepository(jdbc);
        CustomerDirectoryService directory = new CustomerDirectoryService(customerRepository);
        this.customerAdmin = new CustomerAdminService(customerRepository, transactions);
        this.accounts = new AccountService(customerRepository, adapters.passwordHasher(), transactions,
                adapters.clock());

        // sales
        JdbcCartRepository cartRepository = new JdbcCartRepository(jdbc);
        JdbcOrderRepository orderRepository = new JdbcOrderRepository(jdbc);
        JdbcOrderEventOutbox outbox = new JdbcOrderEventOutbox(jdbc);
        this.relay = new OrderEventRelay(outbox, events, transactions, adapters.clock());
        this.cart = new CartService(stock, cartRepository, shipping, transactions);
        this.checkout = new CheckoutService(directory, cartRepository, stock, orderRepository,
                new JdbcOrderNumberSequence(jdbc, transactions), new JdbcOrderRequestLog(jdbc), adapters.paymentGateway(),
                shipping, transactions, outbox, relay, adapters.clock());
        this.orders = new OrderService(orderRepository);
        this.orderAdmin = new OrderAdminService(orderRepository, stock, transactions, outbox, relay, adapters.clock());
        PurchaseHistoryService purchases = new PurchaseHistoryService(orderRepository);

        // review
        JdbcReviewRepository reviewRepository = new JdbcReviewRepository(jdbc);
        this.reviewAdmin = new ReviewAdminService(reviewRepository, ratings, directory, transactions);
        this.reviews = new ReviewService(reviewRepository, ratings, purchases, directory,
                transactions, adapters.clock());
    }

    /**
     * Brings the database schema up to date, module by module (ADR-0018).
     *
     * @return the number of migrations applied
     */
    public int migrate() {
        return ModuleMigrations.migrate(dataSource, ModuleMigrations.ALL);
    }

    /** @return browsing and searching the catalog */
    public CatalogFacade catalog() {
        return catalog;
    }

    /** @return managing books, authors and categories */
    public CatalogAdminFacade catalogAdmin() {
        return catalogAdmin;
    }

    /** @return registration, sign-in data, profile and address book */
    public AccountFacade accounts() {
        return accounts;
    }

    /** @return visitors' and customers' carts */
    public CartFacade cart() {
        return cart;
    }

    /** @return placing orders */
    public CheckoutFacade checkout() {
        return checkout;
    }

    /** @return a customer's own orders */
    public OrderFacade orders() {
        return orders;
    }

    /** @return order management for administrators */
    public OrderAdminFacade orderAdmin() {
        return orderAdmin;
    }

    /** @return the relay of order events from the outbox; tests run it as the server's timer would */
    public OrderEventRelay orderEventRelay() {
        return relay;
    }

    /** @return managing user accounts */
    public CustomerAdminFacade customerAdmin() {
        return customerAdmin;
    }

    /** @return moderating reviews */
    public ReviewAdminFacade reviewAdmin() {
        return reviewAdmin;
    }

    /** @return reviews and ratings */
    public ReviewFacade reviews() {
        return reviews;
    }

    /**
     * What the application needs from outside and cannot make by itself.
     *
     * @param clock          the source of the current time
     * @param paymentGateway charges cards
     * @param passwordHasher hashes and checks passwords
     * @param notifier       tells customers about their orders; {@link OrderNotificationObserver} feeds it
     * @param files          keeps uploaded cover images
     */
    public record Adapters(Clock clock, PaymentGateway paymentGateway, PasswordHasher passwordHasher,
                           Notifier notifier, FileStorage files) {

        /** Rejects missing adapters. */
        public Adapters {
            Objects.requireNonNull(clock, "clock");
            Objects.requireNonNull(paymentGateway, "paymentGateway");
            Objects.requireNonNull(passwordHasher, "passwordHasher");
            Objects.requireNonNull(notifier, "notifier");
            Objects.requireNonNull(files, "files");
        }

        /**
         * Uses a directory under the system's temporary directory for uploaded files.
         *
         * @param clock          the clock
         * @param paymentGateway the payment gateway
         * @param passwordHasher the password hasher
         * @param notifier       the notifier
         */
        public Adapters(Clock clock, PaymentGateway paymentGateway, PasswordHasher passwordHasher, Notifier notifier) {
            this(clock, paymentGateway, passwordHasher, notifier, new FileSystemFileStorage(
                    Path.of(System.getProperty("java.io.tmpdir"), "bookstore-files")));
        }
    }
}
