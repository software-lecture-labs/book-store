package com.bookstore.sales.application;

import com.bookstore.catalog.api.StockFacade;
import com.bookstore.sales.api.OrderAdminFacade;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.api.SalesReport;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderEventOutbox;
import com.bookstore.sales.domain.OrderNumber;
import com.bookstore.sales.domain.OrderRepository;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.SalesReportData;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.transaction.TransactionRunner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Order management for administrators (ADM-5): listing every order and moving orders through
 * their statuses. Customers are notified of every change (ORD-9).
 */
@ApplicationScoped
public class OrderAdminService implements OrderAdminFacade {

    private static final int REPORT_DAYS = 30;

    private final OrderRepository orders;
    private final StockFacade stock;
    private final TransactionRunner transactions;
    private final OrderEventOutbox outbox;
    private final OrderEventRelay relay;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param orders       where orders are stored
     * @param stock        the catalog's stock, for returning copies on cancellation
     * @param transactions runs work in a transaction
     * @param outbox       keeps the events of a changed order in its transaction
     * @param relay        hands the events on once the order is stored
     * @param clock        the clock giving status change times
     */
    @Inject
    public OrderAdminService(OrderRepository orders, StockFacade stock, TransactionRunner transactions,
                             OrderEventOutbox outbox, OrderEventRelay relay, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.transactions = transactions;
        this.outbox = outbox;
        this.relay = relay;
        this.clock = clock;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected OrderAdminService() {
        this(null, null, null, null, null, null);
    }

    /**
     * Returns one page of all orders, newest first.
     *
     * @param page which page to return
     * @return the page
     */
    @Override
    public Page<OrderSummary> orders(PageRequest page) {
        return orders.findAll(page).map(SalesMapper::summaryOf);
    }

    /**
     * Returns one page of the orders in one status, newest first.
     *
     * @param status the status
     * @param page   which page to return
     * @return the page
     */
    @Override
    public Page<OrderSummary> ordersIn(String status, PageRequest page) {
        return orders.findByStatus(SalesMapper.statusOf(status), page).map(SalesMapper::summaryOf);
    }

    /**
     * Returns any order.
     *
     * @param orderNumber the order number
     * @return the order
     */
    @Override
    public OrderDetails order(String orderNumber) {
        return SalesMapper.detailsOf(requireOrder(new OrderNumber(orderNumber)));
    }

    /**
     * Moves an order to another status. Cancelling puts the copies back into stock (ORD-7).
     *
     * @param orderNumber the order number
     * @param target      the new status
     * @return the updated order
     * @throws ConflictException if the change is not allowed from the current status (ORD-6)
     */
    @Override
    public OrderDetails changeStatus(String orderNumber, String target) {
        OrderStatus status = SalesMapper.statusOf(target);
        OrderNumber number = new OrderNumber(orderNumber);
        Order order = transactions.inTransaction(() -> {
            Order found = requireOrder(number);
            switch (status) {
                case PAID -> found.markPaid(clock);
                case SHIPPED -> found.ship(clock);
                case DELIVERED -> found.deliver(clock);
                case CANCELLED -> cancel(found);
                case CREATED -> found.status().requireCanMoveTo(OrderStatus.CREATED);
            }
            Order saved = orders.save(found);
            outbox.append(found.pullEvents());
            return saved;
        });
        relay.relayQuietly();
        return SalesMapper.detailsOf(order);
    }

    @Override
    public SalesReport salesReport() {
        LocalDate to = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate from = to.minusDays(REPORT_DAYS - 1);
        SalesReportData data = orders.salesReport(from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        Map<LocalDate, SalesReportData.DailySales> byDate = new HashMap<>();
        data.daily().forEach(day -> byDate.put(day.date(), day));
        List<SalesReport.DailySales> daily = new ArrayList<>(REPORT_DAYS);
        for (int offset = 0; offset < REPORT_DAYS; offset++) {
            LocalDate date = from.plusDays(offset);
            SalesReportData.DailySales totals = byDate.get(date);
            daily.add(totals == null
                    ? new SalesReport.DailySales(date, Money.ZERO, 0, 0)
                    : new SalesReport.DailySales(date, totals.revenue(), totals.orderCount(), totals.unitsSold()));
        }

        Money revenue = Money.sum(daily.stream().map(SalesReport.DailySales::revenue));
        long orderCount = daily.stream().mapToLong(SalesReport.DailySales::orderCount).sum();
        long unitsSold = daily.stream().mapToLong(SalesReport.DailySales::unitsSold).sum();
        return new SalesReport(from, to, revenue, orderCount, unitsSold, daily,
                data.bestSelling().stream().map(OrderAdminService::bookSalesOf).toList(),
                data.leastSelling().stream().map(OrderAdminService::bookSalesOf).toList());
    }

    private void cancel(Order order) {
        order.cancel(clock);
        stock.release(order.quantitiesByBook());
    }

    private static SalesReport.BookSales bookSalesOf(SalesReportData.BookSales sales) {
        return new SalesReport.BookSales(sales.bookId().value(), sales.title(), sales.unitsSold(), sales.revenue());
    }

    private Order requireOrder(OrderNumber number) {
        return orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Order", number));
    }
}
