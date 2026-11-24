package com.bookstore.sales.application;

import com.bookstore.catalog.api.StockFacade;
import com.bookstore.customer.api.CustomerDirectory;
import com.bookstore.sales.api.CartChangedException;
import com.bookstore.sales.api.CheckoutFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PaymentDeclinedException;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.domain.Cart;
import com.bookstore.sales.domain.CartAdjustment;
import com.bookstore.sales.domain.CartRepository;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.sales.domain.OrderNumberSequence;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.sales.domain.OrderRequest;
import com.bookstore.sales.domain.OrderRequestLog;
import com.bookstore.sales.domain.PaymentGateway.PaymentResult;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.sales.domain.PaymentMethod;
import com.bookstore.sales.domain.ProductSnapshot;
import com.bookstore.sales.domain.ShippingPolicy;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.DomainException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Turns a customer's cart into an order (ORD-1 to ORD-5, ORD-9 to ORD-11).
 *
 * <p>Checkout runs in two transactions (ADR-0014):
 *
 * <ol>
 *   <li>Check the cart against the current catalog. If anything changed (a price, the stock, a
 *       book no longer sold), store the corrected cart and stop with a {@link CartChangedException},
 *       so the customer never pays an amount they have not seen.</li>
 *   <li>Place the order: ask the catalog to take the copies out of stock, charge the card if paying
 *       by card, store the order and empty the cart. If anything fails, nothing is stored.</li>
 * </ol>
 *
 * <p>With an idempotency key, a repeat of a request gets the order of the first one instead of a
 * second order; the key is recorded before the payment (ADR-0022).
 *
 * <p>The catalog and customer modules are reached only through their {@code api} packages
 * ({@link StockFacade}, {@link CustomerDirectory}). The order's events are kept in the outbox in the second
 * transaction and handed on after it committed (ADR-0022).
 */
@ApplicationScoped
public class CheckoutService implements CheckoutFacade {

    /** The code of an idempotency key sent again with another address or payment method. */
    public static final String KEY_REUSED = "idempotency.key-reused";

