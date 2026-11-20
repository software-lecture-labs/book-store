package com.bookstore.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.sales.api.CartChangedException;
import com.bookstore.sales.api.CartNotice;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PaymentDeclinedException;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.OrderStatusChanged;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final SalesShop shop = new SalesShop();
    private final FakeCatalog.Item dune = shop.book("Dune", "100.00", 10);
    private final FakeCatalog.Item madonna = shop.book("Kürk Mantolu Madonna", "45.50", 3);
    private final CustomerId customerId = shop.customer("ayse@example.com");
    private final CartRef cart = CartRef.customer(customerId);

    @Test
    void cardPaymentPlacesAPaidOrderAndEmptiesTheCart() {
        shop.cart.add(cart, dune.id(), 2);
        shop.cart.add(cart, madonna.id(), 1);

        OrderDetails order = place("CREDIT_CARD");

        assertThat(order.orderNumber()).isEqualTo("BS-2026-000001");
        assertThat(order.status()).isEqualTo("PAID");
        assertThat(order.total()).isEqualTo(Money.of("295.40"));
        assertThat(order.shippingAddress()).isEqualTo(SalesShop.address());
        assertThat(shop.payments.charged()).containsExactly(Money.of("295.40"));
        assertThat(shop.cart.view(cart).isEmpty()).isTrue();
        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(8);
        assertThat(shop.orders.count()).isEqualTo(1);
    }

    @Test
    void customerIsNotifiedOnlyAfterTheOrderIsStored() {
        shop.cart.add(cart, dune.id(), 1);

        place("CREDIT_CARD");

        assertThat(shop.notifier.events()).hasSize(2);
        assertThat(shop.notifier.events().get(0)).isInstanceOf(OrderPlaced.class);
        assertThat(shop.notifier.events().get(1)).isInstanceOfSatisfying(OrderStatusChanged.class,
                e -> assertThat(e.to()).isEqualTo(OrderStatus.PAID));
    }

    @Test
    void bankTransferWaitsForPayment() {
        shop.cart.add(cart, dune.id(), 1);

        OrderDetails order = place("BANK_TRANSFER");

        assertThat(order.status()).isEqualTo("CREATED");
        assertThat(order.nextStatuses()).containsExactlyInAnyOrder("PAID", "CANCELLED");
        assertThat(shop.payments.charged()).isEmpty();
        assertThat(shop.notifier.events()).singleElement().isInstanceOf(OrderPlaced.class);
    }

    @Test
    void declinedCardPlacesNoOrderAndKeepsTheCart() {
        shop.cart.add(cart, dune.id(), 1);
        shop.payments.declineNext("Insufficient funds");

        assertThatThrownBy(() -> place("CREDIT_CARD"))
                .isInstanceOfSatisfying(PaymentDeclinedException.class,
                        e -> assertThat(e.reason()).isEqualTo("Insufficient funds"));

        assertThat(shop.orders.count()).isZero();
        assertThat(shop.cart.itemCount(cart)).isEqualTo(1);
        assertThat(shop.notifier.events()).isEmpty();
    }

    @Test
    void priceChangeStopsCheckoutUntilTheCustomerHasSeenIt() {
        shop.cart.add(cart, dune.id(), 1);
        dune.changePrice(Money.of("120.00"));

        assertThatThrownBy(() -> place("CREDIT_CARD"))
                .isInstanceOfSatisfying(CartChangedException.class, e -> assertThat(e.adjustments())
                        .containsExactly(new CartNotice.PriceChanged(dune.id(), "Dune", Money.of("100.00"),
                                Money.of("120.00"))));
        assertThat(shop.orders.count()).isZero();

        OrderDetails order = place("CREDIT_CARD");
        assertThat(order.subtotal()).isEqualTo(Money.of("120.00"));
    }

    @Test
    void bookSoldOutSinceItWasAddedStopsCheckout() {
        shop.cart.add(cart, madonna.id(), 3);
        madonna.changeStock(0);

        assertThatThrownBy(() -> place("CREDIT_CARD")).isInstanceOf(CartChangedException.class);
        assertThat(shop.cart.view(cart).isEmpty()).isTrue();
    }

    @Test
    void emptyCartCannotBeOrdered() {
        assertThatThrownBy(() -> place("CREDIT_CARD"))
                .isInstanceOfSatisfying(RuleViolationException.class, e -> assertThat(e.code()).isEqualTo(Order.EMPTY_CART));
    }

    @Test
    void addressMustBelongToTheCustomer() {
        CustomerId other = shop.customer("mehmet@example.com");
        shop.cart.add(cart, dune.id(), 1);

        assertThatThrownBy(() -> shop.checkout.placeOrder(
                new PlaceOrderCommand(customerId, shop.defaultAddressId(other), "CREDIT_CARD")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void orderNumbersAreSequential() {
        shop.cart.add(cart, dune.id(), 1);
        String first = place("CREDIT_CARD").orderNumber();
        shop.cart.add(cart, dune.id(), 1);
        String second = place("CREDIT_CARD").orderNumber();

        assertThat(first).isEqualTo("BS-2026-000001");
        assertThat(second).isEqualTo("BS-2026-000002");
    }

    @Test
    void laterPriceChangesDoNotAlterThePlacedOrder() {
        shop.cart.add(cart, dune.id(), 1);
        String number = place("CREDIT_CARD").orderNumber();

        dune.changePrice(Money.of("500.00"));

        assertThat(shop.myOrders.myOrder(customerId, number).lines().getFirst().unitPrice())
                .isEqualTo(Money.of("100.00"));
    }

    @Test
    void aRepeatedRequestGetsTheFirstOrderAndIsChargedOnce() {
        shop.cart.add(cart, dune.id(), 1);
        long address = shop.defaultAddressId(customerId);

        OrderDetails first = shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "CREDIT_CARD", "k-1"));
        OrderDetails repeat = shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "CREDIT_CARD", "k-1"));

        assertThat(repeat).isEqualTo(first);
        assertThat(shop.orders.count()).isEqualTo(1);
        assertThat(shop.payments.charged()).hasSize(1);
        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(9);
    }

    @Test
    void aKeyCannotBeReusedForAnotherRequest() {
        shop.cart.add(cart, dune.id(), 1);
        long address = shop.defaultAddressId(customerId);
        shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "CREDIT_CARD", "k-1"));

        assertThatThrownBy(() -> shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "BANK_TRANSFER",
                "k-1")))
                .isInstanceOfSatisfying(RuleViolationException.class,
                        e -> assertThat(e.code()).isEqualTo(CheckoutService.KEY_REUSED));
        assertThat(shop.orders.count()).isEqualTo(1);
    }

    @Test
    void ordersWithoutAKeyOrWithNewKeysAreSeparateOrders() {
        long address = shop.defaultAddressId(customerId);
        shop.cart.add(cart, dune.id(), 1);
        shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "CREDIT_CARD"));
        shop.cart.add(cart, dune.id(), 1);
        shop.checkout.placeOrder(new PlaceOrderCommand(customerId, address, "CREDIT_CARD", "k-2"));

        assertThat(shop.orders.count()).isEqualTo(2);
        assertThat(shop.requests.size()).isEqualTo(1);
    }

    private OrderDetails place(String method) {
        return shop.checkout.placeOrder(new PlaceOrderCommand(customerId, shop.defaultAddressId(customerId), method));
    }
}
