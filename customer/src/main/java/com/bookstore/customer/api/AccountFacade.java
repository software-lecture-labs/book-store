package com.bookstore.customer.api;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Optional;

/**
 * What a customer can do with their own account: register, sign in, keep the profile and the
 * address book up to date (ACC-1 – ACC-5).
 */
public interface AccountFacade {

    /**
     * Registers a new customer (ACC-1, ACC-2).
     *
     * @param command the registration form
     * @return the new customer's profile
     * @throws ValidationException if a field is invalid or the password breaks a rule
     * @throws ConflictException   if the e-mail address is already registered
     */
    CustomerView register(RegistrationCommand command);

    /**
     * Looks up a customer by the e-mail address typed on the sign-in form (ACC-3).
     *
     * @param email the e-mail address as typed
     * @return the account, if one is registered under the address
     */
    Optional<LoginAccount> findLoginAccount(String email);

    /**
     * Checks a customer's e-mail address and password, for signing in (ACC-3). A disabled account
     * (ACC-7) cannot sign in.
     *
     * @param email    the e-mail address as typed
     * @param password the password as typed
     * @return the account, if the credentials are right and the account is active
     */
    Optional<LoginAccount> authenticate(String email, String password);

    /**
     * Tells whether an account may still be used; the security layer ends the session of an account
     * that was switched off (ACC-7, ADR-0021).
     *
     * @param customerId the account
     * @return {@code true} if it exists and is active
     */
    boolean isActive(CustomerId customerId);

    /**
     * Returns a customer's profile.
     *
     * @param customerId the customer
     * @return the profile
     * @throws NotFoundException if there is no such customer
     */
    CustomerView profile(CustomerId customerId);

    /**
     * Changes a customer's name and e-mail address (ACC-4).
     *
     * @param customerId the customer
     * @param firstName  the new first name
     * @param lastName   the new last name
     * @param email      the new e-mail address
     * @return the updated profile
     * @throws ConflictException if another customer already uses the e-mail address
     */
    CustomerView updateProfile(CustomerId customerId, String firstName, String lastName, String email);

    /**
     * Changes a customer's password after checking the current one (ACC-4).
     *
     * @param customerId      the customer
     * @param currentPassword the password the customer typed as the current one
     * @param newPassword     the new password
     * @throws ValidationException if the current password is wrong or the new one breaks a rule
     */
    void changePassword(CustomerId customerId, String currentPassword, String newPassword);

    /**
     * Saves a new address (ACC-5).
     *
     * @param customerId  the customer
     * @param label       the customer's name for the address, for example "Ev"
     * @param address     the address
     * @param makeDefault whether it becomes the default address
     * @return the updated profile
     */
    CustomerView addAddress(CustomerId customerId, String label, Address address, boolean makeDefault);

    /**
     * Changes a saved address.
     *
     * @param customerId the customer
     * @param addressId  the address
     * @param label      the new label
     * @param address    the new address
     * @return the updated profile
     * @throws NotFoundException if the customer has no such address
     */
    CustomerView changeAddress(CustomerId customerId, long addressId, String label, Address address);

    /**
     * Makes a saved address the default.
     *
     * @param customerId the customer
     * @param addressId  the address
     * @return the updated profile
     * @throws NotFoundException if the customer has no such address
     */
    CustomerView makeDefaultAddress(CustomerId customerId, long addressId);

    /**
     * Deletes a saved address. Orders placed with it keep their own copy.
     *
     * @param customerId the customer
     * @param addressId  the address
     * @return the updated profile
     * @throws NotFoundException if the customer has no such address
     */
    CustomerView removeAddress(CustomerId customerId, long addressId);

    /**
     * Returns one saved address, for pre-filling the edit form.
     *
     * @param customerId the customer
     * @param addressId  the address
     * @return the address
     * @throws NotFoundException if the customer has no such address
     */
    CustomerView.AddressView address(CustomerId customerId, long addressId);
}
