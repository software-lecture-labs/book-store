package com.bookstore.review.application;

import com.bookstore.catalog.api.RatingFacade;
import com.bookstore.customer.api.CustomerDirectory;
import com.bookstore.sales.api.PurchaseHistory;
import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stand-ins for the modules the review module talks to through their {@code api} packages: the
 * catalog's rating totals, the sales module's purchase history and the customers' public names.
 */
public final class ReviewFakes {

    private ReviewFakes() {
    }

    /** The catalog's rating totals, kept per book. */
    public static final class FakeRatings implements RatingFacade {

        private final Map<BookId, int[]> totals = new HashMap<>();

        public FakeRatings(BookId... bookIds) {
            for (BookId bookId : bookIds) {
                totals.put(bookId, new int[2]);
            }
        }

        public int count(BookId bookId) {
            return totals.get(bookId)[0];
        }

        public int sum(BookId bookId) {
            return totals.get(bookId)[1];
        }

        @Override
        public boolean exists(BookId bookId) {
            return totals.containsKey(bookId);
        }

        @Override
        public void addRating(BookId bookId, int stars) {
            int[] t = require(bookId);
            t[0]++;
            t[1] += stars;
        }

        @Override
        public void removeRating(BookId bookId, int stars) {
            int[] t = require(bookId);
            t[0]--;
            t[1] -= stars;
        }

        private int[] require(BookId bookId) {
            int[] t = totals.get(bookId);
            if (t == null) {
                throw new NotFoundException("Book", bookId);
            }
            return t;
        }
    }

    /** Which customer received which book. */
    public static final class FakePurchases implements PurchaseHistory {

        private final Set<List<Object>> received = new HashSet<>();

        public void received(CustomerId customerId, BookId bookId) {
            received.add(List.of(customerId, bookId));
        }

        @Override
        public boolean hasReceived(CustomerId customerId, BookId bookId) {
            return received.contains(List.of(customerId, bookId));
        }
    }

    /** Fixed public names. */
    public record FakeNames(Map<CustomerId, String> names) implements CustomerDirectory {

        @Override
        public Address shippingAddress(CustomerId customerId, long addressId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<CustomerId, String> publicNames(Collection<CustomerId> customerIds) {
            return customerIds.stream().filter(names::containsKey).collect(Collectors.toMap(id -> id, names::get));
        }
    }
}
