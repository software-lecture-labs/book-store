package com.bookstore.sales.api;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;

/**
 * Turning a customer's cart into an order (ORD-1 – ORD-5).
 */
public interface CheckoutFacade {

    /**
     * Places an order for everything in the customer's cart.
     *
     * <p>Orders paid by card are charged at once and become {@code PAID}. Bank transfers stay
     * {@code CREATED} until an administrator records the payment (ADR-0013).
     *
     * @param command the checkout form
     * @return the new order
     * @throws CartChangedException     if the cart had to be corrected; nothing was ordered
     * @throws RuleViolationException   if the cart is empty
     * @throws ConflictException        if a book ran out of stock meanwhile
     * @throws PaymentDeclinedException if the card was refused; nothing was ordered
     * @throws NotFoundException        if the customer or the address does not exist
     */
    OrderDetails placeOrder(PlaceOrderCommand command);
}
