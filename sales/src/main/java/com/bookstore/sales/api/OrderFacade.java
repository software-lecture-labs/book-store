package com.bookstore.sales.api;

import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/**
 * A customer's own order history (ORD-8, ORD-9).
 */
public interface OrderFacade {

    /**
     * Returns one page of a customer's orders, newest first.
     *
     * @param customerId the signed-in customer
     * @param page       which page to return
     * @return the page
     */
    Page<OrderSummary> myOrders(CustomerId customerId, PageRequest page);

    /**
     * Returns one of the customer's orders.
     *
     * @param customerId  the signed-in customer
     * @param orderNumber the order number
     * @return the order
     * @throws NotFoundException     if there is no such order
     * @throws AccessDeniedException if the order belongs to someone else
     */
    OrderDetails myOrder(CustomerId customerId, String orderNumber);

    /**
     * Cancels one of the customer's orders.
     *
     * @param customerId the signed-in customer
     * @param orderNumber the order number
     * @return the cancelled order
     * @throws AccessDeniedException if the order belongs to someone else
     * @throws NotFoundException if no such order exists
     * @throws ConflictException if the order has already been shipped or delivered
     */
    OrderDetails cancel(CustomerId customerId, String orderNumber);
}
