package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stores and finds customers, including administrators.
 */
public interface CustomerRepository {

    /**
     * Finds a customer by id.
     *
     * @param id the id
     * @return the customer with their addresses, or empty
     */
    Optional<Customer> findById(CustomerId id);

    /**
     * Finds a customer by e-mail address, for signing in.
     *
     * @param email the address
     * @return the customer with their addresses, or empty
     */
    Optional<Customer> findByEmail(Email email);

    /**
     * Finds several customers at once, for example the authors of a page of reviews. Ids with no
     * customer are skipped.
     *
     * @param ids the ids
     * @return the customers found, in no particular order
     */
    List<Customer> findAllById(Collection<CustomerId> ids);

    /**
     * Tells whether an e-mail address is already registered.
     *
     * @param email the address
     * @return {@code true} if taken
     */
    boolean existsByEmail(Email email);

    /**
     * Returns one page of all customers, newest first.
     *
     * @param page which page to return
     * @return the page
     */
    Page<Customer> findAll(PageRequest page);

    /**
     * Inserts a new customer or updates a stored one, including their saved addresses.
     *
     * @param customer the customer
     * @return the same customer, with ids and versions updated
     * @throws com.bookstore.shared.kernel.error.ConflictException if the e-mail address is taken
     */
    Customer save(Customer customer);
}
