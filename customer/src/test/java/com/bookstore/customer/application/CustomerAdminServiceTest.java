package com.bookstore.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.bookstore.customer.api.CustomerSummary;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.PersonName;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.PageRequest;

import org.junit.jupiter.api.Test;

class CustomerAdminServiceTest {

    private final CustomerShop shop = new CustomerShop();
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final CustomerId admin = shop.customers.save(Customer.createAdmin(new Email("admin@example.com"),
            new PersonName("Site", "Yöneticisi"), "{fake}" + CustomerShop.PASSWORD, CustomerShop.CLOCK)).id();

    @Test
    void listsEveryAccountWithItsRoleAndState() {
        assertThat(shop.admin.customers(PageRequest.first()).content())
                .extracting(CustomerSummary::email, CustomerSummary::role, CustomerSummary::active)
                .containsExactlyInAnyOrder(
                        tuple("ayse@example.com", "CUSTOMER", true),
                        tuple("admin@example.com", "ADMIN", true));
    }

    @Test
    void aSwitchedOffAccountCannotSignInUntilItIsSwitchedOnAgain() {
        assertThat(shop.admin.setActive(ayse, false).active()).isFalse();

        assertThat(shop.accounts.authenticate("ayse@example.com", CustomerShop.PASSWORD)).isEmpty();
        assertThat(shop.accounts.isActive(ayse)).isFalse();

        shop.admin.setActive(ayse, true);
        assertThat(shop.accounts.authenticate("ayse@example.com", CustomerShop.PASSWORD)).isPresent();
        assertThat(shop.accounts.isActive(ayse)).isTrue();
    }

    @Test
    void administratorsCannotBeSwitchedOffAndUnknownAccountsAreNotFound() {
        assertThatThrownBy(() -> shop.admin.setActive(admin, false))
                .isInstanceOfSatisfying(RuleViolationException.class,
                        e -> assertThat(e.code()).isEqualTo("customer.admin-protected"));
        assertThatThrownBy(() -> shop.admin.setActive(CustomerId.of(999), false))
                .isInstanceOf(NotFoundException.class);
        assertThat(shop.accounts.isActive(CustomerId.of(999))).isFalse();
    }
}
