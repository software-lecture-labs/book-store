package com.bookstore.sales.domain;

import static com.bookstore.sales.domain.SalesTestData.CUSTOMER_ID;
import static com.bookstore.sales.domain.SalesTestData.priced;
import static com.bookstore.sales.domain.SalesTestData.product;
import static com.bookstore.sales.domain.SalesTestData.stocked;
import static com.bookstore.sales.domain.SalesTestData.withdrawn;
import static com.bookstore.shared.kernel.testing.TestAddresses.home;
import static com.bookstore.shared.kernel.testing.TestClock.CLOCK;
import static com.bookstore.shared.kernel.testing.TestClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OrderTest {

    private static final OrderNumber NUMBER = OrderNumber.of(Year.of(2026), 42);

    private final ProductSnapshot dune = product(1, "Dune", "100.00", 10);
    private final ProductSnapshot madonna = product(2, "Kürk Mantolu Madonna", "45.50", 3);
    private final Map<BookId, ProductSnapshot> products = new HashMap<>(Map.of(BookId.of(1), dune, BookId.of(2), madonna));
    private final Cart cart = Cart.forCustomer(CUSTOMER_ID);

    @Test
    void placingCopiesTheCartAndWorksOutTheTotals() {
        cart.add(dune, 2);
        cart.add(madonna, 1);

        Order order = place();

        assertThat(order.number()).isEqualTo(NUMBER);
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.placedAt()).isEqualTo(NOW);
        assertThat(order.lines()).containsExactly(
                new OrderLine(BookId.of(1), "Dune", Money.of("100.00"), 2),
                new OrderLine(BookId.of(2), "Kürk Mantolu Madonna", Money.of("45.50"), 1));
        assertThat(order.subtotal()).isEqualTo(Money.of("245.50"));
        assertThat(order.shippingCost()).isEqualTo(Money.of("49.90"));
        assertThat(order.total()).isEqualTo(Money.of("295.40"));
        assertThat(order.totalQuantity()).isEqualTo(3);
        assertThat(order.contains(BookId.of(2))).isTrue();
        assertThat(order.isOwnedBy(CUSTOMER_ID)).isTrue();
    }

    @Test
    void quantitiesByBookTellTheCatalogWhatToReserve() {
        cart.add(dune, 2);
        cart.add(madonna, 3);

        assertThat(place().quantitiesByBook()).containsExactly(Map.entry(BookId.of(1), 2), Map.entry(BookId.of(2), 3));
    }

    @Test
    void shippingIsFreeFromTheThreshold() {
        cart.add(dune, 5);

        Order order = place();

        assertThat(order.subtotal()).isEqualTo(Money.of("500.00"));
        assertThat(order.shippingCost()).isEqualTo(Money.ZERO);
    }

    @Test
    void placingRaisesOrderPlacedOnce() {
        cart.add(dune, 1);
        Order order = place();

        assertThat(order.pullEvents()).containsExactly(
                new OrderPlaced(NUMBER, CUSTOMER_ID, Money.of("149.90"), NOW));
        assertThat(order.pullEvents()).isEmpty();
    }

    @Test
    void theOrderUsesThePriceItWasPlacedWith() {
        cart.add(dune, 1);
        Order order = place();

        products.put(BookId.of(1), priced(dune, "999.00"));

        assertThat(order.lines().getFirst().unitPrice()).isEqualTo(Money.of("100.00"));
        assertThat(order.lines().getFirst().bookTitle()).isEqualTo("Dune");
    }

    @Test
    void refusesALineWithTooLittleStock() {
        cart.add(dune, 2);
        cart.add(madonna, 3);
        products.put(BookId.of(2), stocked(madonna, 1));

        assertThatThrownBy(this::place)
                .isInstanceOfSatisfying(StockShortageException.class, e -> {
                    assertThat(e.bookTitle()).isEqualTo("Kürk Mantolu Madonna");
                    assertThat(e.code()).isEqualTo("stock.insufficient");
                });
    }

    @Test
    void refusesABookThatIsNoLongerSoldOrMissing() {
        cart.add(dune, 1);
        products.put(BookId.of(1), withdrawn(dune));
        assertThatThrownBy(this::place).isInstanceOf(ConflictException.class).hasMessageContaining("no longer sold");

        products.remove(BookId.of(1));
        assertThatThrownBy(this::place).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cannotPlaceAnEmptyCart() {
        assertThatThrownBy(this::place)
                .isInstanceOfSatisfying(RuleViolationException.class, e -> assertThat(e.code()).isEqualTo(Order.EMPTY_CART));
    }

    @Test
    void cannotPlaceSomeoneElsesOrAGuestCart() {
        Cart other = Cart.forCustomer(CustomerId.of(99));
        other.add(dune, 1);
        Cart guest = Cart.guest();
        guest.add(dune, 1);

        assertThatThrownBy(() -> place(other)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> place(guest)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void followsTheHappyPathAndRaisesAnEventForEveryChange() {
        cart.add(dune, 1);
        Order order = place();
        order.pullEvents();
        Clock later = Clock.offset(CLOCK, Duration.ofHours(1));

        order.markPaid(later);
        order.ship(later);
        order.deliver(later);

        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.statusChangedAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(order.pullEvents())
                .extracting(OrderTest::target)
                .containsExactly(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    @Test
    void rejectsSkippingSteps() {
        cart.add(dune, 1);
        Order order = place();

        assertThatThrownBy(() -> order.ship(CLOCK))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(OrderStatus.ILLEGAL_TRANSITION));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void paidOrderCanBeCancelled() {
        cart.add(dune, 4);
        Order order = place();
        order.markPaid(CLOCK);

        order.cancel(CLOCK);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        cart.add(dune, 4);
        Order order = place();
        order.markPaid(CLOCK);
        order.ship(CLOCK);

        assertThatThrownBy(() -> order.cancel(CLOCK)).isInstanceOf(ConflictException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void reconstituteRecomputesTheSubtotal() {
        Order order = Order.reconstitute(OrderId.of(5), 2, NUMBER, CUSTOMER_ID,
                List.of(new OrderLine(BookId.of(1), "Dune", Money.of("100"), 3)), home(),
                PaymentMethod.BANK_TRANSFER, Money.ZERO, OrderStatus.SHIPPED, NOW, NOW);

        assertThat(order.id()).isEqualTo(OrderId.of(5));
        assertThat(order.version()).isEqualTo(2);
        assertThat(order.total()).isEqualTo(Money.of("300.00"));
        assertThat(order.pullEvents()).isEmpty();
    }

    private Order place() {
        return place(cart);
    }

    private Order place(Cart someCart) {
        return Order.place(NUMBER, CUSTOMER_ID, someCart, products, home(), PaymentMethod.CREDIT_CARD,
                ShippingPolicy.standard(), CLOCK);
    }

    private static OrderStatus target(Object event) {
        return switch ((OrderEvent) event) {
            case OrderStatusChanged changed -> changed.to();
            case OrderPlaced placed -> throw new AssertionError("unexpected " + placed);
        };
    }
}
