package com.bookstore.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * Stores and finds categories. Categories are always loaded as a whole tree, so that a
 * category's parent and children are available.
 */
public interface CategoryRepository {

    /**
     * Returns the top-level categories with their subcategories attached.
     *
     * @return the roots, sorted by name
     */
    List<Category> findRoots();

    /**
     * Finds a category anywhere in the tree.
     *
     * @param id the id
     * @return the category with its parent and children attached, or empty
     */
    Optional<Category> findById(CategoryId id);

    /**
     * Inserts a new category or updates a stored one. A new subcategory requires its parent to be
     * stored first.
     *
     * @param category the category
     * @return the same category, with its id and version updated
     */
    Category save(Category category);

    /**
     * Deletes a category. The caller must first check that it has no books and no subcategories.
     *
     * @param category a stored category
     */
    void delete(Category category);
}