    private final CustomerDirectory customers;
    private final CartRepository carts;
    private final StockFacade stock;
    private final OrderRepository orders;
    private final OrderNumberSequence orderNumbers;
    private final OrderRequestLog requests;
    private final PaymentGateway paymentGateway;
    private final ShippingPolicy shippingPolicy;
    private final TransactionRunner transactions;
    private final OrderEventOutbox outbox;
    private final OrderEventRelay relay;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param customers      the customer module's directory, for delivery addresses
     * @param carts          where carts are stored
     * @param stock          the catalog's prices and stock
     * @param orders         where orders are stored
     * @param orderNumbers   hands out order numbers
     * @param requests       remembers which order each idempotency key led to
     * @param paymentGateway charges cards
     * @param shippingPolicy the rule for the shipping cost
     * @param transactions   runs work in a transaction
     * @param outbox         keeps the events of a stored order in its transaction
     * @param relay          hands the events on once the order is stored
     * @param clock          the clock giving order times
     */
    @Inject
    public CheckoutService(CustomerDirectory customers, CartRepository carts, StockFacade stock,
                           OrderRepository orders, OrderNumberSequence orderNumbers, OrderRequestLog requests,
                           PaymentGateway paymentGateway,
                           ShippingPolicy shippingPolicy, TransactionRunner transactions,
                           OrderEventOutbox outbox, OrderEventRelay relay, Clock clock) {
        this.customers = customers;
        this.carts = carts;
        this.stock = stock;
        this.orders = orders;
        this.orderNumbers = orderNumbers;
        this.requests = requests;
        this.paymentGateway = paymentGateway;
        this.shippingPolicy = shippingPolicy;
        this.transactions = transactions;
        this.outbox = outbox;
        this.relay = relay;
        this.clock = clock;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CheckoutService() {
        this(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Places an order for everything in the customer's cart.
     *
     * <p>Orders paid by card are charged at once and become {@code PAID}. Bank transfers and cash
     * on delivery stay {@code CREATED} until an administrator records the payment (ADR-0013).
     *
     * @param command the checkout form
     * @return the new order
     * @throws CartChangedException     if the cart had to be corrected; nothing was ordered
     * @throws RuleViolationException   if the cart is empty
     * @throws ValidationException      if the payment method is unknown
     * @throws PaymentDeclinedException if the card was refused; nothing was ordered
     * @throws NotFoundException        if the customer or the address does not exist
     */
    @Override
    public OrderDetails placeOrder(PlaceOrderCommand command) {
        PaymentMethod paymentMethod = SalesMapper.paymentMethodOf(command.paymentMethod());
        String fingerprint = OrderRequest.fingerprintOf(command.addressId(), paymentMethod);
        Optional<OrderDetails> earlier = earlierAnswer(command, fingerprint);
        if (earlier.isPresent()) {
            return earlier.get();
        }
        List<CartAdjustment> adjustments = transactions.inTransaction(() -> reconcileCart(command.customerId()));
        if (!adjustments.isEmpty()) {
            throw new CartChangedException(SalesMapper.noticesOf(adjustments));
        }

        Order order;
        try {
            order = transactions.inTransaction(() -> place(command, paymentMethod, fingerprint));
        } catch (DomainException failure) {
            // The same key may have arrived twice at once. If the first request committed while this
            // one ran, this one fails on the key, or earlier on the cart the first one emptied; either
            // way it answers the first order instead of the failure.
            if (command.idempotencyKey() == null) {
                throw failure;
            }
            return earlierAnswer(command, fingerprint).orElseThrow(() -> failure);
        }

        relay.relayQuietly();
        return SalesMapper.detailsOf(order);
    }

    private List<CartAdjustment> reconcileCart(CustomerId customerId) {
        Cart cart = cartOf(customerId);
        List<CartAdjustment> adjustments = cart.reconcile(Products.load(stock, cart.bookIds()));
        if (!adjustments.isEmpty()) {
            carts.save(cart);
        }
        return adjustments;
    }

    /**
     * Answers a repeat of an earlier request with the order it placed (ADR-0022).
     *
     * @return the earlier order; empty without a key or for a key not seen before
     * @throws RuleViolationException {@code idempotency.key-reused} if the key came with another address or
     *                                payment method
     */
    private Optional<OrderDetails> earlierAnswer(PlaceOrderCommand command, String fingerprint) {
        if (command.idempotencyKey() == null) {
            return Optional.empty();
        }
        return requests.find(command.customerId(), command.idempotencyKey()).map(earlier -> {
            if (!earlier.sameAs(fingerprint)) {
                throw new RuleViolationException(KEY_REUSED,
                        "The key " + earlier.key() + " was used for another order request");
            }
            return orders.findByNumber(earlier.orderNumber())
                    .map(SalesMapper::detailsOf)
                    .orElseThrow(() -> new NotFoundException("Order", earlier.orderNumber()));
        });
    }

    private Order place(PlaceOrderCommand command, PaymentMethod paymentMethod, String fingerprint) {
        Address address = customers.shippingAddress(command.customerId(), command.addressId());
        Cart cart = cartOf(command.customerId());
        Map<BookId, ProductSnapshot> products = Products.load(stock, cart.bookIds());

        // Something may have changed since the first transaction; never charge a price the customer has not seen.
        List<CartAdjustment> lateChanges = cart.reconcile(products);
        if (!lateChanges.isEmpty()) {
            throw new CartChangedException(SalesMapper.noticesOf(lateChanges));
        }

        Order order = Order.place(orderNumbers.next(Year.now(clock)), command.customerId(), cart, products, address,
                paymentMethod, shippingPolicy, clock);
        if (command.idempotencyKey() != null) {
            // Before the payment: a concurrent repeat waits here and then fails instead of paying twice.
            requests.record(new OrderRequest(command.customerId(), command.idempotencyKey(), fingerprint,
                    order.number(), clock.instant()));
        }
        stock.reserve(order.quantitiesByBook());

        if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            PaymentResult result = paymentGateway.charge(order.number(), order.total());
            switch (result) {
                case PaymentResult.Approved approved -> order.markPaid(clock);
                case PaymentResult.Declined declined -> throw new PaymentDeclinedException(declined.reason());
            }
        }

        orders.save(order);
        outbox.append(order.pullEvents());
        cart.clear();
        carts.save(cart);
        return order;
    }

    private Cart cartOf(CustomerId customerId) {
        return carts.findByCustomerId(customerId).orElseGet(() -> Cart.forCustomer(customerId));
    }
}
