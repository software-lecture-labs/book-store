package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Books a visitor or customer intends to buy.
 *
 * <p>A guest cart lives in the visitor's HTTP session, which is why the class is
 * {@link Serializable}. A customer cart is stored in the database under the customer's id. When a
 * visitor signs in, the guest cart is {@linkplain #mergeFrom(Cart) merged} into the customer cart.
 *
 * <p>Lines keep the order in which books were first added.
 */
public final class Cart implements Iterable<CartItem>, Serializable {

    /** Most copies of one book a cart can hold. */
    public static final int MAX_QUANTITY_PER_LINE = 10;

    /** Most different books a cart can hold. */
    public static final int MAX_LINES = 50;

    @Serial
    private static final long serialVersionUID = 1L;

    private final CustomerId customerId;
    private final LinkedHashMap<BookId, CartItem> items = new LinkedHashMap<>();

    private Cart(CustomerId customerId) {
        this.customerId = customerId;
    }

    /**
     * Creates an empty cart for a visitor who has not signed in.
     *
     * @return the cart
     */
    public static Cart guest() {
        return new Cart(null);
    }

    /**
     * Rebuilds a visitor's cart from the lines the web layer kept between requests.
     *
     * @param items the lines, in order
     * @return the cart
     */
    public static Cart guestWith(List<CartItem> items) {
        Cart cart = guest();
        for (CartItem item : items) {
            cart.items.put(item.bookId(), item);
        }
        return cart;
    }

    /**
     * Creates an empty cart for a customer.
     *
     * @param customerId the customer's id
     * @return the cart
     */
    public static Cart forCustomer(CustomerId customerId) {
        Guard.requireNonNull(customerId, "customerId");
        return new Cart(customerId);
    }

    /**
     * Reconstitutes a customer's cart loaded from storage.
     *
     * @param customerId the customer's id
     * @param items      the stored lines, in order
     * @return the cart
     */
    public static Cart reconstitute(CustomerId customerId, List<CartItem> items) {
        Cart cart = forCustomer(customerId);
        for (CartItem item : items) {
            cart.items.put(item.bookId(), item);
        }
        return cart;
    }

    // ---------------------------------------------------------------- queries

    /**
     * Returns the owner of the cart.
     *
     * @return the customer's id, or empty for a guest cart
     */
    public Optional<CustomerId> customerId() {
        return Optional.ofNullable(customerId);
    }

    /** @return the lines, in the order books were first added */
    public List<CartItem> items() {
        return List.copyOf(items.values());
    }

    /** @return the ids of the books in the cart */
    public Set<BookId> bookIds() {
        return Set.copyOf(items.keySet());
    }

    /**
     * Returns how many copies of a book are in the cart.
     *
     * @param bookId the book's id
     * @return the quantity, zero if the book is not in the cart
     */
    public int quantityOf(BookId bookId) {
        CartItem item = items.get(bookId);
        return item == null ? 0 : item.quantity();
    }

    /** @return the number of copies across all lines, shown on the cart icon */
    public int totalQuantity() {
        return items.values().stream().mapToInt(CartItem::quantity).sum();
    }

    /** @return {@code true} if the cart has no lines */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Adds up the cart using prices supplied by the caller, because the cart always charges the
     * books' current prices.
     *
     * @param unitPrice returns the current price of a book given its id
     * @return the sum of price times quantity over all lines
     */
    public Money subtotal(Function<BookId, Money> unitPrice) {
        return Money.sum(items.values().stream()
                .map(item -> unitPrice.apply(item.bookId()).times(item.quantity())));
    }

    /**
     * Iterates over the lines in order. The iterator does not support removal; use
     * {@link #remove(long)} instead.
     *
     * @return an iterator over the lines
     */
    @Override
    public Iterator<CartItem> iterator() {
        return new LineIterator();
    }

    // ---------------------------------------------------------------- commands

    /**
     * Adds copies of a book, on top of any already in the cart.
     *
     * @param product  the book as currently offered by the catalog
     * @param quantity copies to add, positive
     * @throws ConflictException           if the book is not for sale
     * @throws StockShortageException      if the stock does not cover the new quantity
     */
    public void add(ProductSnapshot product, int quantity) {
        Guard.requirePositive(quantity, "quantity");
        put(product, quantityOf(product.bookId()) + quantity);
    }

    /**
     * Sets the number of copies of a book. Zero removes the line.
     *
     * @param product  the book as currently offered by the catalog
     * @param quantity the new quantity, zero or greater
     */
    public void changeQuantity(ProductSnapshot product, int quantity) {
        Guard.requireNonNegative(quantity, "quantity");
        if (quantity == 0) {
            remove(product.bookId());
        } else {
            put(product, quantity);
        }
    }

    /**
     * Removes a book from the cart. Removing a book that is not in the cart does nothing.
     *
     * @param bookId the book's id
     */
    public void remove(BookId bookId) {
        items.remove(bookId);
    }

    /** Removes every line. */
    public void clear() {
        items.clear();
    }

    /**
     * Adds the lines of another cart to this one, typically a guest cart after signing in.
     * Quantities of the same book are added up and capped at {@link #MAX_QUANTITY_PER_LINE}; stock is
     * checked again when the cart is shown.
     *
     * @param other the cart to merge in; it is not changed
     */
    public void mergeFrom(Cart other) {
        for (CartItem item : other) {
            CartItem existing = items.get(item.bookId());
            if (existing != null) {
                int merged = Math.min(MAX_QUANTITY_PER_LINE, existing.quantity() + item.quantity());
                items.put(item.bookId(), existing.withQuantity(merged));
            } else if (items.size() < MAX_LINES) {
                items.put(item.bookId(), item);
            }
        }
    }

    /**
     * Brings the cart in line with the current catalog: removes books that are no longer sold or
     * out of stock, lowers quantities to the available stock and records new prices.
     *
     * @param products the current state of the books in the cart, keyed by id; a missing book counts
     *              as no longer sold
     * @return what was changed, in cart order; empty if nothing changed
     */
    public List<CartAdjustment> reconcile(Map<BookId, ProductSnapshot> products) {
        List<CartAdjustment> adjustments = new ArrayList<>();
        Iterator<Map.Entry<BookId, CartItem>> entries = items.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BookId, CartItem> entry = entries.next();
            CartItem item = entry.getValue();
            ProductSnapshot book = products.get(item.bookId());

            if (book == null || !book.isAvailableForSale()) {
                entries.remove();
                adjustments.add(new CartAdjustment.Removed(item.bookId(),
                        book == null ? "#" + item.bookId() : book.title()));
                continue;
            }
            int available = book.availableStock();
            if (item.quantity() > available) {
                adjustments.add(new CartAdjustment.QuantityReduced(item.bookId(), book.title(), item.quantity(),
                        available));
                item = item.withQuantity(available);
            }
            if (!item.priceWhenAdded().equals(book.unitPrice())) {
                adjustments.add(new CartAdjustment.PriceChanged(item.bookId(), book.title(), item.priceWhenAdded(),
                        book.unitPrice()));
                item = item.withPrice(book.unitPrice());
            }
            entry.setValue(item);
        }
        return adjustments;
    }

    @Override
    public String toString() {
        return "Cart[" + (customerId == null ? "guest" : "customer=" + customerId) + ", " + items.values() + "]";
    }

    private void put(ProductSnapshot book, int newQuantity) {
        Guard.requireNonNull(book, "product");
        if (!book.active()) {
            throw new ConflictException("book.inactive", "'" + book.title() + "' is no longer sold");
        }
        Guard.require(newQuantity <= MAX_QUANTITY_PER_LINE, "quantity",
                "at most " + MAX_QUANTITY_PER_LINE + " copies of one book can be ordered");
        if (!book.hasAtLeast(newQuantity)) {
            throw new StockShortageException(book.title(), newQuantity, book.availableStock());
        }
        Guard.require(items.containsKey(book.bookId()) || items.size() < MAX_LINES, "items",
                "a cart can hold at most " + MAX_LINES + " different books");
        items.put(book.bookId(), new CartItem(book.bookId(), newQuantity, book.unitPrice()));
    }

    /**
     * An inner (non-static) class: each iterator belongs to one cart and reads that cart's lines
     * directly.
     */
    private final class LineIterator implements Iterator<CartItem> {

        private final Iterator<CartItem> delegate = items.values().iterator();

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public CartItem next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return delegate.next();
        }
    }
}
