package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.validation.Guard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Running totals of a book's ratings, kept on the book so that lists can show and sort by the
 * average without counting reviews on every request.
 *
 * @param count number of ratings
 * @param sum   sum of all star values
 */
public record RatingSummary(int count, int sum) {

    /** A book nobody has rated. */
    public static final RatingSummary NONE = new RatingSummary(0, 0);

    private static final int MIN_STARS = 1;
    private static final int MAX_STARS = 5;

    /** Checks that the totals are consistent. */
    public RatingSummary {
        Guard.requireNonNegative(count, "count");
        Guard.require(sum >= count * MIN_STARS && sum <= count * MAX_STARS, "sum",
                "is not possible for " + count + " ratings");
    }

    /**
     * Returns the average star value rounded to one decimal place.
     *
     * @return the average, or empty if nobody has rated the book
     */
    public Optional<BigDecimal> average() {
        if (count == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP));
    }

    /**
     * Returns the average for sorting; unrated books sort below every rated book.
     *
     * @return the exact average, or zero
     */
    public double sortKey() {
        return count == 0 ? 0 : (double) sum / count;
    }

    /**
     * Adds one rating.
     *
     * @param stars star value from 1 to 5
     * @return the new totals
     */
    public RatingSummary with(int stars) {
        return new RatingSummary(count + 1, sum + validStars(stars));
    }

    /**
     * Removes one rating that was added before.
     *
     * @param stars star value from 1 to 5
     * @return the new totals
     */
    public RatingSummary without(int stars) {
        Guard.require(count > 0, "count", "there is no rating to remove");
        return new RatingSummary(count - 1, sum - validStars(stars));
    }

    private static int validStars(int stars) {
        return Guard.requireBetween(stars, MIN_STARS, MAX_STARS, "rating");
    }
}
