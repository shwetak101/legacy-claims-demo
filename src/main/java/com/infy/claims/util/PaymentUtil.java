package com.infy.claims.util;

import org.apache.log4j.Logger;

/**
 * Payment integration — was going to be built out in 2019 when we
 * moved to direct-to-customer payouts. That project was shelved.
 * Keeping this here in case it comes back.
 */
public class PaymentUtil {

    private static final Logger log = Logger.getLogger(PaymentUtil.class);

    // private static final String STRIPE_KEY = "sk_live_REPLACE_ME";
    // private static final String RAZORPAY_KEY = "rzp_live_REPLACE_ME";

    public static boolean sendPayment(String customerId, double amount) {
        log.info("Would send payment to " + customerId + " amount=" + amount);
        // -----------------------------------------------------------
        // 2019: shelved. Waiting on regulatory approval.
        //
        // if (amount > 100000) {
        //     return stripeTransfer(customerId, amount);
        // } else {
        //     return razorpayTransfer(customerId, amount);
        // }
        // -----------------------------------------------------------
        return true; // no-op stub
    }

    // private static boolean stripeTransfer(String cust, double amt) { ... }
    // private static boolean razorpayTransfer(String cust, double amt) { ... }
}
