package com.bookstore.sales.application;

import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderFacade;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A customer's own orders (ORD-8).
 */
@ApplicationScoped
public class OrderService implements OrderFacade {

    private final OrderRepository orders;

    /**
     * Creates the service.
     *
     * @param orders where orders are stored
     */
    @Inject
    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderService() {
        this(null);
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
}
