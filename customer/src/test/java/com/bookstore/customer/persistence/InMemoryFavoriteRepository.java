package com.bookstore.customer.persistence;

import com.bookstore.customer.domain.FavoriteRepository;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryFavoriteRepository implements FavoriteRepository {
    private final Map<CustomerId, LinkedHashMap<BookId, Long>> favorites = new LinkedHashMap<>();
    private long sequence;

    @Override
    public void add(CustomerId customerId, BookId bookId) {
        favorites.computeIfAbsent(customerId, id -> new LinkedHashMap<>()).put(bookId, ++sequence);
    }

    @Override
    public boolean remove(CustomerId customerId, BookId bookId) {
        Map<BookId, Long> ids = favorites.get(customerId);
        return ids != null && ids.remove(bookId) != null;
    }

    @Override
    public Page<BookId> findByCustomer(CustomerId customerId, PageRequest page) {
        Map<BookId, Long> ids = favorites.getOrDefault(customerId, new LinkedHashMap<>());
        return Page.fromList(ids.entrySet().stream()
                .sorted(Map.Entry.<BookId, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey).toList(), page);
    }

    @Override
    public boolean exists(CustomerId customerId, BookId bookId) {
        Map<BookId, Long> ids = favorites.get(customerId);
        return ids != null && ids.containsKey(bookId);
    }
}
