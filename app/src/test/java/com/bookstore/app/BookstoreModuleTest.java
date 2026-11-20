package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.persistence.CatalogDb;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.api.RegistrationCommand;
import com.bookstore.customer.application.CustomerShop;
import com.bookstore.review.api.ReviewCommand;
import com.bookstore.sales.api.CartRef;
import com.bookstore.sales.api.CartView;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.PlaceOrderCommand;
import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.JdbcTransactionRunner;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.TestAddresses;
import com.bookstore.shared.kernel.testing.TestClock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * Smoke test of the composition root: an empty database is migrated through the module, and one
 * customer's whole journey runs through the modules' façades only.
 */
class BookstoreModuleTest {

    private final JdbcDataSource dataSource = emptyDatabase();
    private final List<DomainEvent> notified = new ArrayList<>();
    private final BookstoreModule bookstore = new BookstoreModule(dataSource, new BookstoreModule.Adapters(
            TestClock.CLOCK, approveEverything(), new CustomerShop.PrefixPasswordHasher(), notified::add));

    @Test
    void migratesAnEmptyDatabaseOnce() {
        assertThat(bookstore.migrate()).isPositive();
        assertThat(bookstore.migrate()).isZero();
    }

    @Test
    void aCustomerFindsBuysAndReviewsABook() {
        bookstore.migrate();
        Book dune = catalogFixture().book("Dune", "350.00", 5);

        // A visitor finds the book and puts it in a guest cart.
        List<BookSummary> found = bookstore.catalog()
                .search(BookSearchQuery.everything().withText("dune"), PageRequest.first()).content();
        assertThat(found).extracting(BookSummary::title).containsExactly("Dune");
        GuestCart guest = new GuestCart();
        bookstore.cart().add(CartRef.guest(guest), found.getFirst().id(), 2);
        assertThat(guest.totalQuantity()).isEqualTo(2);

        // The visitor registers, saves an address and the guest cart is merged.
        CustomerView ayse = bookstore.accounts()
                .register(new RegistrationCommand("ayse@example.com", "Ayşe", "Yılmaz", CustomerShop.PASSWORD));
        CustomerId ayseId = ayse.id();
        long home = bookstore.accounts().addAddress(ayseId, "Ev", TestAddresses.home(), true)
                .addresses().getFirst().id();
        CartView merged = bookstore.cart().mergeGuestCart(ayseId, guest);
        assertThat(merged.totalQuantity()).isEqualTo(2);

        // The order is paid by card, shipped and delivered.
        OrderDetails order = bookstore.checkout().placeOrder(new PlaceOrderCommand(ayseId, home, "CREDIT_CARD"));
        assertThat(order.status()).isEqualTo("PAID");
        assertThat(order.total()).isEqualTo(Money.of("700.00"));
        assertThat(bookstore.orders().myOrders(ayseId, PageRequest.first()).totalElements()).isEqualTo(1);
        bookstore.orderAdmin().changeStatus(order.orderNumber(), "SHIPPED");
        bookstore.orderAdmin().changeStatus(order.orderNumber(), "DELIVERED");
        assertThat(notified).as("placed, paid, shipped, delivered").hasSize(4);

        // Having received it, the customer reviews the book.
        assertThat(bookstore.reviews().canReview(ayseId, dune.id())).isTrue();
        bookstore.reviews().write(new ReviewCommand(ayseId, dune.id(), 5, "Harika"));
        assertThat(bookstore.catalog().book(dune.id()).ratingCount()).isEqualTo(1);
        assertThat(bookstore.reviews().ratingBreakdown(dune.id()).count(5)).isEqualTo(1);
        assertThat(bookstore.catalog().book(dune.id()).stock()).isEqualTo(3);
    }

    private CatalogFixture catalogFixture() {
        JdbcTransactionRunner transactions = new JdbcTransactionRunner(dataSource);
        return new CatalogFixture(new CatalogDb(new TestDatabase(dataSource, transactions,
                new JdbcExecutor(transactions))));
    }

    private static JdbcDataSource emptyDatabase() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        h2.setUser("sa");
        return h2;
    }

    private static PaymentGateway approveEverything() {
        return (number, amount) -> new PaymentGateway.PaymentResult.Approved("TX-" + number.value());
    }

    /** Stores books the way an administrator will once the catalog has an admin façade (Phase 8). */
    private record CatalogFixture(CatalogDb db) {

        Book book(String title, String price, int stock) {
            return db.book(title, price, stock);
        }
    }
}
