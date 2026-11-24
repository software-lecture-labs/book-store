package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * An uploaded cover image (ADM-4): a JPEG or PNG of at most {@link #MAX_BYTES} bytes. The type is
 * read from the file's first bytes, never from a name or header the client chose.
 *
 * @param type    the image format
 * @param content the bytes; a copy is kept
 */
public record CoverImage(Type type, byte[] content) {

    /** The largest cover accepted: 2 MB. */
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    private static final String FIELD = "cover";

    /** Checks the image and copies the bytes. */
    public CoverImage {
        if (type == null || content == null || content.length == 0) {
            throw new ValidationException(FIELD, "must be a JPEG or PNG image");
        }
        if (content.length > MAX_BYTES) {
            throw new ValidationException(FIELD, "must be at most 2 MB");
        }
        if (Type.of(content).orElse(null) != type) {
            throw new ValidationException(FIELD, "must be a JPEG or PNG image");
        }
        content = content.clone();
    }

    /**
     * Reads an upload.
     *
     * @param content the uploaded bytes
     * @return the image
     * @throws ValidationException if it is empty, too large, or neither a JPEG nor a PNG
     */
    public static CoverImage of(byte[] content) {
        if (content == null) {
            throw new ValidationException(FIELD, "must be a JPEG or PNG image");
        }
        return new CoverImage(Type.of(content).orElse(null), content);
    }

    /** @return a copy of the bytes */
    @Override
    public byte[] content() {
        return content.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CoverImage that && type == that.type && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "CoverImage[" + type + ", " + content.length + " bytes]";
    }

    /** The formats a cover may have. */
    public enum Type {

        /** JPEG; files start with {@code FF D8 FF}. */
        JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),

        /** PNG; files start with the eight-byte PNG signature. */
        PNG("image/png", "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        private final String mediaType;
        private final String extension;
        private final byte[] signature;

        Type(String mediaType, String extension, byte[] signature) {
            this.mediaType = mediaType;
            this.extension = extension;
            this.signature = signature;
        }

        /** @return the media type, for example {@code image/png} */
        public String mediaType() {
            return mediaType;
        }

        /** @return the file name extension without the dot */
        public String extension() {
            return extension;
        }

        /**
         * Recognises a format by a file's first bytes.
         *
         * @param content the file
         * @return the format; empty if it is neither
         */
        public static Optional<Type> of(byte[] content) {
            return Arrays.stream(values())
                    .filter(type -> content.length >= type.signature.length
                            && Arrays.equals(content, 0, type.signature.length, type.signature, 0, type.signature.length))
                    .findFirst();
        }

        /**
         * Finds the format of a stored file by its key's extension.
         *
         * @param key the storage key, for example {@code covers/1f0c.png}
         * @return the format; empty for another extension
         */
        public static Optional<Type> ofKey(String key) {
            String lower = key.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(type -> lower.endsWith("." + type.extension)).findFirst();
        }
    }
}
