package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRequest;
import com.bookstore.sales.domain.OrderRequestLog;
import com.bookstore.shared.jdbc.Columns;
import com.bookstore.shared.jdbc.DataAccessException;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Keeps idempotency keys in the {@code order_requests} table (ADR-0022). The primary key on
 * customer and key turns a second request with the same key into a conflict.
 */
@ApplicationScoped
public class JdbcOrderRequestLog implements OrderRequestLog {

    private final JdbcExecutor jdbc;

    /**
     * Creates the log.
     *
     * @param jdbc runs the statements in the caller's transaction
     */
    @Inject
    public JdbcOrderRequestLog(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcOrderRequestLog() {
        this(null);
    }

    @Override
    public Optional<OrderRequest> find(CustomerId customerId, String key) {
        return jdbc.queryOne("SELECT user_id, request_key, fingerprint, order_number, received_at FROM order_requests"
                        + " WHERE user_id = ? AND request_key = ?",
                row -> new OrderRequest(CustomerId.of(row.getLong("user_id")), row.getString("request_key"),
                        row.getString("fingerprint"), new OrderNumber(row.getString("order_number")),
                        Columns.instant(row, "received_at")),
                customerId.value(), key);
    }

    @Override
    public void record(OrderRequest request) {
        try {
            jdbc.update("INSERT INTO order_requests (user_id, request_key, fingerprint, order_number, received_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    request.customerId().value(), request.key(), request.fingerprint(),
                    request.orderNumber().value(), request.receivedAt());
        } catch (DataAccessException e) {
            if (e.violates("pk_order_requests")) {
                throw new ConflictException(DUPLICATE_KEY, "Customer " + request.customerId()
                        + " already sent the key " + request.key());
            }
            throw e;
        }
    }
}
