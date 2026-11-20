package com.bookstore.customer.persistence;

import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.PersonName;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.testing.TestAddresses;
import com.bookstore.shared.kernel.testing.TestClock;

/**
 * The customer module's repository on a test database (H2 or PostgreSQL) or in memory, plus a helper
 * that stores a customer. The contract tests run against all three; other modules' persistence
 * tests use it to create the users their rows refer to. {@link #jdbc} is {@code null} in memory.
 */
public final class CustomerDb {

    public final JdbcExecutor jdbc;
    public final CustomerRepository customers;

    /** Creates a database with only the customer module's tables. */
    public CustomerDb() {
        this(TestDatabase.withModules("customer"));
    }

    /**
     * Uses a database created by another module's test, which must include the customer tables.
     *
     * @param database the database
     */
    public CustomerDb(TestDatabase database) {
        jdbc = database.jdbc();
        customers = new JdbcCustomerRepository(jdbc);
    }

    private CustomerDb(CustomerRepository customers) {
        this.jdbc = null;
        this.customers = customers;
    }

    /**
     * Creates the customer module's in-memory repository.
     *
     * @return the fixture
     */
    public static CustomerDb inMemory() {
        return new CustomerDb(new InMemoryCustomerRepository());
    }

    /** Stores a customer named Ayşe Yılmaz with one default address. */
    public Customer customer(String email) {
        Customer customer = Customer.register(Email.of(email), new PersonName("Ayşe", "Yılmaz"), "{hash}pw",
                TestClock.CLOCK);
        customer.addAddress("Ev", TestAddresses.home(), true);
        return customers.save(customer);
    }
}
