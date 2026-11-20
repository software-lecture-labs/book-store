package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.CategoryId;
import com.bookstore.catalog.domain.CategoryRepository;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.testing.InMemoryStore;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The category tree kept as rows, like the {@code categories} table: every read rebuilds a fresh
 * tree, so callers never share objects with the store (ADR-0008).
 */
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<CategoryId, Row> rows = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public synchronized List<Category> findRoots() {
        return loadTree().values().stream()
                .filter(Category::isRoot)
                .sorted(Comparator.comparing(Category::name))
                .toList();
    }

    @Override
    public synchronized Optional<Category> findById(CategoryId id) {
        return Optional.ofNullable(loadTree().get(id));
    }

    @Override
    public synchronized Category save(Category category) {
        CategoryId parentId = category.parent().map(parent -> {
            if (parent.isNew()) {
                throw new IllegalStateException("Store the parent category first");
            }
            return parent.id();
        }).orElse(null);
        boolean nameTaken = rows.values().stream().anyMatch(row -> Objects.equals(row.parentId(), parentId)
                && row.name().equals(category.name()) && (category.isNew() || !row.id().equals(category.id())));
        if (nameTaken) {
            throw new ConflictException("category.name-taken",
                    "A category named '" + category.name() + "' already exists here");
        }
        if (category.isNew()) {
            category.markPersisted(CategoryId.of(sequence.incrementAndGet()), 0);
        } else {
            Row stored = rows.get(category.id());
            if (stored == null || stored.version() != category.version()) {
                throw new ConflictException(InMemoryStore.CONCURRENT_MODIFICATION,
                        "Category " + category.id() + " was changed or deleted by someone else");
            }
            category.markPersisted(category.id(), category.version() + 1);
        }
        rows.put(category.id(), new Row(category.id(), category.version(), parentId, category.name()));
        return category;
    }

    @Override
    public synchronized void delete(Category category) {
        rows.remove(category.id());
    }

    /** Rebuilds the whole tree from the rows, parents before children. */
    private Map<CategoryId, Category> loadTree() {
        Map<CategoryId, Category> byId = new LinkedHashMap<>();
        List<Row> ordered = rows.values().stream()
                .sorted(Comparator.comparing((Row row) -> row.parentId() == null ? 0 : 1).thenComparing(Row::name))
                .toList();
        for (Row row : ordered) {
            Category category = row.parentId() == null
                    ? Category.reconstituteRoot(row.id(), row.version(), row.name())
                    : Category.reconstituteChild(row.id(), row.version(), row.name(), byId.get(row.parentId()));
            byId.put(category.id(), category);
        }
        return byId;
    }

    private record Row(CategoryId id, int version, CategoryId parentId, String name) {
    }
}
