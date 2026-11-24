package com.bookstore.sales.application;

import com.bookstore.sales.api.CartNotice;
import com.bookstore.sales.api.GuestCart;
import com.bookstore.sales.api.OrderDetails;
import com.bookstore.sales.api.OrderSummary;
import com.bookstore.sales.domain.Cart;
import com.bookstore.sales.domain.CartAdjustment;
import com.bookstore.sales.domain.CartItem;
import com.bookstore.sales.domain.Order;
import com.bookstore.sales.domain.OrderLine;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.PaymentMethod;
import com.bookstore.shared.kernel.error.ValidationException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Translates between the sales domain and its {@code api} contract (Mapper, ADR-0003): orders
 * and cart notices into DTOs, status and payment codes into enums, and a {@link GuestCart} into
 * a domain {@link Cart} and back.
 */
final class SalesMapper {

    private SalesMapper() {
    }

    static OrderSummary summaryOf(Order order) {
        return new OrderSummary(order.number().value(), order.customerId(), order.placedAt(), order.status().name(),
                order.paymentMethod().name(), order.total(), order.totalQuantity());
    }

    static OrderDetails detailsOf(Order order) {
        return new OrderDetails(order.number().value(), order.customerId(), order.placedAt(),
                order.statusChangedAt(), order.status().name(),
                order.status().allowedNext().stream().map(OrderStatus::name).collect(Collectors.toSet()),
                order.lines().stream().map(SalesMapper::lineOf).toList(), order.shippingAddress(),
                order.paymentMethod().name(), order.subtotal(), order.shippingCost(), order.total());
    }

    static List<CartNotice> noticesOf(List<CartAdjustment> adjustments) {
        return adjustments.stream().map(SalesMapper::noticeOf).toList();
    }

    /**
     * Reads an order status.
     *
     * @throws ValidationException if the code is unknown
     */
    static OrderStatus statusOf(String code) {
        return parse(OrderStatus.class, code, "status");
    }

    /**
     * Reads a payment method.
     *
     * @throws ValidationException if the code is unknown
     */
    static PaymentMethod paymentMethodOf(String code) {
        return parse(PaymentMethod.class, code, "paymentMethod");
    }

    /** Rebuilds the domain cart a visitor's {@link GuestCart} describes. */
    static Cart cartOf(GuestCart guestCart) {
        return Cart.guestWith(guestCart.items().stream()
                .map(item -> new CartItem(item.bookId(), item.quantity(), item.priceWhenAdded()))
                .toList());
    }

    /** Writes a changed domain cart back into the visitor's {@link GuestCart}. */
    static void copyInto(Cart cart, GuestCart guestCart) {
        guestCart.replaceItems(cart.items().stream()
                .map(item -> new GuestCart.Item(item.bookId(), item.quantity(), item.priceWhenAdded()))
                .toList());
    }

    private static OrderDetails.Line lineOf(OrderLine line) {
        return new OrderDetails.Line(line.bookId(), line.bookTitle(), line.unitPrice(), line.quantity(),
                line.lineTotal());
    }

    private static CartNotice noticeOf(CartAdjustment adjustment) {
        return switch (adjustment) {
            case CartAdjustment.Removed removed -> new CartNotice.Removed(removed.bookId(), removed.bookTitle());
            case CartAdjustment.QuantityReduced reduced -> new CartNotice.QuantityReduced(reduced.bookId(),
                    reduced.bookTitle(), reduced.from(), reduced.to());
            case CartAdjustment.PriceChanged changed -> new CartNotice.PriceChanged(changed.bookId(),
                    changed.bookTitle(), changed.from(), changed.to());
        };
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String code, String field) {
        if (code == null || code.isBlank()) {
            throw new ValidationException(field, "is required");
        }
        try {
            return Enum.valueOf(type, code.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(field, "unknown value: " + code);
        }
    }
}
