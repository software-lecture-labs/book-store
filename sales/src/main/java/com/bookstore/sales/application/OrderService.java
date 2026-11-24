package com.bookstore.sales.application;

import com.bookstore.catalog.api.StockFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderFacade;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A customer's own orders (ORD-8).
 */
@ApplicationScoped
public class OrderService implements OrderFacade {

    private final OrderRepository orders;
    private final StockFacade stock;
    private final TransactionRunner transactions;
    private final OrderEventOutbox outbox;
    private final OrderEventRelay relay;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param orders where orders are stored
     */
    @Inject
    public OrderService(OrderRepository orders, StockFacade stock, TransactionRunner transactions,
                        OrderEventOutbox outbox, OrderEventRelay relay, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.transactions = transactions;
        this.outbox = outbox;
        this.relay = relay;
        this.clock = clock;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderService() {
        this(null, null, null, null, null, null);
    }

    /**
     * Returns one page of a customer's orders, newest first.
     *
     * @param customerId the signed-in customer
     * @param page       which page to return
     * @return the page
     */
    @Override
    public Page<OrderSummary> myOrders(CustomerId customerId, PageRequest page) {
        return orders.findByCustomerId(customerId, page).map(SalesMapper::summaryOf);
    }

    /**
     * Returns one of the customer's orders.
     *
     * @param customerId  the signed-in customer
     * @param orderNumber the order number
     * @return the order
     * @throws NotFoundException     if there is no such order
     * @throws AccessDeniedException if the order belongs to someone else
     */
    @Override
    public OrderDetails myOrder(CustomerId customerId, String orderNumber) {
        OrderNumber number = new OrderNumber(orderNumber);
        Order order = orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Order", number));
        if (!order.isOwnedBy(customerId)) {
            throw new AccessDeniedException("Order " + number + " belongs to another customer");
        }
        return SalesMapper.detailsOf(order);
    }

    @Override
    public OrderDetails cancel(CustomerId customerId, String orderNumber) {
        OrderNumber number = new OrderNumber(orderNumber);
        Order cancelled = transactions.inTransaction(() -> {
            Order order = orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Order", number));
            if (!order.isOwnedBy(customerId)) {
                throw new AccessDeniedException("Order " + number + " belongs to another customer");
            }
            order.cancel(clock);
            stock.release(order.quantitiesByBook());
            Order saved = orders.save(order);
            outbox.append(order.pullEvents());
            return saved;
        });
        relay.relayQuietly();
        return SalesMapper.detailsOf(cancelled);
    }
}
