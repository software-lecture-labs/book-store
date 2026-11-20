package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A confirmed purchase.
 *
 * <p>An order is created from a cart with {@link #place}, which checks that every book can still be
 * sold, copies titles and prices and works out the totals. Taking the copies out of stock is the
 * catalog's job; the application layer asks for it in the same transaction. After that only the
 * order's {@link OrderStatus status} changes, following the allowed transitions.
 */
public class Order extends Entity<OrderId> {

    /** Error code for checking out an empty cart. */
    public static final String EMPTY_CART = "cart.empty";

    private final OrderNumber number;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Address shippingAddress;
    private final PaymentMethod paymentMethod;
    private final Money subtotal;
    private final Money shippingCost;
    private OrderStatus status;
    private final Instant placedAt;
    private Instant statusChangedAt;

    private Order(OrderNumber number, CustomerId customerId, List<OrderLine> lines, Address shippingAddress,
                  PaymentMethod paymentMethod, Money shippingCost, OrderStatus status, Instant placedAt,
                  Instant statusChangedAt) {
        this.number = Guard.requireNonNull(number, "orderNumber");
        Guard.requireNonNull(customerId, "customerId");
        this.customerId = customerId;
        this.lines = List.copyOf(Guard.requireNotEmpty(lines, "lines"));
        this.shippingAddress = Guard.requireNonNull(shippingAddress, "shippingAddress");
        this.paymentMethod = Guard.requireNonNull(paymentMethod, "paymentMethod");
        this.shippingCost = Guard.requireNonNull(shippingCost, "shippingCost");
        Guard.require(!shippingCost.isNegative(), "shippingCost", "must not be negative");
        this.status = Guard.requireNonNull(status, "status");
        this.placedAt = Guard.requireNonNull(placedAt, "placedAt");
        this.statusChangedAt = Guard.requireNonNull(statusChangedAt, "statusChangedAt");
        this.subtotal = Money.sum(this.lines.stream().map(OrderLine::lineTotal));
    }

    /**
     * Places an order for everything in a customer's cart.
     *
     * <p>The caller stores the order, takes the copies out of stock and empties the cart in one
     * transaction.
     *
     * @param number          the new order's number
     * @param customerId      the customer placing the order
     * @param cart            the customer's cart, not empty
     * @param products        every book in the cart as currently offered, keyed by id
     * @param shippingAddress where to deliver
     * @param paymentMethod   how the customer pays
     * @param shippingPolicy  the rule for the shipping cost
     * @param clock           the clock giving the order time
     * @return the order, not yet stored, with an {@link OrderPlaced} event raised
     * @throws ConflictException      if the cart is empty or a book is no longer sold
     * @throws StockShortageException if a book does not have enough stock
     * @throws AccessDeniedException  if the cart belongs to someone else
     */
    public static Order place(OrderNumber number, CustomerId customerId, Cart cart, Map<BookId, ProductSnapshot> products,
                              Address shippingAddress, PaymentMethod paymentMethod,
                              ShippingPolicy shippingPolicy, Clock clock) {
        Guard.requireNonNull(cart, "cart");
        if (cart.isEmpty()) {
            throw new RuleViolationException(EMPTY_CART, "Cannot place an order with an empty cart");
        }
        if (cart.customerId().map(owner -> !owner.equals(customerId)).orElse(true)) {
            throw new AccessDeniedException("The cart does not belong to customer " + customerId);
        }

        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : cart) {
            ProductSnapshot product = products.get(item.bookId());
            if (product == null) {
                throw new NotFoundException("Book", item.bookId());
            }
            requireSellable(product, item.quantity());
            lines.add(OrderLine.of(product, item.quantity()));
        }

        Money subtotal = Money.sum(lines.stream().map(OrderLine::lineTotal));
        Money shippingCost = Guard.requireNonNull(shippingPolicy, "shippingPolicy").costFor(subtotal);
        Instant now = Instant.now(clock);
        Order order = new Order(number, customerId, lines, shippingAddress, paymentMethod, shippingCost,
                OrderStatus.CREATED, now, now);
        order.raise(new OrderPlaced(number, customerId, order.total(), now));
        return order;
    }

    /**
     * Reconstitutes an order loaded from storage.
     *
     * @param id              the stored id
     * @param version         the stored version
     * @param number          the order number
     * @param customerId      the owner
     * @param lines           the stored lines
     * @param shippingAddress the delivery address
     * @param paymentMethod   the payment method
     * @param shippingCost    the shipping cost charged
     * @param status          the current status
     * @param placedAt        when the order was placed
     * @param statusChangedAt when the status last changed
     * @return the order
     */
    public static Order reconstitute(OrderId id, int version, OrderNumber number, CustomerId customerId, List<OrderLine> lines,
                                Address shippingAddress, PaymentMethod paymentMethod, Money shippingCost,
                                OrderStatus status, Instant placedAt, Instant statusChangedAt) {
        Order order = new Order(number, customerId, lines, shippingAddress, paymentMethod, shippingCost, status,
                placedAt, statusChangedAt);
        order.markPersisted(id, version);
        return order;
    }

    // ---------------------------------------------------------------- queries

    /** @return the order number */
    public OrderNumber number() {
        return number;
    }

    /** @return the id of the customer who placed the order */
    public CustomerId customerId() {
        return customerId;
    }

    /** @return the lines, in cart order */
    public List<OrderLine> lines() {
        return lines;
    }

    /** @return the delivery address as it was when the order was placed */
    public Address shippingAddress() {
        return shippingAddress;
    }

    /** @return the payment method */
    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    /** @return the price of the books */
    public Money subtotal() {
        return subtotal;
    }

    /** @return the shipping cost */
    public Money shippingCost() {
        return shippingCost;
    }

    /** @return the amount to pay: subtotal plus shipping */
    public Money total() {
        return subtotal.plus(shippingCost);
    }

    /** @return the current status */
    public OrderStatus status() {
        return status;
    }

    /** @return when the order was placed */
    public Instant placedAt() {
        return placedAt;
    }

    /** @return when the status last changed */
    public Instant statusChangedAt() {
        return statusChangedAt;
    }

    /**
     * Tells whether the order belongs to a customer.
     *
     * @param someCustomerId the customer's id
     * @return {@code true} if that customer placed the order
     */
    public boolean isOwnedBy(CustomerId someCustomerId) {
        return customerId.equals(someCustomerId);
    }

    /**
     * Tells whether the order includes a book.
     *
     * @param bookId the book's id
     * @return {@code true} if one of the lines is for that book
     */
    public boolean contains(BookId bookId) {
        return lines.stream().anyMatch(line -> line.bookId().equals(bookId));
    }

    /** @return the number of copies across all lines */
    public int totalQuantity() {
        return lines.stream().mapToInt(OrderLine::quantity).sum();
    }

    // ---------------------------------------------------------------- commands

    /**
     * Records that the payment went through.
     *
     * @param clock the clock giving the change time
     */
    public void markPaid(Clock clock) {
        moveTo(OrderStatus.PAID, clock);
    }

    /**
     * Records that the parcel was handed to the courier.
     *
     * @param clock the clock giving the change time
     */
    public void ship(Clock clock) {
        moveTo(OrderStatus.SHIPPED, clock);
    }

    /**
     * Records that the customer received the parcel.
     *
     * @param clock the clock giving the change time
     */
    public void deliver(Clock clock) {
        moveTo(OrderStatus.DELIVERED, clock);
    }

    /**
     * Cancels the order. Putting the copies back into stock is the catalog's job; the caller asks for
     * it in the same transaction using {@link #quantitiesByBook()}.
     *
     * @param clock the clock giving the change time
     * @throws ConflictException if the order has already been shipped, delivered or cancelled
     */
    public void cancel(Clock clock) {
        moveTo(OrderStatus.CANCELLED, clock);
    }

    /**
     * Returns how many copies of each book the order contains.
     *
     * @return quantities keyed by book id, in line order
     */
    public Map<BookId, Integer> quantitiesByBook() {
        Map<BookId, Integer> quantities = new LinkedHashMap<>();
        lines.forEach(line -> quantities.merge(line.bookId(), line.quantity(), Integer::sum));
        return Collections.unmodifiableMap(quantities);
    }

    @Override
    public String toString() {
        return "Order[" + number + ", " + status + ", " + total() + "]";
    }

    private void moveTo(OrderStatus target, Clock clock) {
        status.requireCanMoveTo(target);
        OrderStatus previous = status;
        Instant now = Instant.now(clock);
        status = target;
        statusChangedAt = now;
        raise(new OrderStatusChanged(number, customerId, previous, target, now));
    }

    private static void requireSellable(ProductSnapshot product, int quantity) {
        if (!product.active()) {
            throw new ConflictException("book.inactive", "'" + product.title() + "' is no longer sold");
        }
        if (!product.hasAtLeast(quantity)) {
            throw new StockShortageException(product.title(), quantity, product.availableStock());
        }
    }
}
