package com.bankingsystem.model;

/**
 * Concrete {@link InterestStrategy} based on a fixed monthly rate.
 * Investment = 5% monthly, Savings = 0.05% monthly, Cheque = no interest.
 */
public class InterestCalculator implements InterestStrategy {
    public static final double SAVINGS_MONTHLY_RATE = 0.0005;
    public static final double INVESTMENT_MONTHLY_RATE = 0.05;

    private final double monthlyRate;

    public InterestCalculator(double monthlyRate) {
        if (monthlyRate < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        this.monthlyRate = monthlyRate;
    }

    public static InterestCalculator forType(AccountType type) {
        return switch (type) {
            case SAVINGS -> new InterestCalculator(SAVINGS_MONTHLY_RATE);
            case INVESTMENT -> new InterestCalculator(INVESTMENT_MONTHLY_RATE);
            case CHEQUE -> new InterestCalculator(0.0);
        };
    }

    public static double calculateSavingsInterest(double balance) {
        return Money.round(balance * SAVINGS_MONTHLY_RATE);
    }

    public static double calculateInvestmentInterest(double balance) {
        return Money.round(balance * INVESTMENT_MONTHLY_RATE);
    }

    @Override
    public double getMonthlyRate() {
        return monthlyRate;
    }

    @Override
    public double calculateInterest(double balance) {
        return Money.round(balance * monthlyRate);
    }

    @Override
    public double calculateInterest(double balance, int months) {
        return Money.round(InterestStrategy.super.calculateInterest(balance, months));
    }
}
