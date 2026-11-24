package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderRequest;
import com.bookstore.sales.domain.OrderRequestLog;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps idempotency keys in memory, for tests; a key is unique per customer, as in the database. */
public final class InMemoryOrderRequestLog implements OrderRequestLog {

    private final Map<String, OrderRequest> requests = new ConcurrentHashMap<>();

    @Override
    public Optional<OrderRequest> find(CustomerId customerId, String key) {
        return Optional.ofNullable(requests.get(idOf(customerId, key)));
    }

    @Override
    public void record(OrderRequest request) {
        if (requests.putIfAbsent(idOf(request.customerId(), request.key()), request) != null) {
            throw new ConflictException(DUPLICATE_KEY, "Customer " + request.customerId()
                    + " already sent the key " + request.key());
        }
    }

    /** @return how many keys are recorded */
    public int size() {
        return requests.size();
    }

    private static String idOf(CustomerId customerId, String key) {
        return customerId.value() + "\u0000" + key;
    }
}
