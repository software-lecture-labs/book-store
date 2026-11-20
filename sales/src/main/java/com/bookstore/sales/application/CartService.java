package com.bookstore.sales.application;

import com.bookstore.catalog.api.StockFacade;
import com.bookstore.sales.api.CartFacade;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.CartView;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.domain.Cart;
import com.bookstore.sales.domain.CartAdjustment;
import com.bookstore.sales.domain.CartRepository;
import com.bookstore.sales.domain.ProductSnapshot;
import com.bookstore.sales.domain.ShippingPolicy;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cart use cases for visitors and customers (CRT-1 to CRT-5).
 *
 * <p>Every operation returns the cart as it should be shown: checked against the current catalog,
 * priced with current prices and with any adjustments listed.
 */
@ApplicationScoped
public class CartService implements CartFacade {

    private final StockFacade stock;
    private final CartRepository carts;
    private final ShippingPolicy shippingPolicy;
    private final TransactionRunner transactions;

    /**
     * Creates the service.
     *
     * @param stock          the catalog's prices and stock
     * @param carts          where customers' carts are stored
     * @param shippingPolicy the rule for the shipping cost
     * @param transactions   runs work in a transaction
     */
    @Inject
    public CartService(StockFacade stock, CartRepository carts, ShippingPolicy shippingPolicy,
                       TransactionRunner transactions) {
        this.stock = stock;
        this.carts = carts;
        this.shippingPolicy = shippingPolicy;
        this.transactions = transactions;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected CartService() {
        this(null, null, null, null);
    }

    /**
     * Shows a cart.
     *
     * @param ref which cart
     * @return the cart, checked against the current catalog
     */
    @Override
    public CartView view(CartRef ref) {
        return change(ref, cart -> { });
    }

    /**
     * Adds copies of a book.
     *
     * @param ref      which cart
     * @param bookId   the book's id
     * @param quantity copies to add
     * @return the updated cart
     * @throws NotFoundException if the book does not exist or is deactivated
     */
    @Override
    public CartView add(CartRef ref, BookId bookId, int quantity) {
        return change(ref, cart -> cart.add(requireActiveBook(bookId), quantity));
    }

    /**
     * Sets the number of copies of a book; zero removes it.
     *
     * @param ref      which cart
     * @param bookId   the book's id
     * @param quantity the new quantity
     * @return the updated cart
     */
    @Override
    public CartView changeQuantity(CartRef ref, BookId bookId, int quantity) {
        return change(ref, cart -> {
            if (quantity == 0) {
                cart.remove(bookId);
            } else {
                cart.changeQuantity(requireActiveBook(bookId), quantity);
            }
        });
    }

    /**
     * Removes a book.
     *
     * @param ref    which cart
     * @param bookId the book's id
     * @return the updated cart
     */
    @Override
    public CartView remove(CartRef ref, BookId bookId) {
        return change(ref, cart -> cart.remove(bookId));
    }

    /**
     * Empties a cart.
     *
     * @param ref which cart
     * @return the empty cart
     */
    @Override
    public CartView clear(CartRef ref) {
        return change(ref, Cart::clear);
    }

    /**
     * Merges a visitor's session cart into their stored cart after they sign in (CRT-2). The web
     * layer drops the session cart afterwards.
     *
     * @param customerId the customer who just signed in
     * @param guestCart  the session cart; it is not changed
     * @return the merged cart
     */
    @Override
    public CartView mergeGuestCart(CustomerId customerId, GuestCart guestCart) {
        return change(CartRef.customer(customerId), cart -> cart.mergeFrom(SalesMapper.cartOf(guestCart)));
    }

    /**
     * Returns the number of copies in a cart, for the badge on the cart icon. Does not check the
     * catalog, so it is cheap.
     *
     * @param ref which cart
     * @return the number of copies
     */
    public int itemCount(CartRef ref) {
        return load(ref).totalQuantity();
    }

    private CartView change(CartRef ref, Consumer<Cart> operation) {
        return transactions.inTransaction(() -> {
            Cart cart = load(ref);
            operation.accept(cart);
            Map<BookId, ProductSnapshot> current = Products.load(stock, cart.bookIds());
            List<CartAdjustment> adjustments = cart.reconcile(current);
            store(ref, cart);
            return toView(cart, current, adjustments);
        });
    }

    private Cart load(CartRef ref) {
        return switch (ref) {
            case CartRef.Guest guest -> SalesMapper.cartOf(guest.cart());
            case CartRef.Customer customer -> carts.findByCustomerId(customer.customerId())
                    .orElseGet(() -> Cart.forCustomer(customer.customerId()));
        };
    }

    private void store(CartRef ref, Cart cart) {
        switch (ref) {
            case CartRef.Guest guest -> SalesMapper.copyInto(cart, guest.cart());
            case CartRef.Customer customer -> carts.save(cart);
        }
    }

    private ProductSnapshot requireActiveBook(BookId bookId) {
        return Products.loadActive(stock, bookId);
    }

    private CartView toView(Cart cart, Map<BookId, ProductSnapshot> current, List<CartAdjustment> adjustments) {
        List<CartView.Line> lines = cart.items().stream()
                .map(item -> {
                    ProductSnapshot book = current.get(item.bookId());
                    return new CartView.Line(book.bookId(), book.title(), book.authorNames(), book.unitPrice(),
                            item.quantity(), book.unitPrice().times(item.quantity()), book.availableStock());
                })
                .toList();
        Money subtotal = Money.sum(lines.stream().map(CartView.Line::lineTotal));
        Money shipping = lines.isEmpty() ? Money.ZERO : shippingPolicy.costFor(subtotal);
        return new CartView(lines, subtotal, shipping, subtotal.plus(shipping), cart.totalQuantity(),
                SalesMapper.noticesOf(adjustments));
    }
}
