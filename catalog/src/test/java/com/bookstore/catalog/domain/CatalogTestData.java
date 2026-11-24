package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

/**
 * Ready-made, already "stored" catalog objects for tests.
 */
public final class CatalogTestData {

    /** @return the first bytes of a PNG file, then some data */
    public static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    }

    /** @return the first bytes of a JPEG file, then some data */
    public static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    }

    private CatalogTestData() {
    }

    /** @return a stored root category "Literature" (id 1) with a stored child "Novel" (id 2) */
    public static Category novels() {
        Category literature = Category.reconstituteRoot(CategoryId.of(1), 0, "Literature");
        return Category.reconstituteChild(CategoryId.of(2), 0, "Novel", literature);
    }

    /** @return a stored author */
    public static Author author(long id, String name) {
        return Author.reconstitute(AuthorId.of(id), 0, name);
    }

    /**
     * Returns a valid ISBN-13 built from a running number.
     *
     * @param n any number below one billion
     * @return a valid ISBN
     */
    public static Isbn isbn(long n) {
        String body = "978" + String.format("%09d", n);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (body.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return new Isbn(body + (10 - sum % 10) % 10);
    }

    /** @return a stored, active book in {@link #novels()} */
    public static Book book(long id, String title, String price, int stock) {
        return bookBuilder(id, title, price, stock).build();
    }

    /** @return a builder for a stored, active book, for tests that need to change more attributes */
    public static Book.Builder bookBuilder(long id, String title, String price, int stock) {
        return Book.builder()
                .persisted(BookId.of(id), 0)
                .isbn(isbn(id))
                .title(title)
                .author(author(100 + id, "Author " + id))
                .category(novels())
                .price(Money.of(price))
                .stock(Stock.of(stock));
    }
}
