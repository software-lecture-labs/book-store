package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.Cart;
import com.bookstore.sales.domain.CartItem;
import com.bookstore.sales.domain.CartRepository;
import com.bookstore.shared.jdbc.Columns;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Stores customers' carts: one {@code carts} row per customer and one {@code cart_items} row per
 * line. Saving replaces all lines, because carts are small.
 */
@ApplicationScoped
public class JdbcCartRepository implements CartRepository {

    private final JdbcExecutor jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc runs the SQL
     */
    @Inject
    public JdbcCartRepository(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcCartRepository() {
        this(null);
    }

    @Override
    public Optional<Cart> findByCustomerId(CustomerId customerId) {
        if (!jdbc.exists("SELECT 1 FROM carts WHERE user_id = ?", customerId)) {
            return Optional.empty();
        }
        List<CartItem> items = jdbc.query(
                "SELECT book_id, quantity, price_when_added FROM cart_items WHERE user_id = ? ORDER BY sort_order",
                row -> new CartItem(BookId.of(row.getLong("book_id")), row.getInt("quantity"),
                        Columns.money(row, "price_when_added")),
                customerId);
        return Optional.of(Cart.reconstitute(customerId, items));
    }

    @Override
    public Cart save(Cart cart) {
        CustomerId customerId = cart.customerId()
                .orElseThrow(() -> new IllegalArgumentException("Guest carts are kept in the session, not stored"));
        if (!jdbc.exists("SELECT 1 FROM carts WHERE user_id = ?", customerId)) {
            jdbc.update("INSERT INTO carts (user_id) VALUES (?)", customerId);
        }
        jdbc.update("DELETE FROM cart_items WHERE user_id = ?", customerId);

        List<Object[]> rows = new ArrayList<>();
        int order = 0;
        for (CartItem item : cart) {
            rows.add(new Object[] {customerId, item.bookId(), order++, item.quantity(), item.priceWhenAdded()});
        }
        jdbc.batch("INSERT INTO cart_items (user_id, book_id, sort_order, quantity, price_when_added) "
                + "VALUES (?, ?, ?, ?, ?)", rows);
        return cart;
    }
}
