package com.bookstore.review.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * How many ratings of each star value a book received, for the bar chart on its detail page.
 */
public final class RatingDistribution {

    /** Index 0 is unused so that {@code counts[stars]} reads naturally. */
    private final int[] counts = new int[Rating.MAX + 1];

    private RatingDistribution() {
    }

    /**
     * Counts a collection of ratings.
     *
     * @param ratings the ratings, in any order
     * @return the distribution
     */
    public static RatingDistribution of(Collection<Rating> ratings) {
        Map<Rating, Long> grouped = ratings.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        RatingDistribution distribution = new RatingDistribution();
        grouped.forEach((rating, count) -> distribution.counts[rating.stars()] = Math.toIntExact(count));
        return distribution;
    }

    /**
     * Returns how many ratings have a star value.
     *
     * @param rating the star value
     * @return the count
     */
    public int count(Rating rating) {
        return counts[rating.stars()];
    }

    /**
     * Returns the number of ratings.
     *
     * @return the total count
     */
    public int total() {
        return Arrays.stream(counts).sum();
    }

    /**
     * Returns the share of ratings with a star value, as a whole percentage.
     *
     * @param rating the star value
     * @return from 0 to 100, zero if there are no ratings
     */
    public int percentage(Rating rating) {
        int total = total();
        return total == 0 ? 0 : Math.round(100f * count(rating) / total);
    }

    /**
     * Returns the average star value rounded to one decimal place.
     *
     * @return the average, or empty if there are no ratings
     */
    public Optional<BigDecimal> average() {
        int total = total();
        if (total == 0) {
            return Optional.empty();
        }
        int sum = 0;
        for (int stars = Rating.MIN; stars <= Rating.MAX; stars++) {
            sum += stars * counts[stars];
        }
        return Optional.of(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RatingDistribution that && Arrays.equals(counts, that.counts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(counts);
    }

    @Override
    public String toString() {
        return "RatingDistribution" + Arrays.toString(Arrays.copyOfRange(counts, Rating.MIN, Rating.MAX + 1));
    }
}
