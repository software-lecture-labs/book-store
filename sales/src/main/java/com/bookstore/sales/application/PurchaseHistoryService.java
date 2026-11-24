package com.bookstore.sales.application;

import com.bookstore.sales.api.PurchaseHistory;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Answers other modules' questions about purchases.
 */
@ApplicationScoped
public class PurchaseHistoryService implements PurchaseHistory {

    private final OrderRepository orders;

    /**
     * Creates the service.
     *
     * @param orders where orders are stored
     */
    @Inject
    public PurchaseHistoryService(OrderRepository orders) {
        this.orders = orders;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected PurchaseHistoryService() {
        this(null);
    }

    @Override
    public boolean hasReceived(CustomerId customerId, BookId bookId) {
        return orders.hasDeliveredBook(customerId, bookId);
    }
}
