package com.bookstore.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.OrderPlaced;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.OrderStatusChanged;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import org.junit.jupiter.api.Test;

/** The customer's order history and the administrator's order management. */
class OrderManagementTest {

    private final SalesShop shop = new SalesShop();
    private final FakeCatalog.Item dune = shop.book("Dune", "100.00", 10);
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final CustomerId mehmet = shop.customer("mehmet@example.com");

    @Test
    void customersSeeOnlyTheirOwnOrders() {
        String aysesOrder = shop.buy(ayse, dune, 1).orderNumber();
        shop.buy(mehmet, dune, 2);
        shop.buy(ayse, dune, 3);

        Page<OrderSummary> page = shop.myOrders.myOrders(ayse, PageRequest.first());

        assertThat(page.content()).extracting(OrderSummary::customerId).containsOnly(ayse);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(shop.myOrders.myOrder(ayse, aysesOrder).lines())
                .singleElement()
                .satisfies(line -> assertThat(line.quantity()).isEqualTo(1));
        assertThatThrownBy(() -> shop.myOrders.myOrder(mehmet, aysesOrder)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> shop.myOrders.myOrder(ayse, "BS-2026-999999")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void administratorMovesAnOrderToDeliveredAndTheCustomerIsToldEachTime() {
        String number = shop.buy(ayse, dune, 1).orderNumber();
        shop.notifier.clear();

        shop.orderAdmin.changeStatus(number, "SHIPPED");
        OrderDetails delivered = shop.orderAdmin.changeStatus(number, "DELIVERED");

        assertThat(delivered.status()).isEqualTo("DELIVERED");
        assertThat(delivered.nextStatuses()).isEmpty();
        assertThat(shop.notifier.events()).extracting(OrderManagementTest::newStatus)
                .containsExactly(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    @Test
    void cancellingReturnsTheStock() {
        String number = shop.buy(ayse, dune, 4).orderNumber();
        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(6);

        shop.orderAdmin.changeStatus(number, "CANCELLED");

        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(10);
        assertThat(shop.orderAdmin.order(number).status()).isEqualTo("CANCELLED");
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        String number = shop.buy(ayse, dune, 4).orderNumber();
        shop.orderAdmin.changeStatus(number, "SHIPPED");

        assertThatThrownBy(() -> shop.orderAdmin.changeStatus(number, "CANCELLED"))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(OrderStatus.ILLEGAL_TRANSITION));
        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(6);
        assertThatThrownBy(() -> shop.orderAdmin.changeStatus(number, "CREATED"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void customerCanCancelOwnOrderAndRestoreStock() {
        String number = shop.buy(ayse, dune, 4).orderNumber();

        OrderDetails cancelled = shop.myOrders.cancel(ayse, number);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.nextStatuses()).isEmpty();
        assertThat(shop.catalog.stockOf(dune.id())).isEqualTo(10);
    }

    @Test
    void customerCannotCancelAnotherCustomersOrder() {
        String number = shop.buy(mehmet, dune, 1).orderNumber();

        assertThatThrownBy(() -> shop.myOrders.cancel(ayse, number))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void customerCannotCancelShippedOrder() {
        String number = shop.buy(ayse, dune, 1).orderNumber();
        shop.orderAdmin.changeStatus(number, "SHIPPED");

        assertThatThrownBy(() -> shop.myOrders.cancel(ayse, number))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(OrderStatus.ILLEGAL_TRANSITION));
    }

    @Test
    void administratorFiltersOrdersByStatus() {
        String shipped = shop.buy(ayse, dune, 1).orderNumber();
        shop.buy(mehmet, dune, 1);
        shop.orderAdmin.changeStatus(shipped, "SHIPPED");

        assertThat(shop.orderAdmin.ordersIn("SHIPPED", PageRequest.first()).content())
                .extracting(OrderSummary::orderNumber).containsExactly(shipped);
        assertThat(shop.orderAdmin.orders(PageRequest.first()).totalElements()).isEqualTo(2);
    }

    private static OrderStatus newStatus(DomainEvent event) {
        return switch ((OrderEvent) event) {
            case OrderStatusChanged changed -> changed.to();
            case OrderPlaced placed -> OrderStatus.CREATED;
        };
    }
}
