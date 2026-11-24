package com.bookstore.customer.persistence;

import com.bookstore.customer.domain.AddressId;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.PersonName;
import com.bookstore.customer.domain.Role;
import com.bookstore.customer.domain.SavedAddress;
import com.bookstore.shared.jdbc.DataAccessException;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.RecordRowMapper;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Stores customers in the {@code users} table and their saved addresses in {@code addresses}.
 */
@ApplicationScoped
public class JdbcCustomerRepository implements CustomerRepository {

    private static final String USER_COLUMNS =
            "id, version, email, first_name, last_name, password_hash, role, active, registered_at";
    private static final String ADDRESS_COLUMNS = "id, version, user_id, is_default, label, recipient, phone, "
            + "address_line, district, city, postal_code";

    private static final RecordRowMapper<UserRow> USERS = RecordRowMapper.of(UserRow.class);
    private static final RecordRowMapper<AddressRow> ADDRESSES = RecordRowMapper.of(AddressRow.class);

    private final JdbcExecutor jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc runs the SQL
     */
    @Inject
    public JdbcCustomerRepository(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcCustomerRepository() {
        this(null);
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return load("SELECT " + USER_COLUMNS + " FROM users WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        return load("SELECT " + USER_COLUMNS + " FROM users WHERE email = ?", email.value()).stream().findFirst();
    }

    @Override
    public List<Customer> findAllById(Collection<CustomerId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return load("SELECT " + USER_COLUMNS + " FROM users WHERE id IN (" + JdbcExecutor.placeholders(ids.size())
                + ")", JdbcExecutor.params(ids));
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jdbc.exists("SELECT 1 FROM users WHERE email = ?", email.value());
    }

    @Override
    public Page<Customer> findAll(PageRequest page) {
        long total = jdbc.queryForLong("SELECT COUNT(*) FROM users");
        List<Customer> customers = load("SELECT " + USER_COLUMNS
                + " FROM users ORDER BY registered_at DESC, id DESC LIMIT ? OFFSET ?", page.size(), page.offset());
        return new Page<>(customers, page, total);
    }

    // ---------------------------------------------------------------- commands

    @Override
    public Customer save(Customer customer) {
        try {
            if (customer.isNew()) {
                long id = jdbc.insert("INSERT INTO users (email, first_name, last_name, password_hash, role, active, "
                                + "registered_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        customer.email().value(), customer.name().firstName(), customer.name().lastName(),
                        customer.passwordHash(), customer.role(), customer.isActive(), customer.registeredAt());
                customer.markPersisted(CustomerId.of(id), 0);
            } else {
                jdbc.updateVersioned("Customer " + customer.id(),
                        "UPDATE users SET email = ?, first_name = ?, last_name = ?, password_hash = ?, active = ?, "
                                + "version = version + 1 WHERE id = ? AND version = ?",
                        customer.email().value(), customer.name().firstName(), customer.name().lastName(),
                        customer.passwordHash(), customer.isActive(), customer.id(), customer.version());
                customer.markPersisted(customer.id(), customer.version() + 1);
            }
        } catch (DataAccessException e) {
            if (e.violates("uq_users_email")) {
                throw new ConflictException("email.taken", "The e-mail address " + customer.email()
                        + " is already registered");
            }
            throw e;
        }
        saveAddresses(customer);
        return customer;
    }

    /** Brings the stored addresses in line with the customer's: deletes removed ones, updates and inserts. */
    private void saveAddresses(Customer customer) {
        Set<Long> kept = new HashSet<>();
        customer.addresses().stream().filter(a -> !a.isNew()).forEach(a -> kept.add(a.id().value()));
        List<Long> stored = jdbc.query("SELECT id FROM addresses WHERE user_id = ?", row -> row.getLong(1),
                customer.id());
        for (long id : stored) {
            if (!kept.contains(id)) {
                jdbc.update("DELETE FROM addresses WHERE id = ?", id);
            }
        }

        int order = 0;
        for (SavedAddress saved : customer.addresses()) {
            Address a = saved.address();
            boolean isDefault = customer.defaultAddress().map(saved::equals).orElse(false);
            if (saved.isNew()) {
                long id = jdbc.insert("INSERT INTO addresses (user_id, sort_order, is_default, label, recipient, phone, "
                                + "address_line, district, city, postal_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        customer.id(), order, isDefault, saved.label(), a.recipient(), a.phone(), a.line(),
                        a.district(), a.city(), a.postalCode());
                saved.markPersisted(AddressId.of(id), 0);
            } else {
                jdbc.update("UPDATE addresses SET sort_order = ?, is_default = ?, label = ?, recipient = ?, phone = ?, "
                                + "address_line = ?, district = ?, city = ?, postal_code = ?, version = version + 1 "
                                + "WHERE id = ?",
                        order, isDefault, saved.label(), a.recipient(), a.phone(), a.line(),
                        a.district(), a.city(), a.postalCode(), saved.id());
                saved.markPersisted(saved.id(), saved.version() + 1);
            }
            order++;
        }
    }

    // ---------------------------------------------------------------- loading

    private List<Customer> load(String sql, Object... params) {
        List<UserRow> users = jdbc.query(sql, USERS, params);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> ids = users.stream().map(UserRow::id).toList();
        Map<Long, List<AddressRow>> addresses = jdbc.query("SELECT " + ADDRESS_COLUMNS + " FROM addresses "
                                + "WHERE user_id IN (" + JdbcExecutor.placeholders(ids.size()) + ") ORDER BY user_id, sort_order",
                        ADDRESSES, JdbcExecutor.params(ids))
                .stream()
                .collect(Collectors.groupingBy(AddressRow::userId));
        return users.stream().map(user -> user.toCustomer(addresses.getOrDefault(user.id(), List.of()))).toList();
    }

    /** One {@code users} row, read by reflection. */
    private record UserRow(long id, int version, String email, String firstName, String lastName,
                           String passwordHash, Role role, boolean active, Instant registeredAt) {

        Customer toCustomer(List<AddressRow> addressRows) {
            List<SavedAddress> addresses = addressRows.stream().map(AddressRow::toSavedAddress).toList();
            AddressId defaultId = addressRows.stream().filter(AddressRow::isDefault).map(row -> AddressId.of(row.id()))
                    .findFirst().orElse(null);
            return Customer.reconstitute(CustomerId.of(id), version, Email.of(email), new PersonName(firstName, lastName), passwordHash,
                    role, active, registeredAt, addresses, defaultId);
        }
    }

    /** One {@code addresses} row, read by reflection. */
    private record AddressRow(long id, int version, long userId, boolean isDefault, String label, String recipient,
                              String phone, String addressLine, String district, String city, String postalCode) {

        SavedAddress toSavedAddress() {
            return SavedAddress.reconstitute(AddressId.of(id), version, label,
                    new Address(recipient, phone, addressLine, district, city, postalCode));
        }
    }
}
