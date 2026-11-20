package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

/**
 * Ready-made products and ids for the sales domain tests. A {@link ProductSnapshot} cannot change,
 * so "the price changed" is written as a new snapshot with {@link #priced}.
 */
public final class SalesTestData {

    /** A customer id used as the owner of carts and orders. */
    public static final CustomerId CUSTOMER_ID = CustomerId.of(7);

    private SalesTestData() {
    }

    /** @return an active product with the given price and stock, by "Author {id}" */
    public static ProductSnapshot product(long id, String title, String price, int stock) {
        return new ProductSnapshot(BookId.of(id), title, "Author " + id, Money.of(price), stock, true);
    }

    /** @return the same product with a new price */
    public static ProductSnapshot priced(ProductSnapshot product, String price) {
        return new ProductSnapshot(product.bookId(), product.title(), product.authorNames(), Money.of(price),
                product.availableStock(), product.active());
    }

    /** @return the same product with a new stock */
    public static ProductSnapshot stocked(ProductSnapshot product, int stock) {
        return new ProductSnapshot(product.bookId(), product.title(), product.authorNames(), product.unitPrice(),
                stock, product.active());
    }

    /** @return the same product, no longer sold */
    public static ProductSnapshot withdrawn(ProductSnapshot product) {
        return new ProductSnapshot(product.bookId(), product.title(), product.authorNames(), product.unitPrice(),
                product.availableStock(), false);
    }
}
