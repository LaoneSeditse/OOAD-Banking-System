package com.bankingsystem.model;

import java.time.LocalDate;

/**
 * Investment account: pays 5% interest monthly, allows deposits and withdrawals and
 * requires a minimum opening deposit of BWP 500.00.
 */
public class InvestmentAccount extends Account {
    public static final double MINIMUM_INITIAL_DEPOSIT = 500.0;

    private final double interestRate = InterestCalculator.INVESTMENT_MONTHLY_RATE;

    public InvestmentAccount(Customer holder, String accountNumber, String branch) {
        super(holder, accountNumber, branch, AccountType.INVESTMENT);
    }

    public InvestmentAccount(Customer holder, String accountNumber, String branch,
                             double balance, AccountStatus status, LocalDate openedDate) {
        super(holder, accountNumber, branch, AccountType.INVESTMENT, balance, status, openedDate);
    }

    @Override
    public double calculateInterest() {
        return InterestCalculator.calculateInvestmentInterest(getBalance());
    }

    @Override
    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public double getMinimumOpeningDeposit() {
        return MINIMUM_INITIAL_DEPOSIT;
    }
}
