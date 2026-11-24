package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Optional;

/**
 * Remembers which order each {@code Idempotency-Key} led to (ADR-0022). A key is recorded in the
 * same transaction as its order, before the payment, so a repeat that arrives while the first
 * request is still running waits for it and then finds its order.
 */
public interface OrderRequestLog {

    /** The code of a key that is already recorded for the customer. */
    String DUPLICATE_KEY = "order.request-duplicate";

    /**
     * Finds an earlier request.
     *
     * @param customerId who sent it
     * @param key        the key
     * @return the request, if the customer used the key before
     */
    Optional<OrderRequest> find(CustomerId customerId, String key);

    /**
     * Records a request.
     *
     * @param request the request
     * @throws ConflictException {@link #DUPLICATE_KEY} if the customer already used the key
     */
    void record(OrderRequest request);
}
