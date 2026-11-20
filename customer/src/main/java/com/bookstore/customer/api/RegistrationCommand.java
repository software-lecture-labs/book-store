package com.bookstore.customer.api;

/**
 * The registration form (ACC-1).
 *
 * @param email     the e-mail address
 * @param firstName the first name
 * @param lastName  the last name
 * @param password  the chosen password, in plain text; never stored
 */
public record RegistrationCommand(String email, String firstName, String lastName, String password) {

    /** Hides the password when the command is logged. */
    @Override
    public String toString() {
        return "RegistrationCommand[email=" + email + ", firstName=" + firstName + ", lastName=" + lastName
                + ", password=***]";
    }
}
