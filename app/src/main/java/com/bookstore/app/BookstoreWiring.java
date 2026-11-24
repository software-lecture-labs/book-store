package com.bookstore.app;

import com.bookstore.customer.domain.PasswordHasher;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.sales.domain.ShippingPolicy;
import com.bookstore.shared.jdbc.ConnectionProvider;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.event.Notifier;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * The objects the CDI container cannot make by itself (ADR-0006, ADR-0010): the clock, the shipping
 * rule, the JDBC executor and the adapters to the outside world. Everything else is a bean that
 * the container builds through its {@code @Inject} constructor.
 */
@ApplicationScoped
public class BookstoreWiring {

    /** @return the real time, in UTC */
    @Produces
    @Singleton
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** @return the standard shipping rule: free from a threshold, a flat fee below it */
    @Produces
    @Singleton
    public ShippingPolicy shippingPolicy() {
        return ShippingPolicy.standard();
    }

    /**
     * Returns the JDBC executor every repository uses.
     *
     * @param connections the container's connections
     * @return the executor
     */
    @Produces
    @Singleton
    public JdbcExecutor jdbcExecutor(ConnectionProvider connections) {
        return new JdbcExecutor(connections);
    }

    /** @return the password hasher: BCrypt, still accepting the PBKDF2 hashes of Phase 5 and 6 (ADR-0020) */
    @Produces
    @Singleton
    public PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher();
    }

    /** @return the payment gateway; a demo one until a payment provider is chosen */
    @Produces
    @Singleton
    public PaymentGateway paymentGateway() {
        return DemoAdapters.approvingPayments();
    }

    /** @return the notifier; it writes to the log until an e-mail service is chosen */
    @Produces
    @Singleton
    public Notifier notifier() {
        return DemoAdapters.loggingNotifier();
    }
}
