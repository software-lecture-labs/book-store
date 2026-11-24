package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated settled-order data used to assemble an administrator's report.
 *
 * @param daily       order totals grouped by date
 * @param bestSelling the highest-volume books
 * @param leastSelling the lowest-volume books that had sales
 */
public record SalesReportData(List<DailySales> daily, List<BookSales> bestSelling, List<BookSales> leastSelling) {

    /** Maximum number of books in either ranking. */
    public static final int RANKING_SIZE = 5;

    /** Copies the lists. */
    public SalesReportData {
        daily = List.copyOf(daily);
        bestSelling = List.copyOf(bestSelling);
        leastSelling = List.copyOf(leastSelling);
    }

    /** A day's charged total, including shipping. */
    public record DailySales(LocalDate date, Money revenue, long orderCount, long unitsSold) {
    }

    /** Book revenue excludes shipping because shipping is an order-level amount. */
    public record BookSales(BookId bookId, String title, long unitsSold, Money revenue) {
    }
}
