package com.bookstore.catalog.api;

import java.util.List;

/**
 * The catalog's numbers for the administrator's overview (ADM-8).
 *
 * @param totalBooks   every book, deactivated ones too
 * @param activeBooks  books on sale
 * @param lowStockBelow the threshold the list below was made with
 * @param lowStock     books on sale with fewer copies than the threshold, fewest first
 */
public record CatalogStats(long totalBooks, long activeBooks, int lowStockBelow, List<AdminBookRow> lowStock) {

    /** Copies the list. */
    public CatalogStats {
        lowStock = List.copyOf(lowStock);
    }
}
