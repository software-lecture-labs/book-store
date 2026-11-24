package com.bookstore.customer.rest;

import com.bookstore.customer.api.RegistrationCommand;
import com.bookstore.shared.kernel.address.Address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The request bodies of the account endpoints. Bean Validation checks only their format
 * (ADR-0009); rules such as the password policy stay in the domain and come back as 422.
 */
public final class AccountRequests {

    private AccountRequests() {
    }

    /**
     * {@code POST /api/v1/customers}.
     *
     * @param email     the e-mail address, used to sign in
     * @param firstName the first name
     * @param lastName  the last name
     * @param password  the password
     */
    public record RegistrationRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 200) String password) {

        RegistrationCommand toCommand() {
            return new RegistrationCommand(email, firstName, lastName, password);
        }

        /** Hides the password when the request is logged. */
        @Override
        public String toString() {
            return "RegistrationRequest[email=" + email + "]";
        }
    }

    /**
     * {@code PUT /api/v1/me}.
     *
     * @param firstName the new first name
     * @param lastName  the new last name
     * @param email     the new e-mail address
     */
    public record ProfileRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email) {
    }

    /**
     * {@code PUT /api/v1/me/password}.
     *
     * @param currentPassword the password the customer has now
     * @param newPassword     the new password
     */
    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(max = 200) String newPassword) {

        /** Hides the passwords when the request is logged. */
        @Override
        public String toString() {
            return "PasswordChangeRequest[***]";
        }
    }

    /**
     * {@code POST /api/v1/me/addresses} and {@code PUT /api/v1/me/addresses/{id}}.
     *
     * @param label       the customer's name for the address, for example "Ev"
     * @param recipient   who receives the parcel
     * @param phone       the recipient's phone number
     * @param line        street, building and flat
     * @param district    the district
     * @param city        the city
     * @param postalCode  five digits
     * @param makeDefault whether it becomes the default address; ignored when changing an address
     */
    public record AddressRequest(
            @NotBlank @Size(max = 50) String label,
            @NotBlank @Size(max = 200) String recipient,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 300) String line,
            @NotBlank @Size(max = 100) String district,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Pattern(regexp = "\\d{5}", message = "must be five digits") String postalCode,
            boolean makeDefault) {

        Address toAddress() {
            return new Address(recipient, phone, line, district, city, postalCode);
        }
    }

    /**
     * Whether an account may sign in (ADM-6).
     *
     * @param active the new state
     */
    public record ActiveRequest(@NotNull Boolean active) {
    }
}
