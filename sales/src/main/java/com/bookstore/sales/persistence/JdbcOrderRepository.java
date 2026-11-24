package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderId;
import com.bookstore.sales.domain.OrderLine;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.PaymentMethod;
import com.bookstore.sales.domain.SalesReportData;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.RecordRowMapper;
import com.bookstore.shared.jdbc.RowMapper;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Stores orders in {@code orders} and their lines in {@code order_lines}. Once placed, only an
 * order's status changes, so an update touches the {@code orders} row only.
 */
@ApplicationScoped
public class JdbcOrderRepository implements OrderRepository {

    private static final String COLUMNS = "o.id, o.version, o.order_number, o.user_id, o.status, o.payment_method, "
            + "o.shipping_cost, o.placed_at, o.status_changed_at, o.ship_recipient, o.ship_phone, o.ship_line, "
            + "o.ship_district, o.ship_city, o.ship_postal_code";
    private static final String UTC_SALE_DATE = "CAST(o.placed_at AT TIME ZONE 'UTC' AS DATE)";
    private static final String NEWEST_FIRST = " ORDER BY o.placed_at DESC, o.id DESC";

    private static final RecordRowMapper<OrderRow> ORDERS = RecordRowMapper.of(OrderRow.class);
    private static final RecordRowMapper<LineRow> LINES = RecordRowMapper.of(LineRow.class);
    private static final RowMapper<SalesReportData.DailySales> DAILY_SALES = row ->
            new SalesReportData.DailySales(row.getDate("sale_date").toLocalDate(),
                    Money.of(row.getBigDecimal("revenue")), row.getLong("order_count"), row.getLong("units_sold"));
    private static final RowMapper<SalesReportData.BookSales> BOOK_SALES = row ->
            new SalesReportData.BookSales(BookId.of(row.getLong("book_id")), row.getString("book_title"),
                    row.getLong("units_sold"), Money.of(row.getBigDecimal("revenue")));

