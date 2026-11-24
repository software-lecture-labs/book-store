package com.bookstore.catalog.api;

import java.util.Arrays;

/**
 * A book's cover image, ready to be sent (CAT-2, ADM-4).
 *
 * @param key       the storage key; it changes whenever a new cover is uploaded, so it can tag caches
 * @param mediaType {@code image/jpeg} or {@code image/png}
 * @param content   the bytes; a copy is kept
 */
public record CoverFile(String key, String mediaType, byte[] content) {

    /** Copies the bytes. */
    public CoverFile {
        content = content.clone();
    }

    /** @return a copy of the bytes */
    @Override
    public byte[] content() {
        return content.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CoverFile that && key.equals(that.key) && mediaType.equals(that.mediaType)
                && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "CoverFile[" + key + ", " + mediaType + ", " + content.length + " bytes]";
    }
}
