package banking.model;

import banking.util.Money;

public class InterestCalculator {
    public static final double SAVINGS_MONTHLY_RATE = 0.0005;
    public static final double INVESTMENT_MONTHLY_RATE = 0.05;

    public double calculateSavingsInterest(double balance) {
        return Money.round(balance * SAVINGS_MONTHLY_RATE);
    }

    public double calculateInvestmentInterest(double balance) {
        return Money.round(balance * INVESTMENT_MONTHLY_RATE);
    }
}
