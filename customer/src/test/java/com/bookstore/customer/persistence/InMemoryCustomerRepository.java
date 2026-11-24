package com.bookstore.customer.persistence;

import com.bookstore.customer.domain.AddressId;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.SavedAddress;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.InMemoryStore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Customers in a map, with an extra index by e-mail address. Saved addresses get their own ids. */
public class InMemoryCustomerRepository extends InMemoryStore<CustomerId, Customer> implements CustomerRepository {

    private final Map<Email, CustomerId> idByEmail = new HashMap<>();
    private final AtomicLong addressSequence = new AtomicLong(1000);

    /** Creates an empty repository. */
    public InMemoryCustomerRepository() {
        super(CustomerId::new);
    }

    @Override
    protected Customer copyOf(Customer customer) {
        List<SavedAddress> addresses = customer.addresses().stream()
                .map(a -> SavedAddress.reconstitute(a.id(), a.version(), a.label(), a.address()))
                .toList();
        return Customer.reconstitute(customer.id(), customer.version(), customer.email(), customer.name(),
                customer.passwordHash(), customer.role(), customer.isActive(), customer.registeredAt(), addresses,
                customer.defaultAddress().map(SavedAddress::id).orElse(null));
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        return Optional.ofNullable(idByEmail.get(email)).flatMap(this::findById);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return idByEmail.containsKey(email);
    }

    @Override
    public Page<Customer> findAll(PageRequest page) {
        return Page.fromList(all().stream()
                .sorted(Comparator.comparing(Customer::registeredAt).thenComparing(customer -> customer.id().value()).reversed())
                .toList(), page);
    }

    @Override
    protected void beforeSave(Customer customer) {
        CustomerId owner = idByEmail.get(customer.email());
        if (owner != null && (customer.isNew() || !owner.equals(customer.id()))) {
            throw new ConflictException("email.taken", customer.email() + " is already registered");
        }
    }

    @Override
    protected void afterSave(Customer customer) {
        idByEmail.values().removeIf(id -> id.equals(customer.id()));
        idByEmail.put(customer.email(), customer.id());
        for (SavedAddress address : customer.addresses()) {
            if (address.isNew()) {
                address.markPersisted(AddressId.of(addressSequence.incrementAndGet()), 0);
            }
        }
    }
}
