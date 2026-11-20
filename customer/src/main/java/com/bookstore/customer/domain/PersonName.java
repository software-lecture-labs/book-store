package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.validation.Guard;

/**
 * A person's first and last name.
 *
 * @param firstName the first name, or names
 * @param lastName  the family name
 */
public record PersonName(String firstName, String lastName) {

    /** Longest allowed first or last name. */
    public static final int MAX_LENGTH = 100;

    /** Trims and checks both parts. */
    public PersonName {
        firstName = Guard.requireText(firstName, "firstName", MAX_LENGTH);
        lastName = Guard.requireText(lastName, "lastName", MAX_LENGTH);
    }

    /**
     * Returns the name as it is usually written.
     *
     * @return for example {@code "Ayşe Yılmaz"}
     */
    public String fullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns the first name and the initial of the last name, shown next to public reviews so
     * that customers' full names are not published.
     *
     * @return for example {@code "Ayşe Y."}
     */
    public String publicName() {
        int initialEnd = lastName.offsetByCodePoints(0, 1);
        return firstName + " " + lastName.substring(0, initialEnd) + ".";
    }

    @Override
    public String toString() {
        return fullName();
    }
}
