package com.bookstore.customer.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.customer.domain.AddressId;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.Email;
import com.bookstore.customer.domain.PersonName;
import com.bookstore.customer.domain.Role;
import com.bookstore.customer.domain.SavedAddress;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.TestAddresses;
import com.bookstore.shared.kernel.testing.TestClock;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The contract of the customer repository (ADR-0008): the in-memory, H2 and PostgreSQL
 * implementations must all pass these tests.
 */
abstract class CustomerRepositoryContract {

    protected final CustomerDb db = newDb();

    /** @return a fresh, empty repository of the implementation under test */
    protected abstract CustomerDb newDb();

    @Test
    void customerAndAddressesSurviveTheRoundTrip() {
        Customer saved = db.customer("Ayse@Example.com");

        Customer loaded = db.customers.findByEmail(Email.of("ayse@example.com")).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.name()).isEqualTo(new PersonName("Ayşe", "Yılmaz"));
        assertThat(loaded.passwordHash()).isEqualTo("{hash}pw");
        assertThat(loaded.role()).isEqualTo(Role.CUSTOMER);
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.registeredAt()).isEqualTo(TestClock.NOW);
        assertThat(loaded.addresses()).singleElement().satisfies(a -> {
            assertThat(a.label()).isEqualTo("Ev");
            assertThat(a.address()).isEqualTo(TestAddresses.home());
        });
        assertThat(loaded.defaultAddress()).isPresent();
    }

    @Test
    void addressBookChangesAreStored() {
        Customer customer = db.customer("ayse@example.com");
        AddressId home = customer.addresses().getFirst().id();

        customer.addAddress("İş", TestAddresses.work(), true);
        db.customers.save(customer);
        Customer reloaded = db.customers.findById(customer.id()).orElseThrow();
        assertThat(reloaded.addresses()).extracting(SavedAddress::label).containsExactly("Ev", "İş");
        assertThat(reloaded.defaultAddress()).map(SavedAddress::label).contains("İş");

        reloaded.removeAddress(reloaded.defaultAddress().orElseThrow().id());
        reloaded.changeAddress(home, "Yazlık", TestAddresses.home());
        db.customers.save(reloaded);
        Customer last = db.customers.findById(customer.id()).orElseThrow();
        assertThat(last.addresses()).extracting(SavedAddress::label).containsExactly("Yazlık");
        assertThat(last.defaultAddress()).map(SavedAddress::id).contains(home);
    }

    @Test
    void emailIsUnique() {
        db.customer("ayse@example.com");
        Customer twin = Customer.register(Email.of("AYSE@example.com"), new PersonName("A", "B"), "h", TestClock.CLOCK);

        assertThatThrownBy(() -> db.customers.save(twin))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("email.taken"));
        assertThat(db.customers.existsByEmail(Email.of("ayse@example.com"))).isTrue();
    }

    @Test
    void profileChangesAreStoredWithANewVersion() {
        Customer customer = db.customer("ayse@example.com");
        customer.rename(new PersonName("Ayşe Nur", "Kaya"));
        customer.changePasswordHash("{hash}new");
        customer.deactivate();
        db.customers.save(customer);

        Customer loaded = db.customers.findById(customer.id()).orElseThrow();
        assertThat(loaded.name().fullName()).isEqualTo("Ayşe Nur Kaya");
        assertThat(loaded.passwordHash()).isEqualTo("{hash}new");
        assertThat(loaded.isActive()).isFalse();
        assertThat(loaded.version()).isEqualTo(1);
    }

    @Test
    void listsAndLooksUpSeveralAtOnce() {
        Customer a = db.customer("a@example.com");
        Customer b = db.customer("b@example.com");

        Page<Customer> page = db.customers.findAll(PageRequest.first());

        assertThat(page.content()).extracting(c -> c.email().value()).containsExactly("b@example.com", "a@example.com");
        assertThat(db.customers.findAllById(List.of(a.id(), b.id(), CustomerId.of(999)))).hasSize(2);
    }
}
