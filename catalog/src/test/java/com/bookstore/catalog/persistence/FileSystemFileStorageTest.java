package com.bookstore.catalog.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemFileStorageTest {

    @TempDir
    Path root;

    @Test
    void storesLoadsAndDeletesUnderNewKeys() throws IOException {
        FileSystemFileStorage storage = new FileSystemFileStorage(root);

        String first = storage.store("covers", new byte[]{1, 2, 3}, "png");
        String second = storage.store("covers", new byte[]{4}, "png");

        assertThat(first).startsWith("covers/").endsWith(".png").isNotEqualTo(second);
        assertThat(storage.load(first)).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(1, 2, 3));
        try (Stream<Path> files = Files.list(root.resolve("covers"))) {
            assertThat(files).hasSize(2);
        }

        storage.delete(first);
        storage.delete(first);
        assertThat(storage.load(first)).isEmpty();
        assertThat(storage.load(second)).isPresent();
    }

    @Test
    void keysCannotLeadOutOfTheDirectory() {
        FileSystemFileStorage storage = new FileSystemFileStorage(root.resolve("files"));

        assertThatThrownBy(() -> storage.load("../secret.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete("covers/../../x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store("../covers", new byte[]{1}, "png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store("covers", new byte[]{1}, "png/../x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
