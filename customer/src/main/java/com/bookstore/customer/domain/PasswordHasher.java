package com.bookstore.customer.domain;

/**
 * Turns passwords into one-way hashes and checks passwords against them. The infrastructure module
 * implements it with BCrypt.
 */
public interface PasswordHasher {

    /**
     * Hashes a password. Hashing the same password twice gives different hashes, because each hash
     * has its own random salt.
     *
     * @param rawPassword the password as typed
     * @return the hash, safe to store
     */
    String hash(String rawPassword);

    /**
     * Checks whether a password matches a stored hash.
     *
     * @param rawPassword the password as typed
     * @param hash        a hash produced by {@link #hash(String)}
     * @return {@code true} if they match
     */
    boolean matches(String rawPassword, String hash);
}
