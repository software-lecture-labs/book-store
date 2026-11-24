package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.util.List;

/**
 * What the cart page shows, priced with the books' current prices.
 *
 * @param lines         the lines, in the order books were added
 * @param subtotal      the price of the books
 * @param shippingCost  the shipping cost under the store's policy
 * @param total         subtotal plus shipping
 * @param totalQuantity the number of copies across all lines
 * @param adjustments   changes made because the catalog changed, to show as warnings (CRT-5)
 */
public record CartView(List<Line> lines, Money subtotal, Money shippingCost, Money total, int totalQuantity,
                       List<CartNotice> adjustments) {

    /** Copies the lists. */
    public CartView {
        lines = List.copyOf(lines);
        adjustments = List.copyOf(adjustments);
    }

    /**
     * Tells whether the cart has no lines.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * One line of the cart page.
     *
     * @param bookId         the book's id
     * @param title          the title
     * @param authorNames    the authors, joined with commas
     * @param unitPrice      the current price of one copy
     * @param quantity       copies in the cart
     * @param lineTotal      unit price times quantity
     * @param availableStock copies that could be ordered, to limit the quantity selector
     */
    public record Line(BookId bookId, String title, String authorNames, Money unitPrice, int quantity, Money lineTotal,
                       int availableStock) {
    }
}
