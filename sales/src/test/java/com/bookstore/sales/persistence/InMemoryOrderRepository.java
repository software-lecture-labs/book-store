package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderId;
import com.bookstore.sales.domain.OrderLine;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.SalesReportData;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.InMemoryStore;

import java.util.Comparator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Orders in a map. Lists are sorted newest first. */
public class InMemoryOrderRepository extends InMemoryStore<OrderId, Order> implements OrderRepository {

    /** Creates an empty repository. */
    public InMemoryOrderRepository() {
        super(OrderId::new);
    }

    @Override
    protected Order copyOf(Order order) {
        return Order.reconstitute(order.id(), order.version(), order.number(), order.customerId(), order.lines(),
                order.shippingAddress(), order.paymentMethod(), order.shippingCost(), order.status(), order.placedAt(),
                order.statusChangedAt());
    }

    private static final Comparator<Order> NEWEST_FIRST =
            Comparator.comparing(Order::placedAt).thenComparing(order -> order.id().value()).reversed();

    @Override
    public Optional<Order> findByNumber(OrderNumber number) {
        return all().stream().filter(order -> order.number().equals(number)).findFirst();
    }

    @Override
    public Page<Order> findByCustomerId(CustomerId customerId, PageRequest page) {
        return newestFirst(order -> order.isOwnedBy(customerId), page);
    }

    @Override
    public Page<Order> findAll(PageRequest page) {
        return newestFirst(order -> true, page);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, PageRequest page) {
        return newestFirst(order -> order.status() == status, page);
    }

    @Override
    public boolean hasDeliveredBook(CustomerId customerId, BookId bookId) {
        return all().stream().anyMatch(order -> order.isOwnedBy(customerId)
                && order.status() == OrderStatus.DELIVERED
                && order.contains(bookId));
    }

    @Override
    public SalesReportData salesReport(Instant fromInclusive, Instant untilExclusive) {
        List<Order> matching = all().stream()
                .filter(order -> isSettled(order.status()))
                .filter(order -> !order.placedAt().isBefore(fromInclusive) && order.placedAt().isBefore(untilExclusive))
                .toList();
        Map<LocalDate, DailyAccumulator> daily = new HashMap<>();
        Map<BookId, BookAccumulator> books = new HashMap<>();
        for (Order order : matching) {
            LocalDate date = order.placedAt().atZone(ZoneOffset.UTC).toLocalDate();
            daily.computeIfAbsent(date, ignored -> new DailyAccumulator()).add(order);
            order.lines().forEach(line -> books.computeIfAbsent(line.bookId(),
                    ignored -> new BookAccumulator(line.bookTitle())).add(line));
        }

        List<SalesReportData.DailySales> dailySales = daily.entrySet().stream()
                .map(entry -> new SalesReportData.DailySales(entry.getKey(), entry.getValue().revenue,
                        entry.getValue().orderCount, entry.getValue().unitsSold))
                .sorted(Comparator.comparing(SalesReportData.DailySales::date))
                .toList();
        List<SalesReportData.BookSales> bestSelling = books.entrySet().stream()
                .map(entry -> new SalesReportData.BookSales(entry.getKey(), entry.getValue().title,
                        entry.getValue().unitsSold, entry.getValue().revenue))
                .sorted(Comparator.comparingLong(SalesReportData.BookSales::unitsSold).reversed()
                        .thenComparing(SalesReportData.BookSales::revenue, Comparator.reverseOrder())
                        .thenComparing(SalesReportData.BookSales::title)
                        .thenComparing(book -> book.bookId().value()))
                .limit(SalesReportData.RANKING_SIZE)
                .toList();
        List<SalesReportData.BookSales> leastSelling = books.entrySet().stream()
                .map(entry -> new SalesReportData.BookSales(entry.getKey(), entry.getValue().title,
                        entry.getValue().unitsSold, entry.getValue().revenue))
                .sorted(Comparator.comparingLong(SalesReportData.BookSales::unitsSold)
                        .thenComparing(SalesReportData.BookSales::revenue)
                        .thenComparing(SalesReportData.BookSales::title)
                        .thenComparing(book -> book.bookId().value()))
                .limit(SalesReportData.RANKING_SIZE)
                .toList();
        return new SalesReportData(dailySales, bestSelling, leastSelling);
    }

    private Page<Order> newestFirst(Predicate<Order> condition, PageRequest page) {
        List<Order> matches = where(condition).stream().sorted(NEWEST_FIRST).toList();
        return Page.fromList(matches, page);
    }

    private static boolean isSettled(OrderStatus status) {
        return status == OrderStatus.PAID || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED;
    }

    private static final class DailyAccumulator {
        private Money revenue = Money.ZERO;
        private long orderCount;
        private long unitsSold;

        void add(Order order) {
            revenue = revenue.plus(order.total());
            orderCount++;
            unitsSold += order.totalQuantity();
        }
    }

    private static final class BookAccumulator {
        private final String title;
        private Money revenue = Money.ZERO;
        private long unitsSold;

        BookAccumulator(String title) {
            this.title = title;
        }

        void add(OrderLine line) {
            revenue = revenue.plus(line.lineTotal());
            unitsSold += line.quantity();
        }
    }
}
