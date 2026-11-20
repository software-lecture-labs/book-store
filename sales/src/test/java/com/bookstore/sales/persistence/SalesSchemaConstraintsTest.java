package com.bookstore.sales.persistence;

import static com.bookstore.sales.persistence.SalesDb.snapshotOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.customer.domain.Customer;
import com.bookstore.sales.domain.OrderEvent;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.PaymentMethod;
import com.bookstore.sales.domain.ProductSnapshot;
import com.bookstore.shared.jdbc.DataAccessException;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The sales tables' own constraints, checked with raw SQL that bypasses the domain: H2 here,
 * PostgreSQL in {@link PostgresSalesSchemaConstraintsTest}.
 */
class SalesSchemaConstraintsTest {

    private final SalesDb db = newDb();

    /** @return the database the constraints are checked on */
    protected SalesDb newDb() {
        return new SalesDb();
    }

    @Test
    void quantityIsCappedAtTenByTheDatabaseToo() {
        Customer customer = db.customers.customer("ayse@example.com");
        ProductSnapshot dune = snapshotOf(db.catalog.book("Dune", "100", 10));
        db.jdbc.update("INSERT INTO carts (user_id) VALUES (?)", customer.id());

        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO cart_items (user_id, book_id, sort_order, quantity, "
                + "price_when_added) VALUES (?, ?, 0, 11, 100)", customer.id(), dune.bookId()))
                .isInstanceOfSatisfying(DataAccessException.class,
                        e -> assertThat(e.isConstraintViolation()).isTrue());
    }

    @Test
    void orderStatusMustBeKnownToTheDatabase() {
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO order_statuses (code) VALUES ('CREATED')"))
                .isInstanceOfSatisfying(DataAccessException.class,
                        e -> assertThat(e.isConstraintViolation()).isTrue());
    }

    @Test
    void referenceTablesListEveryStatusAndPaymentMethodOfTheDomain() {
        assertThat(db.jdbc.query("SELECT code FROM order_statuses", row -> row.getString("code")))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
        assertThat(db.jdbc.query("SELECT code FROM payment_methods", row -> row.getString("code")))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PaymentMethod.values()).map(Enum::name).toList());
        assertThat(db.jdbc.query("SELECT code FROM order_event_types", row -> row.getString("code")))
                .containsExactlyInAnyOrderElementsOf(JdbcOrderEventOutbox.EVENT_TYPES)
                .hasSameSizeAs(OrderEvent.class.getPermittedSubclasses());
    }
}
