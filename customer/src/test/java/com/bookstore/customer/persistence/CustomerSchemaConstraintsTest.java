package com.bookstore.customer.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.Role;
import com.bookstore.shared.jdbc.DataAccessException;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The customer tables' own constraints, checked with raw SQL that bypasses the domain: H2 here,
 * PostgreSQL in {@link PostgresCustomerSchemaConstraintsTest}.
 */
class CustomerSchemaConstraintsTest {

    private final CustomerDb db = newDb();

    /** @return the database the constraints are checked on */
    protected CustomerDb newDb() {
        return new CustomerDb();
    }

    @Test
    void theRolesTableListsEveryRoleOfTheDomain() {
        assertThat(db.jdbc.query("SELECT code FROM roles", row -> row.getString("code")))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(Role.values()).map(Role::name).toList());
    }

    @Test
    void roleMustBeKnownToTheDatabase() {
        Customer customer = db.customer("ayse@example.com");

        assertThatThrownBy(() -> db.jdbc.update("UPDATE users SET role = 'HACKER' WHERE id = ?", customer.id()))
                .isInstanceOfSatisfying(DataAccessException.class,
                        e -> assertThat(e.isConstraintViolation()).isTrue());
        db.jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", customer.id());
        assertThat(db.customers.findById(customer.id()).orElseThrow().isAdmin()).isTrue();
    }
}
