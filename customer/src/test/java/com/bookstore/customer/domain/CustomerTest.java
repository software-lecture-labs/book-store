package com.bookstore.customer.domain;

import static com.bookstore.shared.kernel.testing.TestAddresses.home;
import static com.bookstore.shared.kernel.testing.TestClock.CLOCK;
import static com.bookstore.shared.kernel.testing.TestClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.List;

import org.junit.jupiter.api.Test;

class CustomerTest {

    private final Customer customer = Customer.register(
            Email.of("Ayse@Example.com"), new PersonName("Ayşe", "Yılmaz"), "$2a$10$hash", CLOCK);

    @Test
    void registersAsAnActiveCustomer() {
        assertThat(customer.email().value()).isEqualTo("ayse@example.com");
        assertThat(customer.role()).isEqualTo(Role.CUSTOMER);
        assertThat(customer.isAdmin()).isFalse();
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.registeredAt()).isEqualTo(NOW);
        assertThat(customer.name().fullName()).isEqualTo("Ayşe Yılmaz");
    }

    @Test
    void publicNameHidesMostOfTheLastName() {
        assertThat(new PersonName("Ayşe", "Yılmaz").publicName()).isEqualTo("Ayşe Y.");
        assertThat(new PersonName("Mehmet Ali", "Özdemir").publicName()).isEqualTo("Mehmet Ali Ö.");
        assertThat(new PersonName("Jo", "Ü").publicName()).isEqualTo("Jo Ü.");
    }

    @Test
    void adminHasTheAdminRole() {
        Customer admin = Customer.createAdmin(Email.of("admin@bookstore.local"), new PersonName("Site", "Admin"),
                "hash", CLOCK);

        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    void firstAddressBecomesTheDefault() {
        SavedAddress home = customer.addAddress("Ev", home(), false);
        SavedAddress work = customer.addAddress("İş", home(), false);

        assertThat(customer.addresses()).containsExactly(home, work);
        assertThat(customer.defaultAddress()).containsSame(home);
    }

    @Test
    void addressesAreFoundByIdAndTheDefaultMovesWhenItIsRemoved() {
        Customer stored = Customer.reconstitute(CustomerId.of(1), 0, Email.of("a@b.co"), new PersonName("A", "B"), "hash", Role.CUSTOMER,
                true, NOW, List.of(SavedAddress.reconstitute(AddressId.of(10), 0, "Ev", home()),
                        SavedAddress.reconstitute(AddressId.of(11), 0, "İş", home())), AddressId.of(10));

        assertThat(stored.defaultAddress()).map(SavedAddress::label).contains("Ev");

        stored.makeDefault(AddressId.of(11));
        assertThat(stored.defaultAddress()).map(SavedAddress::label).contains("İş");

        stored.removeAddress(AddressId.of(11));
        assertThat(stored.defaultAddress()).map(SavedAddress::label).contains("Ev");

        stored.removeAddress(AddressId.of(10));
        assertThat(stored.defaultAddress()).isEmpty();
        assertThatThrownBy(() -> stored.makeDefault(AddressId.of(10))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void numberOfSavedAddressesIsLimited() {
        for (int i = 0; i < Customer.MAX_ADDRESSES; i++) {
            customer.addAddress("Address " + i, home(), false);
        }

        assertThatThrownBy(() -> customer.addAddress("One more", home(), false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        customer.deactivate();
        assertThat(customer.isActive()).isFalse();

        customer.activate();
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void emailIsNormalisedAndChecked() {
        assertThat(Email.of("  Ali.Veli@Örnek.com.tr ")).hasToString("ali.veli@örnek.com.tr");
        assertThat(Email.of("a@b.co")).isEqualTo(Email.of("A@B.CO"));

        assertThatThrownBy(() -> Email.of("no-at-sign.com")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Email.of("a@nodot")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Email.of("a b@c.com")).isInstanceOf(ValidationException.class);
    }

    @Test
    void addressChecksPostalCodeAndPhone() {
        assertThat(home().singleLine()).isEqualTo("Ayşe Yılmaz, Bağdat Cad. No: 1 D: 2, Kadıköy/İstanbul 34710");

        assertThatThrownBy(() -> new Address("A", "+90 532 000 00 00", "L", "D", "C", "3471"))
                .hasMessageContaining("postalCode");
        assertThatThrownBy(() -> new Address("A", "12", "L", "D", "C", "34710"))
                .hasMessageContaining("phone");
    }

    @Test
    void passwordPolicyReportsEveryBrokenRule() {
        PasswordPolicy.check("kitap2026");

        assertThatThrownBy(() -> PasswordPolicy.check("abc"))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.violations())
                        .extracting(ValidationException.Violation::message)
                        .containsExactly("must be at least 8 characters", "must contain a digit"));
        assertThatThrownBy(() -> PasswordPolicy.check("12345678"))
                .hasMessageContaining("must contain a letter");
        assertThatThrownBy(() -> PasswordPolicy.check("a1".repeat(40)))
                .hasMessageContaining("at most 72");
    }
}
