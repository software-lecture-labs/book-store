package com.bookstore.review.domain;

import com.bookstore.shared.kernel.validation.Guard;

/**
 * A star rating from 1 to 5.
 *
 * @param stars the number of stars
 */
public record Rating(int stars) implements Comparable<Rating> {

    /** Lowest rating. */
    public static final int MIN = 1;

    /** Highest rating. */
    public static final int MAX = 5;

    /** Checks the range. */
    public Rating {
        Guard.requireBetween(stars, MIN, MAX, "rating");
    }

    /**
     * Returns the rating with the given number of stars.
     *
     * @param stars from 1 to 5
     * @return the rating
     */
    public static Rating of(int stars) {
        return new Rating(stars);
    }

    @Override
    public int compareTo(Rating other) {
        return Integer.compare(stars, other.stars);
    }
}
