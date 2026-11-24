package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * The cart of a visitor who has not signed in. The web layer keeps it between requests (in the
 * HTTP session or the browser, decided in Phase 7) and passes it to {@link CartFacade} through
 * {@link CartRef#guest(GuestCart)}; the cart module updates its lines after every change.
 *
 * <p>It holds only what is needed to rebuild the cart: book ids, quantities and the price each
 * book had when it was added, so the customer can be told about price changes.
 */
public final class GuestCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Item> items = List.of();

    /** Creates an empty guest cart. */
    public GuestCart() {
    }

    /**
     * Returns the lines, in the order the books were first added.
     *
     * @return an unmodifiable list
     */
    public List<Item> items() {
        return items;
    }

    /** @return {@code true} if the cart has no lines */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the number of copies of a book in the cart.
     *
     * @param bookId the book
     * @return the quantity, zero if the book is not in the cart
     */
    public int quantityOf(BookId bookId) {
        return items.stream().filter(item -> item.bookId().equals(bookId)).mapToInt(Item::quantity).sum();
    }

    /** @return the number of copies in the cart */
    public int totalQuantity() {
        return items.stream().mapToInt(Item::quantity).sum();
    }

    /**
     * Replaces the lines after the cart module changed the cart. Not meant for the web layer.
     *
     * @param newItems the new lines
     */
    public void replaceItems(List<Item> newItems) {
        this.items = List.copyOf(Guard.requireNonNull(newItems, "items"));
    }

    @Override
    public String toString() {
        return "GuestCart" + items;
    }

    /**
     * One line of a guest cart.
     *
     * @param bookId         the book
     * @param quantity       the number of copies
     * @param priceWhenAdded the book's price when it was added
     */
    public record Item(BookId bookId, int quantity, Money priceWhenAdded) implements Serializable {
    }
}
