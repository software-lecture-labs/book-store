package com.bookstore.app;

import com.bookstore.customer.domain.PasswordHasher;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes passwords with PBKDF2-HMAC-SHA256 from the JDK: a random salt per password and many
 * iterations, stored as {@code pbkdf2$<iterations>$<salt>$<hash>}. It needs no library, so the
 * Phase 5 server can store real passwords; Phase 7 decides whether BCrypt replaces it (ADR-0011).
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    /** Creates a hasher with the iteration count OWASP recommends for PBKDF2-HMAC-SHA256. */
    public Pbkdf2PasswordHasher() {
        this(600_000);
    }

    /**
     * Creates a hasher with a chosen cost.
     *
     * @param iterations how many rounds; fewer only in tests
     */
    public Pbkdf2PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    @Override
    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + base64.encodeToString(salt) + "$"
                + base64.encodeToString(derive(rawPassword, salt, iterations));
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        String[] parts = hash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int rounds = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            // Compares in constant time, so the answer's timing tells an attacker nothing.
            return MessageDigest.isEqual(expected, derive(rawPassword, salt, rounds));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    @Override
    public String toString() {
        return "Pbkdf2PasswordHasher[" + iterations + " iterations]";
    }
}
