package com.bookstore.customer.application;

import com.bookstore.customer.api.CustomerDirectory;
import com.bookstore.customer.domain.AddressId;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Answers other modules' questions about customers.
 */
@ApplicationScoped
public class CustomerDirectoryService implements CustomerDirectory {

    private final CustomerRepository customers;

    /**
     * Creates the service.
     *
     * @param customers where customers are stored
     */
    @Inject
    public CustomerDirectoryService(CustomerRepository customers) {
        this.customers = customers;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CustomerDirectoryService() {
        this(null);
    }

    @Override
    public Address shippingAddress(CustomerId customerId, long addressId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer", customerId));
        return customer.address(AddressId.of(addressId)).address();
    }

    @Override
    public Map<CustomerId, String> publicNames(Collection<CustomerId> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return customers.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::id, customer -> customer.name().publicName()));
    }
}
