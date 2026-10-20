package com.bankingsystem.model;

/**
 * Strategy for calculating the interest earned on an account balance.
 */
public interface InterestStrategy {

    /** @return the monthly interest rate as a fraction (e.g. 0.05 = 5%) */
    double getMonthlyRate();

    /** Interest earned on {@code balance} for one month. */
    double calculateInterest(double balance);

    /** Overloaded: compound interest earned on {@code balance} over {@code months} months. */
    default double calculateInterest(double balance, int months) {
        if (months < 0) {
            throw new IllegalArgumentException("months cannot be negative");
        }
        return balance * Math.pow(1 + getMonthlyRate(), months) - balance;
    }
}
