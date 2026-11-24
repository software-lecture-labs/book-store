package com.bookstore.customer.persistence;

import com.bookstore.customer.domain.FavoriteRepository;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;
import java.time.Clock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JdbcFavoriteRepository implements FavoriteRepository {
    private final JdbcExecutor jdbc;
    private final Clock clock;

    @Inject
    public JdbcFavoriteRepository(JdbcExecutor jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    protected JdbcFavoriteRepository() {
        this(null, null);
    }

    @Override
    public void add(CustomerId customerId, BookId bookId) {
        jdbc.update("INSERT INTO favorites (user_id, book_id, added_at) VALUES (?, ?, ?)",
                customerId, bookId, clock.instant());
    }

    @Override
    public boolean remove(CustomerId customerId, BookId bookId) {
        return jdbc.update("DELETE FROM favorites WHERE user_id = ? AND book_id = ?", customerId, bookId) == 1;
    }

    @Override
    public Page<BookId> findByCustomer(CustomerId customerId, PageRequest page) {
        long total = jdbc.queryForLong("SELECT COUNT(*) FROM favorites WHERE user_id = ?", customerId);
        if (total == 0) {
            return Page.empty(page);
        }
        List<BookId> ids = jdbc.query("SELECT book_id FROM favorites WHERE user_id = ? "
                        + "ORDER BY added_at DESC, book_id DESC LIMIT ? OFFSET ?",
                row -> BookId.of(row.getLong(1)), customerId, page.size(), page.offset());
        return new Page<>(ids, page, total);
    }

    @Override
    public boolean exists(CustomerId customerId, BookId bookId) {
        return jdbc.exists("SELECT 1 FROM favorites WHERE user_id = ? AND book_id = ?",
                customerId, bookId);
    }
}
