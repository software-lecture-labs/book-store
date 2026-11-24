package com.bookstore.sales.api;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/**
 * Order management for administrators (ORD-6, ORD-7, ADM-5).
 */
public interface OrderAdminFacade {

    /**
     * Returns one page of all orders, newest first.
     *
     * @param page which page to return
     * @return the page
     */
    Page<OrderSummary> orders(PageRequest page);

    /**
     * Returns one page of the orders in one status, newest first.
     *
     * @param status the status, for example {@code "SHIPPED"}
     * @param page   which page to return
     * @return the page
     * @throws ValidationException if the status is unknown
     */
    Page<OrderSummary> ordersIn(String status, PageRequest page);

    /**
     * Returns any order.
     *
     * @param orderNumber the order number
     * @return the order
     * @throws NotFoundException if there is no such order
     */
    OrderDetails order(String orderNumber);

    /**
     * Moves an order to another status. Cancelling puts the copies back into stock (ORD-7).
     *
     * @param orderNumber the order number
     * @param target      the new status, for example {@code "SHIPPED"}
     * @return the updated order
     * @throws ValidationException if the status is unknown
     * @throws ConflictException   if the change is not allowed from the current status (ORD-6)
     */
    OrderDetails changeStatus(String orderNumber, String target);

    /**
     * Returns the last 30 UTC days of settled sales for the administrator's dashboard.
     *
     * @return daily totals and book rankings
     */
    SalesReport salesReport();
}
