package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.time.Instant;
import java.util.Optional;

/**
 * Stores and finds orders.
 */
public interface OrderRepository {

    /**
     * Finds an order by id.
     *
     * @param id the id
     * @return the order with its lines, or empty
     */
    Optional<Order> findById(OrderId id);

    /**
     * Finds an order by its number.
     *
     * @param number the order number
     * @return the order with its lines, or empty
     */
    Optional<Order> findByNumber(OrderNumber number);

    /**
     * Returns one page of a customer's orders, newest first.
     *
     * @param customerId the customer's id
     * @param page       which page to return
     * @return the page
     */
    Page<Order> findByCustomerId(CustomerId customerId, PageRequest page);

    /**
     * Returns one page of all orders, newest first.
     *
     * @param page which page to return
     * @return the page
     */
    Page<Order> findAll(PageRequest page);

    /**
     * Returns one page of the orders in a status, newest first.
     *
     * @param status the status
     * @param page   which page to return
     * @return the page
     */
    Page<Order> findByStatus(OrderStatus status, PageRequest page);

    /**
     * Aggregates paid, shipped and delivered orders in a half-open time range.
     *
     * @param fromInclusive the first instant
     * @param untilExclusive the first instant not included
     * @return daily totals and the five highest- and lowest-volume books with sales
     */
    SalesReportData salesReport(Instant fromInclusive, Instant untilExclusive);

    /**
     * Tells whether a customer has received a book, which is required before reviewing it.
     *
     * @param customerId the customer's id
     * @param bookId     the book's id
     * @return {@code true} if a {@link OrderStatus#DELIVERED delivered} order of the customer
     *         contains the book
     */
    boolean hasDeliveredBook(CustomerId customerId, BookId bookId);

    /**
     * Inserts a new order or updates a stored one's status.
     *
     * @param order the order
     * @return the same order, with its id and version updated
     * @throws com.bookstore.shared.kernel.error.ConflictException if the order was changed by someone else
     *                                                      in the meantime
     */
    Order save(Order order);
}
