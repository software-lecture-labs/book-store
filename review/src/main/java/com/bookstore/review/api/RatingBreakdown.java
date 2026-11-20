package com.bookstore.review.api;

import com.bookstore.shared.kernel.validation.Guard;

import java.math.BigDecimal;
import java.util.List;

/**
 * How a book's ratings are spread over the five star values (REV-6).
 *
 * @param counts  the number of ratings for one to five stars, in that order
 * @param average the average star value with one decimal place; {@code null} if nobody rated the book
 */
public record RatingBreakdown(List<Integer> counts, BigDecimal average) {

    /** Lowest star value. */
    public static final int MIN_STARS = 1;

    /** Highest star value. */
    public static final int MAX_STARS = 5;

    /** Copies the counts and checks that there is one per star value. */
    public RatingBreakdown {
        counts = List.copyOf(counts);
        Guard.require(counts.size() == MAX_STARS, "counts", "must have one entry per star value");
    }

    /**
     * Returns the number of ratings with a star value.
     *
     * @param stars from 1 to 5
     * @return the count
     */
    public int count(int stars) {
        return counts.get(Guard.requireBetween(stars, MIN_STARS, MAX_STARS, "stars") - MIN_STARS);
    }

    /**
     * Returns the number of ratings.
     *
     * @return the total
     */
    public int total() {
        return counts.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns the share of ratings with a star value, as a whole percentage.
     *
     * @param stars from 1 to 5
     * @return from 0 to 100, zero if there are no ratings
     */
    public int percentage(int stars) {
        int total = total();
        return total == 0 ? 0 : Math.round(100f * count(stars) / total);
    }
}
