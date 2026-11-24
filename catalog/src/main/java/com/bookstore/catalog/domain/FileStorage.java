package com.bookstore.catalog.domain;

import java.util.Optional;

/**
 * Where uploaded files such as cover images are kept (ADM-4, ADR-0021). The catalog stores only
 * the key it gets back; the adapter decides where the bytes go: a directory, an object store.
 */
public interface FileStorage {

    /**
     * Stores a new file under a key nobody used before.
     *
     * @param folder    a short lower-case name grouping the files, for example {@code "covers"}
     * @param content   the bytes
     * @param extension the file name extension without the dot, for example {@code "png"}
     * @return the key to load or delete the file with
     */
    String store(String folder, byte[] content, String extension);

    /**
     * Loads a file.
     *
     * @param key the key {@link #store} returned
     * @return the bytes; empty if there is no such file
     */
    Optional<byte[]> load(String key);

    /**
     * Deletes a file; a missing file is not an error.
     *
     * @param key the key {@link #store} returned
     */
    void delete(String key);
}
