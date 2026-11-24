package com.bookstore.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.api.RegistrationCommand;
import com.bookstore.customer.domain.Email;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.CustomerId;

import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final CustomerShop shop = new CustomerShop();

    @Test
    void registersACustomerWithAHashedPassword() {
        CustomerView view = shop.accounts.register(
                new RegistrationCommand(" Ali@Example.com ", "Ali", "Veli", CustomerShop.PASSWORD));

        assertThat(view.email()).isEqualTo("ali@example.com");
        assertThat(view.role()).isEqualTo("CUSTOMER");
        assertThat(view.registeredAt()).isEqualTo(CustomerShop.CLOCK.instant());
        String storedHash = shop.customers.findById(view.id()).orElseThrow().passwordHash();
        assertThat(storedHash).isNotEqualTo(CustomerShop.PASSWORD);
        assertThat(shop.passwordHasher.matches(CustomerShop.PASSWORD, storedHash)).isTrue();
    }

    @Test
    void emailCanOnlyBeRegisteredOnceWhateverItsCase() {
        shop.accounts.register(new RegistrationCommand("ali@example.com", "Ali", "Veli", CustomerShop.PASSWORD));

        assertThatThrownBy(() -> shop.accounts.register(
                new RegistrationCommand("ALI@example.com", "Başka", "Biri", CustomerShop.PASSWORD)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(AccountService.EMAIL_TAKEN));
    }

    @Test
    void weakPasswordsAndBadInputAreRejectedBeforeAnythingIsStored() {
        assertThatThrownBy(() -> shop.accounts.register(new RegistrationCommand("a@b.co", "A", "B", "short")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> shop.accounts.register(new RegistrationCommand("not-an-email", "A", "B",
                CustomerShop.PASSWORD))).isInstanceOf(ValidationException.class);

        assertThat(shop.customers.count()).isZero();
        assertThat(shop.transactions.started()).isZero();
    }

    @Test
    void commandDoesNotPrintThePassword() {
        assertThat(new RegistrationCommand("a@b.co", "A", "B", "secret123").toString()).doesNotContain("secret123");
    }

    @Test
    void findsTheLoginAccountByEmail() {
        CustomerId id = shop.customer("ayse@example.com");

        assertThat(shop.accounts.findLoginAccount("AYSE@example.com")).hasValueSatisfying(account -> {
            assertThat(account.customerId()).isEqualTo(id);
            assertThat(account.active()).isTrue();
            assertThat(account.toString()).doesNotContain(account.passwordHash());
        });
        assertThat(shop.accounts.findLoginAccount("nobody@example.com")).isEmpty();
        assertThat(shop.accounts.findLoginAccount("not an email")).isEmpty();
    }

    @Test
    void authenticationNeedsTheRightPasswordAndAnActiveAccount() {
        CustomerId id = shop.customer("ayse@example.com");

        assertThat(shop.accounts.authenticate("AYSE@example.com", CustomerShop.PASSWORD))
                .hasValueSatisfying(account -> assertThat(account.customerId()).isEqualTo(id));
        assertThat(shop.accounts.authenticate("ayse@example.com", "wrong-password")).isEmpty();
        assertThat(shop.accounts.authenticate("ayse@example.com", null)).isEmpty();
        assertThat(shop.accounts.authenticate("nobody@example.com", CustomerShop.PASSWORD)).isEmpty();

        var customer = shop.customers.findById(id).orElseThrow();
        customer.deactivate();
        shop.customers.save(customer);
        assertThat(shop.accounts.authenticate("ayse@example.com", CustomerShop.PASSWORD)).isEmpty();
    }

    @Test
    void loginAccountShowsDeactivatedCustomers() {
        CustomerId id = shop.customer("ayse@example.com");
        var customer = shop.customers.findById(id).orElseThrow();
        customer.deactivate();
        shop.customers.save(customer);

        assertThat(shop.accounts.findLoginAccount("ayse@example.com")).hasValueSatisfying(
                account -> assertThat(account.active()).isFalse());
    }

    @Test
    void updatesTheProfileAndRefusesATakenEmail() {
        CustomerId ayse = shop.customer("ayse@example.com");
        shop.customer("mehmet@example.com");

        CustomerView updated = shop.accounts.updateProfile(ayse, "Ayşe Nur", "Yılmaz", "ayse.nur@example.com");

        assertThat(updated.firstName()).isEqualTo("Ayşe Nur");
        assertThat(shop.customers.existsByEmail(Email.of("ayse.nur@example.com"))).isTrue();
        assertThat(shop.customers.existsByEmail(Email.of("ayse@example.com"))).isFalse();
        assertThatThrownBy(() -> shop.accounts.updateProfile(ayse, "A", "Y", "mehmet@example.com"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void keepingTheSameEmailIsNotAConflict() {
        CustomerId ayse = shop.customer("ayse@example.com");

        assertThat(shop.accounts.updateProfile(ayse, "Ayşe", "Kaya", "AYSE@example.com").lastName())
                .isEqualTo("Kaya");
    }

    @Test
    void changingThePasswordRequiresTheCurrentOne() {
        CustomerId id = shop.customer("ayse@example.com");

        assertThatThrownBy(() -> shop.accounts.changePassword(id, "wrong-password1", "yeniSifre1"))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.violations())
                        .singleElement()
                        .satisfies(v -> assertThat(v.field()).isEqualTo("currentPassword")));

        shop.accounts.changePassword(id, CustomerShop.PASSWORD, "yeniSifre1");
        String hash = shop.customers.findById(id).orElseThrow().passwordHash();
        assertThat(shop.passwordHasher.matches("yeniSifre1", hash)).isTrue();
    }

    @Test
    void manageTheAddressBook() {
        CustomerId id = shop.customer("ayse@example.com");
        long home = shop.defaultAddressId(id);
        Address work = new Address("Ayşe Yılmaz", "+90 212 000 00 00", "Büyükdere Cad. 100", "Şişli", "İstanbul",
                "34394");

        CustomerView withWork = shop.accounts.addAddress(id, "İş", work, false);
        long workId = withWork.addresses().get(1).id();

        assertThat(withWork.addresses()).extracting(CustomerView.AddressView::label).containsExactly("Ev", "İş");
        assertThat(shop.accounts.makeDefaultAddress(id, workId).addresses().get(1).isDefault()).isTrue();
        assertThat(shop.accounts.changeAddress(id, home, "Yazlık", CustomerShop.address()).addresses().getFirst().label())
                .isEqualTo("Yazlık");
        assertThat(shop.accounts.address(id, workId).address()).isEqualTo(work);

        CustomerView afterRemoval = shop.accounts.removeAddress(id, workId);
        assertThat(afterRemoval.addresses()).singleElement().satisfies(a -> assertThat(a.isDefault()).isTrue());
        assertThatThrownBy(() -> shop.accounts.address(id, workId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownCustomerIsReported() {
        assertThatThrownBy(() -> shop.accounts.profile(CustomerId.of(999))).isInstanceOf(NotFoundException.class);
    }
}
