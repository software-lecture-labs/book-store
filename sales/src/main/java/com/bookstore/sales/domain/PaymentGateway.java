package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * Charges the customer for an order. The store only simulates payment: the implementation never
 * calls a real payment provider and no card data ever reaches it.
 */
public interface PaymentGateway {

    /**
     * Tries to charge an amount.
     *
     * @param orderNumber the order being paid for, used as the payment reference
     * @param amount      the amount to charge
     * @return whether the charge went through
     */
    PaymentResult charge(OrderNumber orderNumber, Money amount);

    /**
     * The outcome of a charge. Callers handle both cases with a {@code switch}.
     */
    sealed interface PaymentResult {

        /**
         * The charge went through.
         *
         * @param transactionId the provider's reference for the charge
         */
        record Approved(String transactionId) implements PaymentResult {

            /** Requires a reference. */
            public Approved {
                Guard.requireText(transactionId, "transactionId", 100);
            }
        }

        /**
         * The charge was refused.
         *
         * @param reason why, in words the customer may see
         */
        record Declined(String reason) implements PaymentResult {

            /** Requires a reason. */
            public Declined {
                Guard.requireText(reason, "reason", 200);
            }
        }
    }
}
