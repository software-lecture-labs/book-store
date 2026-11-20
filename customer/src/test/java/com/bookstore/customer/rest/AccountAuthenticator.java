package com.bookstore.customer.rest;

import com.bookstore.customer.api.AccountFacade;
import com.bookstore.shared.web.Authenticator;
import com.bookstore.shared.web.Caller;

import java.util.Optional;

/**
 * Signs HTTP Basic requests in with the customer module's accounts in the HTTP tests, as the
 * server's identity store does (ADR-0020): the e-mail address is the user name, the password is
 * checked by {@link AccountFacade#authenticate}.
 */
public class AccountAuthenticator implements Authenticator {

    private final AccountFacade accounts;

    /**
     * Creates the authenticator.
     *
     * @param accounts the account use cases
     */
    public AccountAuthenticator(AccountFacade accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Caller> authenticate(String username, String password) {
        return accounts.authenticate(username, password)
                .map(account -> new Caller(account.customerId(), account.role()));
    }
}
