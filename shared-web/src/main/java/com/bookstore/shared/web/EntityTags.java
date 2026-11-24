package com.bookstore.shared.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entity tags made from an aggregate's version, against lost updates over HTTP (ADR-0022): a read
 * answers {@code ETag: "7"}; a change may send it back as {@code If-Match: "7"}. If the record has
 * changed since, the change is refused with 412 instead of silently overwriting someone else's work.
 */
public final class EntityTags {

    /** The request header that carries the expected tag. */
    public static final String IF_MATCH = "If-Match";

    private static final Pattern STRONG_VERSION = Pattern.compile("\"(\\d{1,9})\"");

    private EntityTags() {
    }

    /**
     * Makes the tag of a version.
     *
     * @param version the aggregate's version
     * @return the tag, for example {@code "7"} with the quotes
     */
    public static String of(int version) {
        return "\"" + version + "\"";
    }

    /**
     * Reads the version a change expects and checks it against the current one.
     *
     * @param ifMatch the {@code If-Match} header, or {@code null}
     * @param current the version the record has now
     * @return the expected version; {@code null} if the request did not ask for a check
     *         ({@code null} header or {@code *})
     * @throws WebException 412 if the header names another version or is not a strong version tag
     */
    public static Integer expectedVersion(String ifMatch, int current) {
        if (ifMatch == null || ifMatch.isBlank() || ifMatch.strip().equals("*")) {
            return null;
        }
        Matcher matcher = STRONG_VERSION.matcher(ifMatch.strip());
        if (!matcher.matches() || Integer.parseInt(matcher.group(1)) != current) {
            throw WebException.preconditionFailed();
        }
        return current;
    }
}
