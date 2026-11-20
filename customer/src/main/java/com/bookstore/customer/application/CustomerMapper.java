package com.bookstore.customer.application;

import com.bookstore.customer.api.CustomerSummary;
import com.bookstore.customer.api.CustomerView;
import com.bookstore.customer.api.LoginAccount;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.domain.SavedAddress;

/**
 * Translates customers into the {@code api} contract's views (Mapper, ADR-0003). The password
 * hash only ever leaves the module inside a {@link LoginAccount}, for the security layer.
 */
final class CustomerMapper {

    private CustomerMapper() {
    }

    static CustomerView viewOf(Customer customer) {
        return new CustomerView(customer.id(), customer.email().value(), customer.name().firstName(),
                customer.name().lastName(), customer.role().name(), customer.registeredAt(),
                customer.addresses().stream().map(address -> addressOf(customer, address)).toList());
    }

    static CustomerView.AddressView addressOf(Customer owner, SavedAddress address) {
        boolean isDefault = owner.defaultAddress().map(address::equals).orElse(false);
        return new CustomerView.AddressView(address.id().value(), address.label(), address.address(), isDefault);
    }

    static CustomerSummary summaryOf(Customer customer) {
        return new CustomerSummary(customer.id(), customer.email().value(), customer.name().firstName(),
                customer.name().lastName(), customer.role().name(), customer.isActive(), customer.registeredAt());
    }

    static LoginAccount loginOf(Customer customer) {
        return new LoginAccount(customer.id(), customer.email().value(), customer.passwordHash(),
                customer.role().name(), customer.isActive());
    }
}
