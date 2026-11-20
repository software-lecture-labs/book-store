package com.bookstore.sales.rest;

import com.bookstore.sales.api.CartNotice;
import com.bookstore.sales.api.CartView;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.util.List;

/**
 * How a cart is written as JSON. The cart's notices get a {@code type} member, so a client can
 * tell a removed book from a lower quantity or a new price.
 *
 * @param lines         the lines
 * @param subtotal      the price of the books
 * @param shippingCost  the shipping cost
 * @param total         what checkout would charge
 * @param totalQuantity the number of copies
 * @param notices       what the cart changed on its own since the last look (CRT-5)
 */
public record CartResponse(List<CartView.Line> lines, Money subtotal, Money shippingCost, Money total,
                           int totalQuantity, List<Notice> notices) {

    /** Copies the lists. */
    public CartResponse {
        lines = List.copyOf(lines);
        notices = List.copyOf(notices);
    }

    /**
     * Copies a cart view.
     *
     * @param cart the view
     * @return the response body
     */
    public static CartResponse of(CartView cart) {
        return new CartResponse(cart.lines(), cart.subtotal(), cart.shippingCost(), cart.total(), cart.totalQuantity(),
                notices(cart.adjustments()));
    }

    /**
     * Writes cart notices with their type.
     *
     * @param notices the notices
     * @return the response bodies
     */
    public static List<Notice> notices(List<CartNotice> notices) {
        return notices.stream().map(Notice::of).toList();
    }

    /**
     * One thing the cart changed.
     *
     * @param type         {@code REMOVED}, {@code QUANTITY_REDUCED} or {@code PRICE_CHANGED}
     * @param bookId       the book
     * @param bookTitle    the title
     * @param fromQuantity the old quantity, for {@code QUANTITY_REDUCED}
     * @param toQuantity   the new quantity, for {@code QUANTITY_REDUCED}
     * @param fromPrice    the old price, for {@code PRICE_CHANGED}
     * @param toPrice      the new price, for {@code PRICE_CHANGED}
     */
    public record Notice(String type, BookId bookId, String bookTitle, Integer fromQuantity, Integer toQuantity,
                         Money fromPrice, Money toPrice) {

        static Notice of(CartNotice notice) {
            return switch (notice) {
                case CartNotice.Removed removed ->
                        new Notice("REMOVED", removed.bookId(), removed.bookTitle(), null, null, null, null);
                case CartNotice.QuantityReduced reduced -> new Notice("QUANTITY_REDUCED", reduced.bookId(),
                        reduced.bookTitle(), reduced.from(), reduced.to(), null, null);
                case CartNotice.PriceChanged changed -> new Notice("PRICE_CHANGED", changed.bookId(),
                        changed.bookTitle(), null, null, changed.from(), changed.to());
            };
        }
    }
}
