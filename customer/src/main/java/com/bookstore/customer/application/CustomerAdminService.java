package com.bookstore.customer.application;

import com.bookstore.customer.api.CustomerAdminFacade;
import com.bookstore.customer.api.CustomerSummary;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.CustomerRepository;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The administrator's account use cases (ADM-6). No administrator can be stopped from signing in
 * here, so nobody can lock the shop's management out (ADR-0021).
 */
@ApplicationScoped
public class CustomerAdminService implements CustomerAdminFacade {

    private final CustomerRepository customers;
    private final TransactionRunner transactions;

    /**
     * Creates the service.
     *
     * @param customers    where accounts are stored
     * @param transactions runs each change in a transaction
     */
    @Inject
    public CustomerAdminService(CustomerRepository customers, TransactionRunner transactions) {
        this.customers = customers;
        this.transactions = transactions;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CustomerAdminService() {
        this(null, null);
    }

    @Override
    public Page<CustomerSummary> customers(PageRequest page) {
        return customers.findAll(page).map(CustomerMapper::summaryOf);
    }

    @Override
    public CustomerSummary setActive(CustomerId customerId, boolean active) {
        return transactions.inTransaction(() -> {
            Customer customer = customers.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Customer", customerId));
            if (customer.isAdmin()) {
                throw new RuleViolationException("customer.admin-protected",
                        "An administrator's account cannot be switched off here");
            }
            if (active) {
                customer.activate();
            } else {
                customer.deactivate();
            }
            return CustomerMapper.summaryOf(customers.save(customer));
        });
    }
}
