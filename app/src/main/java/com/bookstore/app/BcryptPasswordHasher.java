package com.bookstore.app;

import com.bookstore.customer.domain.PasswordHasher;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

/**
 * Hashes passwords with BCrypt (ADR-0011, ADR-0020): a random salt and a cost factor are part of
 * every hash ({@code $2y$12$...}). Passwords longer than BCrypt's 72 bytes are first reduced with
 * SHA-512 instead of being cut silently.
 *
 * <p>Hashes made before Phase 7 with {@link Pbkdf2PasswordHasher} ({@code pbkdf2$...}) are still
 * checked with that hasher, so no account has to reset its password.
 */
public final class BcryptPasswordHasher implements PasswordHasher {

    private static final BCrypt.Version VERSION = BCrypt.Version.VERSION_2Y;

    private final int cost;
    private final BCrypt.Hasher hasher = BCrypt.with(VERSION, LongPasswordStrategies.hashSha512(VERSION));
    private final BCrypt.Verifyer verifyer = BCrypt.verifyer(VERSION, LongPasswordStrategies.hashSha512(VERSION));
    private final Pbkdf2PasswordHasher legacy = new Pbkdf2PasswordHasher();

    /** Creates a hasher with cost 12, about a quarter of a second per hash on a current machine. */
    public BcryptPasswordHasher() {
        this(12);
    }

    /**
     * Creates a hasher with a chosen cost.
     *
     * @param cost the log2 of the number of rounds, from 4 to 31; lower only in tests
     */
    public BcryptPasswordHasher(int cost) {
        if (cost < 4 || cost > 31) {
            throw new IllegalArgumentException("cost must be between 4 and 31");
        }
        this.cost = cost;
    }

    @Override
    public String hash(String rawPassword) {
        return hasher.hashToString(cost, rawPassword.toCharArray());
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (hash.startsWith("pbkdf2$")) {
            return legacy.matches(rawPassword, hash);
        }
        if (!hash.startsWith("$2")) {
            return false;
        }
        return verifyer.verify(rawPassword.toCharArray(), hash).verified;
    }

    @Override
    public String toString() {
        return "BcryptPasswordHasher[cost " + cost + "]";
    }
}
