package com.bookstore.customer.api;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Collection;
import java.util.Map;

/**
 * What other modules may ask about customers (ADR-0003: a business module uses another only
 * through its {@code api} package). The sales module needs delivery addresses; the review module
 * needs the names shown next to reviews.
 */
public interface CustomerDirectory {

    /**
     * Returns one of a customer's saved addresses, to be copied onto an order.
     *
     * @param customerId the customer's id
     * @param addressId  the saved address's id
     * @return the address
     * @throws com.bookstore.shared.kernel.error.NotFoundException if there is no such customer, or
     *                                                             the address belongs to someone else
     */
    Address shippingAddress(CustomerId customerId, long addressId);

    /**
     * Returns the public names of several customers, for example {@code "Ayşe Y."}. Ids with no
     * customer are left out.
     *
     * @param customerIds the customers' ids
     * @return the names keyed by customer id
     */
    Map<CustomerId, String> publicNames(Collection<CustomerId> customerIds);
}
