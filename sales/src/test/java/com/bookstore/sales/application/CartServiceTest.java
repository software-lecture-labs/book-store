package com.bookstore.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.sales.api.CartNotice;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.CartView;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.domain.StockShortageException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import org.junit.jupiter.api.Test;

class CartServiceTest {

    private final SalesShop shop = new SalesShop();
    private final FakeCatalog.Item dune = shop.book("Dune", "100.00", 20);
    private final FakeCatalog.Item madonna = shop.book("Kürk Mantolu Madonna", "45.50", 3);
    private final CustomerId customerId = shop.customer("ayse@example.com");

    @Test
    void guestCartIsChangedInPlaceAndPricedWithShipping() {
        GuestCart session = new GuestCart();

        CartView view = shop.cart.add(CartRef.guest(session), dune.id(), 2);

        assertThat(session.quantityOf(dune.id())).isEqualTo(2);
        assertThat(view.lines()).singleElement().satisfies(line -> {
            assertThat(line.title()).isEqualTo("Dune");
            assertThat(line.lineTotal()).isEqualTo(Money.of("200.00"));
            assertThat(line.availableStock()).isEqualTo(20);
        });
        assertThat(view.subtotal()).isEqualTo(Money.of("200.00"));
        assertThat(view.shippingCost()).isEqualTo(Money.of("49.90"));
        assertThat(view.total()).isEqualTo(Money.of("249.90"));
        assertThat(shop.carts.findByCustomerId(customerId)).isEmpty();
    }

    @Test
    void customerCartIsStored() {
        CartRef ref = CartRef.customer(customerId);

        shop.cart.add(ref, dune.id(), 5);
        shop.cart.add(ref, madonna.id(), 1);

        assertThat(shop.carts.findByCustomerId(customerId)).hasValueSatisfying(
                cart -> assertThat(cart.totalQuantity()).isEqualTo(6));
        CartView view = shop.cart.view(ref);
        assertThat(view.subtotal()).isEqualTo(Money.of("545.50"));
        assertThat(view.shippingCost()).isEqualTo(Money.ZERO);
        assertThat(shop.cart.itemCount(ref)).isEqualTo(6);
    }

    @Test
    void emptyCartHasNoShippingCost() {
        CartView view = shop.cart.view(CartRef.customer(customerId));

        assertThat(view.isEmpty()).isTrue();
        assertThat(view.total()).isEqualTo(Money.ZERO);
    }

    @Test
    void changingTheQuantityToZeroRemovesTheLine() {
        CartRef ref = CartRef.customer(customerId);
        shop.cart.add(ref, dune.id(), 2);

        assertThat(shop.cart.changeQuantity(ref, dune.id(), 4).totalQuantity()).isEqualTo(4);
        assertThat(shop.cart.changeQuantity(ref, dune.id(), 0).isEmpty()).isTrue();
    }

    @Test
    void removeAndClear() {
        CartRef ref = CartRef.customer(customerId);
        shop.cart.add(ref, dune.id(), 1);
        shop.cart.add(ref, madonna.id(), 1);

        assertThat(shop.cart.remove(ref, dune.id()).lines()).extracting(CartView.Line::title)
                .containsExactly("Kürk Mantolu Madonna");
        assertThat(shop.cart.clear(ref).isEmpty()).isTrue();
    }

    @Test
    void cannotAddMoreThanTheStockOrAnInactiveBook() {
        CartRef ref = CartRef.customer(customerId);
        madonna.deactivate();

        assertThatThrownBy(() -> shop.cart.add(ref, dune.id(), 21)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> shop.cart.add(ref, madonna.id(), 1)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> shop.cart.add(ref, BookId.of(999), 1)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void viewingWarnsAboutCatalogChangesAndStoresTheCorrectedCart() {
        CartRef ref = CartRef.customer(customerId);
        shop.cart.add(ref, dune.id(), 2);
        shop.cart.add(ref, madonna.id(), 3);
        dune.changePrice(Money.of("90.00"));
        madonna.changeStock(1);

        CartView first = shop.cart.view(ref);
        CartView second = shop.cart.view(ref);

        assertThat(first.adjustments()).containsExactly(
                new CartNotice.PriceChanged(dune.id(), "Dune", Money.of("100.00"), Money.of("90.00")),
                new CartNotice.QuantityReduced(madonna.id(), "Kürk Mantolu Madonna", 3, 1));
        assertThat(first.subtotal()).isEqualTo(Money.of("225.50"));
        assertThat(second.adjustments()).as("warnings are shown once").isEmpty();
    }

    @Test
    void signingInMergesTheGuestCart() {
        shop.cart.add(CartRef.customer(customerId), dune.id(), 1);
        GuestCart session = new GuestCart();
        shop.cart.add(CartRef.guest(session), dune.id(), 2);
        shop.cart.add(CartRef.guest(session), madonna.id(), 1);

        CartView merged = shop.cart.mergeGuestCart(customerId, session);

        assertThat(merged.lines()).extracting(CartView.Line::quantity).containsExactly(3, 1);
        assertThat(shop.carts.findByCustomerId(customerId).orElseThrow().totalQuantity()).isEqualTo(4);
    }

    @Test
    void insufficientStockIsReportedWithTheTitle() {
        assertThatThrownBy(() -> shop.cart.add(CartRef.customer(customerId), madonna.id(), 4))
                .isInstanceOfSatisfying(StockShortageException.class,
                        e -> assertThat(e.bookTitle()).isEqualTo("Kürk Mantolu Madonna"));
    }

    @Test
    void guestReferenceNeedsACart() {
        assertThatThrownBy(() -> CartRef.guest(null)).isInstanceOf(ValidationException.class);
    }
}
