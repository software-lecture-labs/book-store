package com.bookstore.app;

import com.bookstore.sales.domain.PaymentGateway;
import com.bookstore.shared.kernel.event.Notifier;

import java.lang.System.Logger.Level;

/**
 * Stand-ins for the outside world while there is none: a payment gateway that approves every card
 * and a notifier that writes what it would tell the customer to the log. Real adapters come with
 * the payment provider and the e-mail service.
 */
public final class DemoAdapters {

    private static final System.Logger LOG = System.getLogger(DemoAdapters.class.getName());

    private DemoAdapters() {
    }

    /** @return a gateway that approves every charge and logs it */
    public static PaymentGateway approvingPayments() {
        return (orderNumber, amount) -> {
            LOG.log(Level.INFO, "Demo payment: charged {0} for order {1}", amount, orderNumber.value());
            return new PaymentGateway.PaymentResult.Approved("DEMO-" + orderNumber.value());
        };
    }

    /** @return a notifier that logs each event instead of sending an e-mail */
    public static Notifier loggingNotifier() {
        return event -> LOG.log(Level.INFO, "Notification: {0}", event);
    }
}
