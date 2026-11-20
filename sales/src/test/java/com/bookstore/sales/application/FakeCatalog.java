package com.bookstore.sales.application;

import com.bookstore.catalog.api.BookOffer;
import com.bookstore.catalog.api.StockFacade;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stands in for the catalog module: books with a price, a stock and an active flag, behind the
 * {@link StockFacade} the sales module depends on. Like the real catalog, reserving is all or
 * nothing.
 */
public final class FakeCatalog implements StockFacade {

    private final Map<BookId, Item> items = new LinkedHashMap<>();
    private long nextId = 1;

    /**
     * Adds an active book.
     *
     * @param title the title
     * @param price the price
     * @param stock copies on hand
     * @return the book, which tests can change like a catalog administrator would
     */
    public Item add(String title, String price, int stock) {
        Item item = new Item(BookId.of(nextId++), title, Money.of(price), stock);
        items.put(item.id, item);
        return item;
    }

    /**
     * Returns a book's current stock.
     *
     * @param bookId the book's id
     * @return copies on hand
     */
    public int stockOf(BookId bookId) {
        return items.get(bookId).stock;
    }

    @Override
    public List<BookOffer> offers(Collection<BookId> bookIds) {
        return bookIds.stream().distinct().map(items::get).filter(Objects::nonNull).map(Item::offer).toList();
    }

    @Override
    public void reserve(Map<BookId, Integer> quantities) {
        quantities.forEach((bookId, quantity) -> {
            Item item = require(bookId);
            if (!item.active) {
                throw new ConflictException("book.inactive", "'" + item.title + "' is no longer sold");
            }
            if (item.stock < quantity) {
                throw new ConflictException("stock.insufficient", "not enough '" + item.title + "'");
            }
        });
        quantities.forEach((bookId, quantity) -> items.get(bookId).stock -= quantity);
    }

    @Override
    public void release(Map<BookId, Integer> quantities) {
        quantities.forEach((bookId, quantity) -> require(bookId).stock += quantity);
    }

    private Item require(BookId bookId) {
        Item item = items.get(bookId);
        if (item == null) {
            throw new NotFoundException("Book", bookId);
        }
        return item;
    }

    /** One book in the fake catalog. */
    public static final class Item {

        private final BookId id;
        private final String title;
        private Money price;
        private int stock;
        private boolean active = true;

        private Item(BookId id, String title, Money price, int stock) {
            this.id = id;
            this.title = title;
            this.price = price;
            this.stock = stock;
        }

        /** @return the book's id */
        public BookId id() {
            return id;
        }

        /** @param newPrice the new price */
        public void changePrice(Money newPrice) {
            this.price = newPrice;
        }

        /** @param quantity the new stock */
        public void changeStock(int quantity) {
            this.stock = quantity;
        }

        /** Stops selling the book. */
        public void deactivate() {
            this.active = false;
        }

        private BookOffer offer() {
            return new BookOffer(id, title, "Author of " + title, price, stock, active);
        }
    }
}
