package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.FileStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Keeps files in a directory on the server (ADR-0021): {@code <root>/<folder>/<random>.<ext>}. The
 * key is that relative path. A file is written under a temporary name and then moved into place,
 * so a reader never sees half a file. Keys that would lead out of the root are refused.
 */
public final class FileSystemFileStorage implements FileStorage {

    private static final Pattern NAME = Pattern.compile("[a-z0-9-]{1,40}");

    private final Path root;

    /**
     * Creates the storage; the directory is created when the first file is stored.
     *
     * @param root the directory
     */
    public FileSystemFileStorage(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    @Override
    public String store(String folder, byte[] content, String extension) {
        if (!NAME.matcher(folder).matches() || !NAME.matcher(extension).matches()) {
            throw new IllegalArgumentException("folder and extension must be short lower-case names");
        }
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(temporary, content);
                move(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store " + key, e);
        }
        return key;
    }

    @Override
    public Optional<byte[]> load(String key) {
        try {
            return Optional.of(Files.readAllBytes(resolve(key)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + key, e);
        }
    }

    @Override
    public String toString() {
        return "FileSystemFileStorage[" + root + "]";
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Not a key of this storage: " + key);
        }
        return path;
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
