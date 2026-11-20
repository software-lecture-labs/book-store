package com.bookstore.catalog.api;

import java.util.List;

/**
 * One category in the navigation tree, with its subcategories.
 *
 * @param id       the category's id
 * @param name     the name
 * @param children the subcategories, by name
 */
public record CategoryNode(long id, String name, List<CategoryNode> children) {

    /** Copies the children. */
    public CategoryNode {
        children = List.copyOf(children);
    }
}
