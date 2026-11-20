package com.bookstore.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.List;

import org.junit.jupiter.api.Test;

class CustomerDirectoryServiceTest {

    private final CustomerShop shop = new CustomerShop();
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final CustomerId mehmet = shop.customer("mehmet@example.com");

    @Test
    void returnsTheCustomersOwnAddressOnly() {
        assertThat(shop.directory.shippingAddress(ayse, shop.defaultAddressId(ayse))).isEqualTo(CustomerShop.address());

        assertThatThrownBy(() -> shop.directory.shippingAddress(ayse, shop.defaultAddressId(mehmet)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> shop.directory.shippingAddress(CustomerId.of(999), 1)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void publicNamesSkipUnknownCustomers() {
        shop.accounts.updateProfile(mehmet, "Mehmet", "Öztürk", "mehmet@example.com");

        assertThat(shop.directory.publicNames(List.of(ayse, mehmet, CustomerId.of(999))))
                .containsEntry(ayse, "Ayşe Y.")
                .containsEntry(mehmet, "Mehmet Ö.")
                .hasSize(2);
        assertThat(shop.directory.publicNames(List.of())).isEmpty();
    }
}
