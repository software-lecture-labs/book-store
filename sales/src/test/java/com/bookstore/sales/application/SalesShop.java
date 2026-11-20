package com.bookstore.sales.application;

import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.sales.domain.ShippingPolicy;
import com.bookstore.sales.persistence.InMemoryCartRepository;
import com.bookstore.sales.persistence.InMemoryOrderNumberSequence;
import com.bookstore.sales.persistence.InMemoryOrderEventOutbox;
import com.bookstore.sales.persistence.InMemoryOrderRepository;
import com.bookstore.sales.persistence.InMemoryOrderRequestLog;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.event.InProcessEventPublisher;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.testing.DirectTransactions;
import com.bookstore.shared.kernel.testing.RecordingNotifier;
import com.bookstore.shared.kernel.testing.TestAddresses;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * The sales module wired with in-memory repositories, fake ports and fakes of the catalog and
 * customer modules' facades. Each test creates a fresh shop.
 */
public final class SalesShop {

    /** A clock stopped at a fixed moment. */
    public static final Clock CLOCK = TestClock.CLOCK;

    public final FakeCatalog catalog = new FakeCatalog();
    public final FakeCustomers customers = new FakeCustomers();
    public final InMemoryCartRepository carts = new InMemoryCartRepository();
    public final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    public final DirectTransactions transactions = new DirectTransactions();
    public final RecordingNotifier notifier = new RecordingNotifier();
    public final InProcessEventPublisher events = new InProcessEventPublisher();

    {
        events.subscribe(OrderEvent.class, new OrderNotificationObserver(notifier));
    }

    public final ScriptedPaymentGateway payments = new ScriptedPaymentGateway();
    public final InMemoryOrderNumberSequence orderNumbers = new InMemoryOrderNumberSequence();
    public final InMemoryOrderRequestLog requests = new InMemoryOrderRequestLog();
    public final InMemoryOrderEventOutbox outbox = new InMemoryOrderEventOutbox();
    public final OrderEventRelay relay = new OrderEventRelay(outbox, events, transactions, CLOCK);
    public final ShippingPolicy shipping = ShippingPolicy.standard();

    public final CartService cart = new CartService(catalog, carts, shipping, transactions);
    public final CheckoutService checkout = new CheckoutService(customers, carts, catalog, orders, orderNumbers,
            requests, payments, shipping, transactions, outbox, relay, CLOCK);
    public final OrderService myOrders = new OrderService(orders);
    public final OrderAdminService orderAdmin = new OrderAdminService(orders, catalog, transactions, outbox, relay, CLOCK);
    public final PurchaseHistoryService purchases = new PurchaseHistoryService(orders);

    /** Adds an active book to the fake catalog. */
    public FakeCatalog.Item book(String title, String price, int stock) {
        return catalog.add(title, price, stock);
    }

    /** Registers a customer with one address and returns the customer's id. The e-mail is ignored. */
    public CustomerId customer(String email) {
        return customers.register();
    }

    /** Returns the id of a customer's address. */
    public long defaultAddressId(CustomerId customerId) {
        return customers.addressIdOf(customerId);
    }

    /** Puts a book in a customer's cart and pays by card. */
    public OrderDetails buy(CustomerId customerId, FakeCatalog.Item book, int quantity) {
        cart.add(CartRef.customer(customerId), book.id(), quantity);
        return checkout.placeOrder(new PlaceOrderCommand(customerId, defaultAddressId(customerId),
                "CREDIT_CARD"));
    }

    /** Buys a book and has the order shipped and delivered. */
    public OrderDetails buyAndReceive(CustomerId customerId, FakeCatalog.Item book) {
        OrderDetails order = buy(customerId, book, 1);
        orderAdmin.changeStatus(order.orderNumber(), "SHIPPED");
        return orderAdmin.changeStatus(order.orderNumber(), "DELIVERED");
    }

    /** A valid delivery address, the one every fake customer has. */
    public static Address address() {
        return TestAddresses.home();
    }

    /** Approves every charge unless told to decline the next one. */
    public static final class ScriptedPaymentGateway implements PaymentGateway {

        private final List<Money> charged = new ArrayList<>();
        private String declineReason;

        @Override
        public PaymentResult charge(OrderNumber orderNumber, Money amount) {
            if (declineReason != null) {
                String reason = declineReason;
                declineReason = null;
                return new PaymentResult.Declined(reason);
            }
            charged.add(amount);
            return new PaymentResult.Approved("TX-" + orderNumber.value());
        }

        /** Makes the next charge fail. */
        public void declineNext(String reason) {
            this.declineReason = reason;
        }

        /** @return the amounts charged successfully, in order */
        public List<Money> charged() {
            return List.copyOf(charged);
        }
    }
}
