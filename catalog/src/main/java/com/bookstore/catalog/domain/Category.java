package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.validation.Guard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * A node in the two-level category tree, for example <em>Literature → Novel</em>.
 *
 * <p>A category is either a root, which may have subcategories, or a subcategory of a root, which
 * may not. Subcategories are created through their parent with {@link #addSubcategory(String)}, so
 * the links in both directions always agree.
 */
public class Category extends Entity<CategoryId> {

    /** Longest allowed name. */
    public static final int MAX_NAME_LENGTH = 100;

    /** Number of levels in the tree. */
    public static final int MAX_DEPTH = 2;

    private String name;
    private final Category parent;
    private final List<Category> children = new ArrayList<>();

    private Category(String name, Category parent) {
        this.name = validName(name);
        this.parent = parent;
    }

    private Category(CategoryId id, int version, String name, Category parent) {
        super(id, version);
        this.name = validName(name);
        this.parent = parent;
    }

    /**
     * Creates a new top-level category.
     *
     * @param name the name, for example {@code "Literature"}
     * @return the category, not yet stored
     */
    public static Category root(String name) {
        return new Category(name, null);
    }

    /**
     * Reconstitutes a top-level category loaded from storage.
     *
     * @param id      the stored id
     * @param version the stored version
     * @param name    the stored name
     * @return the category, without children until they are restored
     */
    public static Category reconstituteRoot(CategoryId id, int version, String name) {
        return new Category(id, version, name, null);
    }

    /**
     * Reconstitutes a subcategory loaded from storage and attaches it to its parent.
     *
     * @param id      the stored id
     * @param version the stored version
     * @param name    the stored name
     * @param parent  the already restored parent, which must be a root
     * @return the subcategory
     */
    public static Category reconstituteChild(CategoryId id, int version, String name, Category parent) {
        requireCanHaveChildren(parent);
        Category child = new Category(id, version, name, parent);
        parent.children.add(child);
        return child;
    }

    /**
     * Creates a subcategory under this category.
     *
     * @param childName the subcategory's name, unique among its siblings
     * @return the new subcategory, not yet stored
     * @throws ValidationException if this category is itself a subcategory, or the name is taken
     */
    public Category addSubcategory(String childName) {
        requireCanHaveChildren(this);
        String validName = validName(childName);
        boolean taken = children.stream().anyMatch(c -> c.name.equalsIgnoreCase(validName));
        Guard.require(!taken, "name", "a subcategory with this name already exists");
        Category child = new Category(validName, this);
        children.add(child);
        return child;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Changes the name.
     *
     * @param newName the new name
     */
    public void rename(String newName) {
        this.name = validName(newName);
    }

    /**
     * Returns the parent category.
     *
     * @return the parent, or empty for a root
     */
    public Optional<Category> parent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Tells whether this is a top-level category.
     *
     * @return {@code true} for a root
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns the direct subcategories.
     *
     * @return an unmodifiable view, in creation order
     */
    public List<Category> children() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns the level of this category: 1 for a root, 2 for a subcategory.
     *
     * @return the depth, computed recursively from the parent chain
     */
    public int depth() {
        return parent == null ? 1 : parent.depth() + 1;
    }

    /**
     * Returns the names from the root down to this category, for breadcrumbs.
     *
     * @return for example {@code ["Literature", "Novel"]}
     */
    public List<String> path() {
        List<String> names = new ArrayList<>();
        collectPath(this, names);
        return List.copyOf(names);
    }

    /**
     * Returns this category and every category below it, visiting the tree level by level
     * (breadth-first). Used when listing a category should include its subcategories' books.
     *
     * @return this category first, then its descendants
     */
    public List<Category> selfAndDescendants() {
        List<Category> result = new ArrayList<>();
        Deque<Category> queue = new ArrayDeque<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            Category current = queue.poll();
            result.add(current);
            queue.addAll(current.children);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Category[" + String.join(" > ", path()) + "]";
    }

    private static void collectPath(Category category, List<String> names) {
        if (category.parent != null) {
            collectPath(category.parent, names);
        }
        names.add(category.name);
    }

    private static void requireCanHaveChildren(Category category) {
        Guard.requireNonNull(category, "parent");
        if (category.depth() >= MAX_DEPTH) {
            throw new ValidationException("parent", "categories can only be " + MAX_DEPTH + " levels deep");
        }
    }

    private static String validName(String name) {
        return Guard.requireText(name, "name", MAX_NAME_LENGTH);
    }
}
