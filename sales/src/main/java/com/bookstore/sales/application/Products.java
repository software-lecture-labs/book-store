package com.bookstore.sales.application;

import com.bookstore.catalog.api.BookOffer;
import com.bookstore.catalog.api.StockFacade;
import com.bookstore.sales.domain.ProductSnapshot;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns the catalog's {@link BookOffer}s into the sales module's own {@link ProductSnapshot}s
 * (Mapper, ADR-0003): the sales domain never sees a catalog type.
 */
final class Products {

    private Products() {
    }

    /**
     * Loads the current state of several books.
     *
     * @param stock   the catalog's stock facade
     * @param bookIds the books
     * @return snapshots keyed by book id; books that do not exist are missing
     */
    static Map<BookId, ProductSnapshot> load(StockFacade stock, Collection<BookId> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return stock.offers(bookIds).stream()
                .map(Products::snapshotOf)
                .collect(Collectors.toMap(ProductSnapshot::bookId, Function.identity()));
    }

    /**
     * Loads one book that customers can see.
     *
     * @param stock  the catalog's stock facade
     * @param bookId the book
     * @return the snapshot
     * @throws NotFoundException if the book does not exist or is no longer sold
     */
    static ProductSnapshot loadActive(StockFacade stock, BookId bookId) {
        ProductSnapshot product = load(stock, java.util.List.of(bookId)).get(bookId);
        if (product == null || !product.active()) {
            throw new NotFoundException("Book", bookId);
        }
        return product;
    }

    static ProductSnapshot snapshotOf(BookOffer offer) {
        return new ProductSnapshot(offer.bookId(), offer.title(), offer.authorNames(), offer.price(),
                offer.availableStock(), offer.active());
    }
}
