package com.bookstore.sales.api;

import com.bookstore.shared.kernel.money.Money;

import java.time.LocalDate;
import java.util.List;

/**
 * Sales paid, shipped or delivered in the last 30 UTC calendar days. Revenue includes shipping;
 * book rankings include books with at least one sale in the period.
 *
 * @param from             the first included date
 * @param to               the last included date
 * @param revenue          the total charged, including shipping
 * @param orderCount       the number of included orders
 * @param unitsSold        the number of copies sold
 * @param daily            one point for each date, including days without sales
 * @param bestSelling      up to five books, most copies sold first
 * @param leastSelling     up to five books with sales, fewest copies sold first
 */
public record SalesReport(LocalDate from, LocalDate to, Money revenue, long orderCount, long unitsSold,
                          List<DailySales> daily, List<BookSales> bestSelling, List<BookSales> leastSelling) {

    /** Copies the lists. */
    public SalesReport {
        daily = List.copyOf(daily);
        bestSelling = List.copyOf(bestSelling);
        leastSelling = List.copyOf(leastSelling);
    }

    /** One day's settled sales. */
    public record DailySales(LocalDate date, Money revenue, long orderCount, long unitsSold) {
    }

    /** Sales of one book during the reporting period; shipping is not allocated to books. */
    public record BookSales(long bookId, String title, long unitsSold, Money revenue) {
    }
}
