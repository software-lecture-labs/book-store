package com.bookstore.sales.domain;

import static com.bookstore.sales.domain.SalesTestData.CUSTOMER_ID;
import static com.bookstore.sales.domain.SalesTestData.priced;
import static com.bookstore.sales.domain.SalesTestData.product;
import static com.bookstore.sales.domain.SalesTestData.stocked;
import static com.bookstore.sales.domain.SalesTestData.withdrawn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CartTest {

    private static final Money DUNE_PRICE = Money.of("100.00");
    private static final Money MADONNA_PRICE = Money.of("45.50");

    private ProductSnapshot dune = product(1, "Dune", "100.00", 20);
    private ProductSnapshot madonna = product(2, "Kürk Mantolu Madonna", "45.50", 3);
    private final Cart cart = Cart.forCustomer(CUSTOMER_ID);

    @Test
    void addingTheSameBookAddsUpTheQuantity() {
        cart.add(dune, 2);
        cart.add(dune, 3);

        assertThat(cart.quantityOf(BookId.of(1))).isEqualTo(5);
        assertThat(cart.items()).containsExactly(new CartItem(BookId.of(1), 5, DUNE_PRICE));
    }

    @Test
    void linesKeepTheOrderBooksWereFirstAdded() {
        cart.add(madonna, 1);
        cart.add(dune, 1);
        cart.add(madonna, 1);

        List<BookId> order = new ArrayList<>();
        for (CartItem item : cart) {
            order.add(item.bookId());
        }
        assertThat(order).containsExactly(BookId.of(2), BookId.of(1));
        assertThat(cart.totalQuantity()).isEqualTo(3);
    }

    @Test
    void quantityPerLineIsCappedAtTen() {
        cart.add(dune, 10);

        assertThatThrownBy(() -> cart.add(dune, 1))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at most 10");
        assertThat(cart.quantityOf(BookId.of(1))).isEqualTo(10);
    }

    @Test
    void quantityCannotExceedTheStock() {
        assertThatThrownBy(() -> cart.add(madonna, 4)).isInstanceOf(StockShortageException.class);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void outOfStockOrInactiveBooksCannotBeAdded() {
        ProductSnapshot soldOut = product(3, "Sold out", "10", 0);
        ProductSnapshot inactive = withdrawn(product(4, "Inactive", "10", 5));

        assertThatThrownBy(() -> cart.add(soldOut, 1)).isInstanceOf(StockShortageException.class);
        assertThatThrownBy(() -> cart.add(inactive, 1)).isInstanceOf(ConflictException.class);
    }

    @Test
    void changingTheQuantityToZeroRemovesTheLine() {
        cart.add(dune, 2);
        cart.changeQuantity(dune, 7);
        assertThat(cart.quantityOf(BookId.of(1))).isEqualTo(7);

        cart.changeQuantity(dune, 0);
        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.quantityOf(BookId.of(1))).isZero();
    }

    @Test
    void subtotalUsesPricesSuppliedByTheCaller() {
        cart.add(dune, 2);
        cart.add(madonna, 3);
        Map<BookId, Money> prices = Map.of(BookId.of(1), dune.unitPrice(), BookId.of(2), madonna.unitPrice());

        assertThat(cart.subtotal(prices::get)).isEqualTo(Money.of("336.50"));
        assertThat(Cart.guest().subtotal(prices::get)).isEqualTo(Money.ZERO);
    }

    @Test
    void mergingAGuestCartAddsUpAndCapsQuantities() {
        cart.add(dune, 8);
        Cart guest = Cart.guest();
        guest.add(dune, 5);
        guest.add(madonna, 1);

        cart.mergeFrom(guest);

        assertThat(cart.items()).containsExactly(new CartItem(BookId.of(1), 10, DUNE_PRICE), new CartItem(BookId.of(2), 1, MADONNA_PRICE));
        assertThat(guest.items()).hasSize(2);
        assertThat(cart.customerId()).contains(CUSTOMER_ID);
        assertThat(guest.customerId()).isEmpty();
    }

    @Test
    void iteratorDoesNotAllowRemoval() {
        cart.add(dune, 1);
        Iterator<CartItem> iterator = cart.iterator();
        iterator.next();

        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(iterator::next).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void guestCartSurvivesJavaSerialization() throws Exception {
        Cart guest = Cart.guest();
        guest.add(dune, 2);
        guest.add(madonna, 1);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(guest);
        }
        Cart copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Cart) in.readObject();
        }

        assertThat(copy.items()).containsExactly(new CartItem(BookId.of(1), 2, DUNE_PRICE), new CartItem(BookId.of(2), 1, MADONNA_PRICE));
        assertThat(copy.customerId()).isEmpty();
    }

    @Test
    void reconcileRemovesUnsellableBooksLowersQuantitiesAndNotesNewPrices() {
        ProductSnapshot soonInactive = product(3, "Soon inactive", "10", 5);
        ProductSnapshot soonGone = product(4, "Soon gone", "10", 5);
        cart.add(dune, 5);
        cart.add(madonna, 3);
        cart.add(soonInactive, 1);
        cart.add(soonGone, 1);

        madonna = stocked(madonna, 1);
        dune = priced(dune, "90.00");
        soonInactive = withdrawn(soonInactive);

        List<CartAdjustment> adjustments = cart.reconcile(Map.of(BookId.of(1), dune, BookId.of(2), madonna, BookId.of(3), soonInactive));

        assertThat(adjustments).containsExactly(
                new CartAdjustment.PriceChanged(BookId.of(1), "Dune", DUNE_PRICE, Money.of("90.00")),
                new CartAdjustment.QuantityReduced(BookId.of(2), "Kürk Mantolu Madonna", 3, 1),
                new CartAdjustment.Removed(BookId.of(3), "Soon inactive"),
                new CartAdjustment.Removed(BookId.of(4), "#4"));
        assertThat(cart.items()).containsExactly(
                new CartItem(BookId.of(1), 5, Money.of("90.00")), new CartItem(BookId.of(2), 1, MADONNA_PRICE));
    }

    @Test
    void reconcilingAnUpToDateCartChangesNothing() {
        cart.add(dune, 2);

        assertThat(cart.reconcile(Map.of(BookId.of(1), dune))).isEmpty();
        assertThat(cart.reconcile(Map.of(BookId.of(1), dune))).isEmpty();
        assertThat(cart.items()).containsExactly(new CartItem(BookId.of(1), 2, DUNE_PRICE));
    }

    @Test
    void mergeKeepsThePriceTheCustomerSawFirst() {
        cart.add(dune, 1);
        dune = priced(dune, "80.00");
        Cart guest = Cart.guest();
        guest.add(dune, 1);

        cart.mergeFrom(guest);

        assertThat(cart.items()).containsExactly(new CartItem(BookId.of(1), 2, DUNE_PRICE));
    }

    @Test
    void reconstituteRebuildsTheLines() {
        Cart restored = Cart.reconstitute(CUSTOMER_ID, List.of(new CartItem(BookId.of(2), 1, MADONNA_PRICE), new CartItem(BookId.of(1), 4, DUNE_PRICE)));

        assertThat(restored.bookIds()).containsExactlyInAnyOrder(BookId.of(1), BookId.of(2));
        assertThat(restored.items()).first().isEqualTo(new CartItem(BookId.of(2), 1, MADONNA_PRICE));
    }
}
