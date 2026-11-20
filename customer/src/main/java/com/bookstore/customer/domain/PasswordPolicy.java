package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.validation.Guard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The rules a new password must follow. Only the plain-text password typed by the user is checked
 * here; it is hashed straight afterwards and never stored.
 */
public final class PasswordPolicy {

    /** Shortest allowed password. */
    public static final int MIN_LENGTH = 8;

    /** Longest allowed password. BCrypt ignores everything after 72 bytes. */
    public static final int MAX_LENGTH = 72;

    private static final Pattern LETTER = Pattern.compile("\\p{L}");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final String FIELD = "password";

    private PasswordPolicy() {
    }

    /**
     * Checks a plain-text password and reports every broken rule at once.
     *
     * @param password the password as typed
     * @throws ValidationException if any rule is broken
     */
    public static void check(String password) {
        Guard.requireNonNull(password, FIELD);
        List<ValidationException.Violation> problems = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            problems.add(new ValidationException.Violation(FIELD, "must be at least " + MIN_LENGTH + " characters"));
        }
        if (password.length() > MAX_LENGTH) {
            problems.add(new ValidationException.Violation(FIELD, "must be at most " + MAX_LENGTH + " characters"));
        }
        if (!LETTER.matcher(password).find()) {
            problems.add(new ValidationException.Violation(FIELD, "must contain a letter"));
        }
        if (!DIGIT.matcher(password).find()) {
            problems.add(new ValidationException.Violation(FIELD, "must contain a digit"));
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
    }
}
