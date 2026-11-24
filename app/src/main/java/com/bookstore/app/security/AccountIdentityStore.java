package com.bookstore.app.security;

import com.bookstore.customer.api.AccountFacade;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * Checks e-mail addresses and passwords against the customer module's accounts (ADR-0020). A
 * disabled account never signs in (ACC-7). The caller's name is the customer's id, so the
 * resources never look a customer up by e-mail address; the role is the caller's only group.
 */
@ApplicationScoped
public class AccountIdentityStore implements IdentityStore {

    private final AccountFacade accounts;

    /**
     * Creates the store.
     *
     * @param accounts the account use cases
     */
    @Inject
    public AccountIdentityStore(AccountFacade accounts) {
        this.accounts = accounts;
    }

    /** For the CDI container's client proxy only; never called by the application. */
    protected AccountIdentityStore() {
        this(null);
    }

    /**
     * Checks a user name and password.
     *
     * @param credential the e-mail address and the password
     * @return the customer's id and role, or {@link CredentialValidationResult#INVALID_RESULT}
     */
    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        return accounts.authenticate(credential.getCaller(), credential.getPasswordAsString())
                .map(account -> new CredentialValidationResult(Long.toString(account.customerId().value()),
                        Set.of(account.role())))
                .orElse(CredentialValidationResult.INVALID_RESULT);
    }
}
