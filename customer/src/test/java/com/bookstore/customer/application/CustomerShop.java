package com.bookstore.customer.application;

import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.api.RegistrationCommand;
import com.bookstore.customer.domain.PasswordHasher;
import com.bookstore.customer.persistence.InMemoryCustomerRepository;
import com.bookstore.customer.persistence.InMemoryFavoriteRepository;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.TestAddresses;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Clock;

/**
 * The customer module wired with an in-memory repository and a fake password hasher.
 */
public final class CustomerShop {

    /** A clock stopped at a fixed moment. */
    public static final Clock CLOCK = TestClock.CLOCK;

    /** A password that satisfies the password policy. */
    public static final String PASSWORD = "kitap2026";

    public final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    public final DirectTransactions transactions = new DirectTransactions();
    public final PrefixPasswordHasher passwordHasher = new PrefixPasswordHasher();
    public final InMemoryFavoriteRepository favoriteStore = new InMemoryFavoriteRepository();

    public final AccountService accounts = new AccountService(customers, passwordHasher, transactions, CLOCK);
    public final CustomerDirectoryService directory = new CustomerDirectoryService(customers);
    public final CustomerAdminService admin = new CustomerAdminService(customers, transactions);
    public final FavoriteService favorites = new FavoriteService(customers, favoriteStore, transactions);

    /** Registers a customer with one saved default address and returns the customer's id. */
    public CustomerId customer(String email) {
        CustomerView registered = accounts.register(new RegistrationCommand(email, "Ayşe", "Yılmaz", PASSWORD));
        accounts.addAddress(registered.id(), "Ev", address(), true);
        return registered.id();
    }

    /** Returns the id of a customer's default address. */
    public long defaultAddressId(CustomerId customerId) {
        return accounts.profile(customerId).addresses().stream()
                .filter(CustomerView.AddressView::isDefault)
                .findFirst()
                .orElseThrow()
                .id();
    }

    /** A valid delivery address. */
    public static Address address() {
        return TestAddresses.home();
    }

    /** "Hashes" by adding a prefix, so tests can see that the plain password was not stored. */
    public static final class PrefixPasswordHasher implements PasswordHasher {

        private static final String PREFIX = "{fake}";

        @Override
        public String hash(String rawPassword) {
            return PREFIX + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals(PREFIX + rawPassword);
        }
    }
}