    private final JdbcExecutor jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc runs the SQL
     */
    @Inject
    public JdbcOrderRepository(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected JdbcOrderRepository() {
        this(null);
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Optional<Order> findById(OrderId id) {
        return load("SELECT " + COLUMNS + " FROM orders o WHERE o.id = ?", id).stream().findFirst();
    }

    @Override
    public Optional<Order> findByNumber(OrderNumber number) {
        return load("SELECT " + COLUMNS + " FROM orders o WHERE o.order_number = ?", number.value()).stream()
                .findFirst();
    }

    @Override
    public Page<Order> findByCustomerId(CustomerId customerId, PageRequest page) {
        return page(" WHERE o.user_id = ?", page, customerId);
    }

    @Override
    public Page<Order> findAll(PageRequest page) {
        return page("", page);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, PageRequest page) {
        return page(" WHERE o.status = ?", page, status);
    }

    @Override
    public SalesReportData salesReport(Instant fromInclusive, Instant untilExclusive) {
        Object[] range = new Object[] {OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED,
                fromInclusive, untilExclusive};
        String dailySql = """
                SELECT %s AS sale_date,
                       COUNT(*) AS order_count,
                       SUM(o.shipping_cost + totals.subtotal) AS revenue,
                       SUM(totals.units_sold) AS units_sold
                FROM orders o
                JOIN (SELECT order_id, SUM(unit_price * quantity) AS subtotal, SUM(quantity) AS units_sold
                      FROM order_lines GROUP BY order_id) totals ON totals.order_id = o.id
                WHERE o.status IN (?, ?, ?) AND o.placed_at >= ? AND o.placed_at < ?
                GROUP BY %s
                ORDER BY sale_date
                """.formatted(UTC_SALE_DATE, UTC_SALE_DATE);
        List<SalesReportData.DailySales> daily = jdbc.query(dailySql, DAILY_SALES, range);
        List<SalesReportData.BookSales> bestSelling = bookSales(fromInclusive, untilExclusive, true);
        List<SalesReportData.BookSales> leastSelling = bookSales(fromInclusive, untilExclusive, false);
        return new SalesReportData(daily, bestSelling, leastSelling);
    }

    @Override
    public boolean hasDeliveredBook(CustomerId customerId, BookId bookId) {
        return jdbc.exists("SELECT 1 FROM orders o JOIN order_lines l ON l.order_id = o.id "
                + "WHERE o.user_id = ? AND o.status = ? AND l.book_id = ? LIMIT 1",
                customerId, OrderStatus.DELIVERED, bookId);
    }

    // ---------------------------------------------------------------- commands

    @Override
    public Order save(Order order) {
        if (order.isNew()) {
            insert(order);
        } else {
            jdbc.updateVersioned("Order " + order.number(),
                    "UPDATE orders SET status = ?, status_changed_at = ?, version = version + 1 "
                            + "WHERE id = ? AND version = ?",
                    order.status(), order.statusChangedAt(), order.id(), order.version());
            order.markPersisted(order.id(), order.version() + 1);
        }
        return order;
    }

    private void insert(Order order) {
        Address a = order.shippingAddress();
        long id = jdbc.insert("INSERT INTO orders (order_number, user_id, status, payment_method, shipping_cost, "
                        + "placed_at, status_changed_at, ship_recipient, ship_phone, ship_line, ship_district, ship_city, "
                        + "ship_postal_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                order.number().value(), order.customerId(), order.status(), order.paymentMethod(),
                order.shippingCost(), order.placedAt(), order.statusChangedAt(), a.recipient(), a.phone(), a.line(),
                a.district(), a.city(), a.postalCode());
        order.markPersisted(OrderId.of(id), 0);

        List<Object[]> rows = new ArrayList<>();
        int lineNo = 1;
        for (OrderLine line : order.lines()) {
            rows.add(new Object[] {id, lineNo++, line.bookId(), line.bookTitle(), line.unitPrice(), line.quantity()});
        }
        jdbc.batch("INSERT INTO order_lines (order_id, line_no, book_id, book_title, unit_price, quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?)", rows);
    }

    // ---------------------------------------------------------------- loading

    private Page<Order> page(String where, PageRequest page, Object... params) {
        long total = jdbc.queryForLong("SELECT COUNT(*) FROM orders o" + where, params);
        if (total == 0) {
            return Page.empty(page);
        }
        Object[] pageParams = Stream.concat(Stream.of(params), Stream.of(page.size(), page.offset())).toArray();
        List<Order> orders = load("SELECT " + COLUMNS + " FROM orders o" + where + NEWEST_FIRST + " LIMIT ? OFFSET ?",
                pageParams);
        return new Page<>(orders, page, total);
    }

    private List<SalesReportData.BookSales> bookSales(Instant fromInclusive, Instant untilExclusive,
                                                      boolean bestSelling) {
        String direction = bestSelling ? "DESC" : "ASC";
        String sql = """
                SELECT l.book_id, MAX(l.book_title) AS book_title,
                       SUM(l.quantity) AS units_sold, SUM(l.unit_price * l.quantity) AS revenue
                FROM order_lines l
                JOIN orders o ON o.id = l.order_id
                WHERE o.status IN (?, ?, ?) AND o.placed_at >= ? AND o.placed_at < ?
                GROUP BY l.book_id
                ORDER BY units_sold %s, revenue %s, book_title, l.book_id
                LIMIT ?
                """.formatted(direction, direction);
        return jdbc.query(sql, BOOK_SALES, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED,
                fromInclusive, untilExclusive, SalesReportData.RANKING_SIZE);
    }

    private List<Order> load(String sql, Object... params) {
        List<OrderRow> headers = jdbc.query(sql, ORDERS, params);
        if (headers.isEmpty()) {
            return List.of();
        }
        List<Long> ids = headers.stream().map(OrderRow::id).toList();
        Map<Long, List<OrderLine>> lines = jdbc.query("SELECT order_id, line_no, book_id, book_title, unit_price, "
                                + "quantity FROM order_lines WHERE order_id IN (" + JdbcExecutor.placeholders(ids.size())
                                + ") ORDER BY order_id, line_no",
                        LINES, JdbcExecutor.params(ids))
                .stream()
                .collect(Collectors.groupingBy(LineRow::orderId,
                        Collectors.mapping(LineRow::toLine, Collectors.toList())));
        return headers.stream().map(header -> header.toOrder(lines.getOrDefault(header.id(), List.of()))).toList();
    }

    /** One {@code orders} row, read by reflection. */
    private record OrderRow(long id, int version, String orderNumber, long userId, OrderStatus status,
                            PaymentMethod paymentMethod, Money shippingCost, Instant placedAt,
                            Instant statusChangedAt, String shipRecipient, String shipPhone, String shipLine,
                            String shipDistrict, String shipCity, String shipPostalCode) {

        Order toOrder(List<OrderLine> lines) {
            Address address = new Address(shipRecipient, shipPhone, shipLine, shipDistrict, shipCity, shipPostalCode);
            return Order.reconstitute(OrderId.of(id), version, new OrderNumber(orderNumber), CustomerId.of(userId), lines, address, paymentMethod,
                    shippingCost, status, placedAt, statusChangedAt);
        }
    }

    /** One {@code order_lines} row, read by reflection. */
    private record LineRow(long orderId, int lineNo, long bookId, String bookTitle, Money unitPrice, int quantity) {

        OrderLine toLine() {
            return new OrderLine(BookId.of(bookId), bookTitle, unitPrice, quantity);
        }
    }
}
