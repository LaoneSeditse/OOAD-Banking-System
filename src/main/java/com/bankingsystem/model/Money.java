package com.bankingsystem.model;

/**
 * Helper for rounding monetary amounts (Pula / BWP) to 2 decimal places.
 */
public final class Money {
    private Money() {
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static String format(double amount) {
        return String.format("BWP %,.2f", amount);
    }
}
