package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.model.exception.InvalidTransactionException;

import java.time.LocalDate;

/**
 * Cheque account used for salary payments. Allows deposits and withdrawals of any amount,
 * pays no interest and can only be opened by a customer who is employed.
 */
public class ChequeAccount extends Account {
    private final double interestRate = 0.0;
    private final EmploymentDetails employmentDetails;
    private boolean employmentVerified;

    public ChequeAccount(Customer holder, String accountNumber, String branch, EmploymentDetails employmentDetails) {
        super(holder, accountNumber, branch, AccountType.CHEQUE);
        this.employmentDetails = employmentDetails;
    }

    public ChequeAccount(Customer holder, String accountNumber, String branch, EmploymentDetails employmentDetails,
                         double balance, AccountStatus status, LocalDate openedDate) {
        super(holder, accountNumber, branch, AccountType.CHEQUE, balance, status, openedDate);
        this.employmentDetails = employmentDetails;
        this.employmentVerified = employmentDetails != null && employmentDetails.verify();
    }

    public boolean verifyEmployment() {
        employmentVerified = employmentDetails != null && employmentDetails.verify();
        return employmentVerified;
    }

    @Override
    protected void validateOpening() throws BankingException {
        if (!verifyEmployment()) {
            throw new InvalidTransactionException(
                    "A Cheque account can only be opened by an employed customer: employer name, address and position are required");
        }
    }

    @Override
    public double calculateInterest() {
        return 0.0;
    }

    @Override
    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public String getAccountDetails() {
        return super.getAccountDetails() + " | Employer: " + employmentDetails;
    }

    public EmploymentDetails getEmploymentDetails() {
        return employmentDetails;
    }

    public boolean isEmploymentVerified() {
        return employmentVerified;
    }
}
