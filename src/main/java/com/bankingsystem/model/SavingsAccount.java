package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.model.exception.WithdrawalNotAllowedException;

import java.time.LocalDate;

/**
 * Savings account: pays 0.05% interest monthly and does NOT allow withdrawals or transfers out.
 */
public class SavingsAccount extends Account {
    private final double interestRate = InterestCalculator.SAVINGS_MONTHLY_RATE;
    private final boolean allowWithdrawal = false;

    public SavingsAccount(Customer holder, String accountNumber, String branch) {
        super(holder, accountNumber, branch, AccountType.SAVINGS);
    }

    public SavingsAccount(Customer holder, String accountNumber, String branch,
                          double balance, AccountStatus status, LocalDate openedDate) {
        super(holder, accountNumber, branch, AccountType.SAVINGS, balance, status, openedDate);
    }

    @Override
    public Transaction withdraw(double amount, String description) throws BankingException {
        throw new WithdrawalNotAllowedException("Withdrawals are not allowed from a Savings account");
    }

    @Override
    public Transaction transferOut(double amount, String toAccountNumber) throws BankingException {
        throw new WithdrawalNotAllowedException("Transfers out of a Savings account are not allowed");
    }

    @Override
    public double calculateInterest() {
        return InterestCalculator.calculateSavingsInterest(getBalance());
    }

    @Override
    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public boolean allowsWithdrawals() {
        return allowWithdrawal;
    }
}
