package com.bookstore.shared.web;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Currency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * The API's JSON-B rules, in one place (ADR-0010):
 *
 * <ul>
 *   <li>records are read and written by their components;</li>
 *   <li>the shared-kernel ids are written as plain numbers, so {@code BookId} is {@code 42};</li>
 *   <li>{@link Money} is {@code {"amount": 95.00, "currency": "TRY"}} and {@link Year} a number;</li>
 *   <li>missing values are left out of the answer;</li>
 *   <li>unknown fields in a request are an error, so a typo is not silently ignored.</li>
 * </ul>
 */
@Provider
@ApplicationScoped
public class JsonbConfiguration implements ContextResolver<Jsonb> {

    private static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig()
            .withAdapters(new BookIdAdapter(), new CustomerIdAdapter(), new MoneyAdapter(), new YearAdapter())
            .setProperty("jsonb.fail-on-unknown-properties", true)
            .setProperty("yasson.fail-on-unknown-properties", true));

    @Override
    public Jsonb getContext(Class<?> type) {
        return JSONB;
    }

    /** @return the configured instance, for code that writes JSON outside a request */
    public static Jsonb jsonb() {
        return JSONB;
    }

    /** {@link BookId} as its number. */
    public static final class BookIdAdapter implements JsonbAdapter<BookId, Long> {

        @Override
        public Long adaptToJson(BookId id) {
            return id.value();
        }

        @Override
        public BookId adaptFromJson(Long value) {
            return BookId.of(value);
        }
    }

    /** {@link CustomerId} as its number. */
    public static final class CustomerIdAdapter implements JsonbAdapter<CustomerId, Long> {

        @Override
        public Long adaptToJson(CustomerId id) {
            return id.value();
        }

        @Override
        public CustomerId adaptFromJson(Long value) {
            return CustomerId.of(value);
        }
    }

    /** {@link Money} as an amount and a currency code. */
    public static final class MoneyAdapter implements JsonbAdapter<Money, MoneyAdapter.Amount> {

        @Override
        public Amount adaptToJson(Money money) {
            return new Amount(money.amount(), money.currency().getCurrencyCode());
        }

        @Override
        public Money adaptFromJson(Amount amount) {
            return new Money(amount.amount(), Currency.getInstance(amount.currency()));
        }

        /**
         * How money is written.
         *
         * @param amount   the amount, with two decimals
         * @param currency the ISO 4217 code, for example {@code TRY}
         */
        public record Amount(BigDecimal amount, String currency) {
        }
    }

    /** {@link Year} as a number. */
    public static final class YearAdapter implements JsonbAdapter<Year, Integer> {

        @Override
        public Integer adaptToJson(Year year) {
            return year.getValue();
        }

        @Override
        public Year adaptFromJson(Integer value) {
            return Year.of(value);
        }
    }
}
