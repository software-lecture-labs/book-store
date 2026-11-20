package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.OrderStatusChanged;
import com.bookstore.shared.jdbc.Columns;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Keeps order events in the {@code order_outbox} table (ADR-0022). Each event type has its own
 * columns; the type code is checked against the {@code order_event_types} reference table. Waiting
 * rows are locked while a relay hands them on, so several servers can relay at once.
 */
@ApplicationScoped
public class JdbcOrderEventOutbox implements OrderEventOutbox {

    /** The type code of {@link OrderPlaced}. */
    static final String ORDER_PLACED = "ORDER_PLACED";

    /** The type code of {@link OrderStatusChanged}. */
    static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    /** Every type code; the reference table holds the same. */
    static final Set<String> EVENT_TYPES = Set.of(ORDER_PLACED, ORDER_STATUS_CHANGED);

    private static final String COLUMNS =
            "id, event_type, order_number, user_id, total, currency, from_status, to_status, occurred_at";

    private final JdbcExecutor jdbc;

    /**
     * Creates the outbox.
     *
     * @param jdbc runs the statements in the caller's transaction
     */
    @Inject
    public JdbcOrderEventOutbox(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcOrderEventOutbox() {
        this(null);
    }

    @Override
    public void append(List<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            switch (event) {
                case OrderPlaced placed -> jdbc.update("INSERT INTO order_outbox (event_type, order_number, user_id,"
                                + " total, currency, occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                        ORDER_PLACED, placed.orderNumber().value(), customerOf(placed.customerId()),
                        placed.total().amount(), placed.total().currency().getCurrencyCode(), placed.occurredAt());
                case OrderStatusChanged changed -> jdbc.update("INSERT INTO order_outbox (event_type, order_number,"
                                + " user_id, from_status, to_status, occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                        ORDER_STATUS_CHANGED, changed.orderNumber().value(), customerOf(changed.customerId()),
                        changed.from().name(), changed.to().name(), changed.occurredAt());
                default -> throw new IllegalArgumentException("Not an order event: " + event);
            }
        }
    }

    @Override
    public List<Pending> pending(int max) {
        // Locks the rows until the caller's transaction ends and skips those another relay holds, so
        // relays on several servers never take the same event (PostgreSQL; H2 2.2 and later too).
        return jdbc.query("SELECT " + COLUMNS + " FROM order_outbox WHERE published_at IS NULL ORDER BY id LIMIT ?"
                + " FOR UPDATE SKIP LOCKED", JdbcOrderEventOutbox::pendingOf, max);
    }

    @Override
    public void markPublished(long id, Instant publishedAt) {
        jdbc.update("UPDATE order_outbox SET published_at = ? WHERE id = ?", publishedAt, id);
    }

    private static Long customerOf(CustomerId customerId) {
        return customerId == null ? null : customerId.value();
    }

    private static Pending pendingOf(ResultSet row) throws SQLException {
        OrderNumber number = new OrderNumber(row.getString("order_number").strip());
        long user = row.getLong("user_id");
        CustomerId customer = row.wasNull() ? null : CustomerId.of(user);
        Instant occurredAt = Columns.instant(row, "occurred_at");
        String type = row.getString("event_type");
        OrderEvent event = switch (type) {
            case ORDER_PLACED -> {
                BigDecimal total = row.getBigDecimal("total");
                yield new OrderPlaced(number, customer,
                        new Money(total, Currency.getInstance(row.getString("currency").strip())), occurredAt);
            }
            case ORDER_STATUS_CHANGED -> new OrderStatusChanged(number, customer,
                    OrderStatus.valueOf(row.getString("from_status")), OrderStatus.valueOf(row.getString("to_status")),
                    occurredAt);
            default -> throw new IllegalStateException("Unknown order event type " + type);
        };
        return new Pending(row.getLong("id"), event);
    }
}
