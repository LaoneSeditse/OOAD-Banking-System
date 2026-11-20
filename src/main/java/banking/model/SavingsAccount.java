package banking.model;

import banking.exception.BankException;
import banking.exception.ValidationException;

public class SavingsAccount extends Account {
    private final InterestStrategy interestStrategy = new SavingsInterestStrategy();
    private final boolean allowWithdrawal = false;

    private SavingsAccount(String accountNumber, Customer customer, String branch,
                           double balance, AccountStatus status) {
        super(accountNumber, customer, branch, balance, status);
    }

    public static SavingsAccount open(String accountNumber, Customer customer, String branch, double initialDeposit) {
        if (initialDeposit < 0) {
            throw new ValidationException("Initial deposit cannot be negative.");
        }
        SavingsAccount account = new SavingsAccount(accountNumber, customer, branch, 0.0, AccountStatus.ACTIVE);
        if (initialDeposit > 0) {
            account.deposit(initialDeposit, "Initial deposit");
        }
        return account;
    }

    /** Rebuilds an existing account from stored data. */
    public static SavingsAccount restore(String accountNumber, Customer customer, String branch,
                                         double balance, AccountStatus status) {
        return new SavingsAccount(accountNumber, customer, branch, balance, status);
    }

    @Override
    public void deposit(double amount) {
        deposit(amount, "Savings deposit");
    }

    @Override
    public void withdraw(double amount) {
        withdrawFunds(amount);
    }

    @Override
    protected void checkWithdrawalRules(double amount) {
        if (!allowWithdrawal) {
            throw new BankException("Withdrawals are not allowed from a Savings account.");
        }
    }

    @Override
    public double calculateInterest() {
        return interestStrategy.calculateInterest(getBalance());
    }

    @Override
    public double getInterestRate() {
        return interestStrategy.getRate();
    }

    public boolean isAllowWithdrawal() {
        return allowWithdrawal;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }
}
