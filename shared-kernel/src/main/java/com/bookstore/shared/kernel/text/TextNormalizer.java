package com.bookstore.shared.kernel.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Turns text into a search key that ignores case, accents and Turkish-specific letters.
 *
 * <p>{@code "Çalıkuşu"}, {@code "CALIKUSU"} and {@code "çalikuşu"} all become {@code "calikusu"}.
 * This is needed because Java's default lower-casing is locale-dependent: in Turkish, {@code "I"}
 * lower-cases to dotless {@code "ı"}, not {@code "i"}.
 */
public final class TextNormalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private TextNormalizer() {
    }

    /**
     * Builds the search key for a text.
     *
     * <ol>
     *   <li>Lower-case with Turkish rules ({@code "I" → "ı"}, {@code "İ" → "i"}).</li>
     *   <li>Decompose letters into base letter plus accent (Unicode NFD) and drop the accents.</li>
     *   <li>Replace dotless {@code "ı"}, which has no decomposition, with {@code "i"}.</li>
     *   <li>Collapse runs of whitespace into a single space and trim.</li>
     * </ol>
     *
     * @param text any text
     * @return the normalised search key
     */
    public static String searchKey(String text) {
        Objects.requireNonNull(text, "text");
        String decomposed = Normalizer.normalize(text.toLowerCase(TURKISH), Normalizer.Form.NFD);

        StringBuilder key = new StringBuilder(decomposed.length());
        boolean pendingSpace = false;
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                pendingSpace = !key.isEmpty();
                continue;
            }
            if (pendingSpace) {
                key.append(' ');
                pendingSpace = false;
            }
            key.append(c == 'ı' ? 'i' : c);
        }
        return key.toString();
    }

    /**
     * Tells whether {@code text} contains {@code query}, comparing search keys.
     *
     * @param text  the text to search in
     * @param query the text to look for
     * @return {@code true} if the normalised text contains the normalised query
     */
    public static boolean containsIgnoringCase(String text, String query) {
        return searchKey(text).contains(searchKey(query));
    }
}
