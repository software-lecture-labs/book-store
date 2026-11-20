package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.domain.Book;
import com.bookstore.customer.domain.Customer;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PaymentDeclinedException;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.shared.kernel.error.ConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

/**
 * Checkout across the catalog, customer and sales modules, on the real JDBC repositories and
 * transactions. These are the guarantees the in-memory fakes cannot show: a failure rolls back every
 * change in every module, and two customers racing for the last copy cannot both get it.
 */
class CheckoutWithDatabaseTest {

    private final JdbcBookstore store = newStore();

    /** @return the application on H2; PostgreSQL in {@link PostgresCheckoutWithDatabaseTest} */
    protected JdbcBookstore newStore() {
        return new JdbcBookstore();
    }

    private final Book dune = store.catalogDb.book("Dune", "100.00", 5);
    private final Customer ayse = store.customerDb.customer("ayse@example.com");

    @Test
    void successfulCheckoutStoresEverythingTogether() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 2);

        OrderDetails order = place(ayse);

        assertThat(order.status()).isEqualTo("PAID");
        assertThat(stockOf(dune)).isEqualTo(3);
        assertThat(store.orders.findByNumber(new OrderNumber(order.orderNumber())))
                .hasValueSatisfying(o -> assertThat(o.totalQuantity()).isEqualTo(2));
        assertThat(store.carts.findByCustomerId(ayse.id()).orElseThrow().isEmpty()).isTrue();
        assertThat(store.notified).hasSize(2);
    }

    /**
     * The dual-write problem (ADR-0007): the order is committed before its events are published, so
     * a failing notification cannot undo it. The customer sees an error for an order that exists, and
     * the notification is lost. The Transactional Outbox of Phase 9 closes this gap.
     */
    @Test
    void aFailingNotificationKeepsTheEventInTheOutboxUntilItGetsThrough() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 2);
        store.notificationsFail.set(true);

        OrderDetails order = place(ayse);

        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(stockOf(dune)).isEqualTo(3);
        assertThat(store.carts.findByCustomerId(ayse.id()).orElseThrow().isEmpty()).isTrue();
        assertThat(store.notified).isEmpty();
        // Placed, then paid by card: two events.
        assertThat(pendingEvents()).isEqualTo(2);

        store.notificationsFail.set(false);
        assertThat(store.module.orderEventRelay().relay()).isEqualTo(2);

        assertThat(store.notified).hasSize(2)
                .allSatisfy(event -> assertThat(event).hasFieldOrPropertyWithValue("orderNumber",
                        new OrderNumber(order.orderNumber())));
        assertThat(pendingEvents()).isZero();
    }

    @Test
    void aDeclinedPaymentLeavesNoEventBehind() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 1);
        store.declineCards.set(true);

        assertThatThrownBy(() -> place(ayse)).isInstanceOf(PaymentDeclinedException.class);

        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM order_outbox")).isZero();
    }

    private long pendingEvents() {
        return store.database.jdbc().queryForLong("SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL");
    }

    @Test
    void declinedPaymentRollsBackTheStockTheOrderAndTheOrderNumber() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 2);
        store.declineCards.set(true);

        assertThatThrownBy(() -> place(ayse)).isInstanceOf(PaymentDeclinedException.class);

        assertThat(stockOf(dune)).as("stock taken by Order.place was rolled back").isEqualTo(5);
        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM orders")).isZero();
        assertThat(store.carts.findByCustomerId(ayse.id()).orElseThrow().totalQuantity()).isEqualTo(2);
        assertThat(store.notified).isEmpty();

        store.declineCards.set(false);
        assertThat(place(ayse).orderNumber()).as("the rolled-back number is handed out again")
                .isEqualTo("BS-2026-000001");
    }

    @Test
    void cancellingPutsTheStockBackInTheDatabase() {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 4);
        String number = place(ayse).orderNumber();

        store.orderAdmin.changeStatus(number, "CANCELLED");

        assertThat(stockOf(dune)).isEqualTo(5);
        assertThat(store.orderAdmin.order(number).status()).isEqualTo("CANCELLED");
    }

    @Test
    void twoCustomersRacingForTheLastCopyCannotBothGetIt() throws Exception {
        Book lastCopy = store.catalogDb.book("Last copy", "50.00", 1);
        Customer mehmet = store.customerDb.customer("mehmet@example.com");
        store.cart.add(CartRef.customer(ayse.id()), lastCopy.id(), 1);
        store.cart.add(CartRef.customer(mehmet.id()), lastCopy.id(), 1);

        CountDownLatch start = new CountDownLatch(1);
        List<Callable<String>> attempts = List.of(ayse, mehmet).stream()
                .<Callable<String>>map(customer -> () -> {
                    start.await();
                    try {
                        return place(customer).orderNumber();
                    } catch (ConflictException e) {
                        return "refused: " + e.code();
                    }
                })
                .toList();

        List<String> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<String>> futures = attempts.stream().map(pool::submit).toList();
            start.countDown();
            for (Future<String> future : futures) {
                outcomes.add(future.get());
            }
        }

        assertThat(outcomes).filteredOn(o -> o.startsWith("BS-")).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o.startsWith("refused: ")).hasSize(1);
        assertThat(stockOf(lastCopy)).isZero();
        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM orders")).isEqualTo(1);
    }

    @Test
    void theSameKeySentTwiceAtOncePlacesOneOrderAndAnswersItTwice() throws Exception {
        store.cart.add(CartRef.customer(ayse.id()), dune.id(), 1);
        long addressId = ayse.defaultAddress().orElseThrow().id().value();
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            start.await();
            return store.checkout.placeOrder(new PlaceOrderCommand(ayse.id(), addressId, "CREDIT_CARD", "same-key"))
                    .orderNumber();
        };

        List<String> answers = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<String>> futures = List.of(pool.submit(attempt), pool.submit(attempt));
            start.countDown();
            for (Future<String> future : futures) {
                answers.add(future.get());
            }
        }

        assertThat(answers).hasSize(2).allMatch(answers.getFirst()::equals);
        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM orders")).isEqualTo(1);
        assertThat(store.database.jdbc().queryForLong("SELECT COUNT(*) FROM order_requests")).isEqualTo(1);
        assertThat(stockOf(dune)).isEqualTo(4);
    }

    private OrderDetails place(Customer customer) {
        long addressId = customer.defaultAddress().orElseThrow().id().value();
        return store.checkout.placeOrder(new PlaceOrderCommand(customer.id(), addressId, "CREDIT_CARD"));
    }

    private int stockOf(Book book) {
        return store.catalogDb.books.findById(book.id()).orElseThrow().stock().quantity();
    }
}
