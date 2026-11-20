package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.FileStorage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Keeps files in memory, for tests; it stores and returns copies. */
public final class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String store(String folder, byte[] content, String extension) {
        String key = folder + "/" + counter.incrementAndGet() + "." + extension;
        files.put(key, content.clone());
        return key;
    }

    @Override
    public Optional<byte[]> load(String key) {
        return Optional.ofNullable(files.get(key)).map(byte[]::clone);
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }

    /** @return the keys of the files kept now */
    public Set<String> keys() {
        return Set.copyOf(files.keySet());
    }
}
