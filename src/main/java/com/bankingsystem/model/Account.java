package com.bankingsystem.model;

import com.bankingsystem.model.exception.AccountStateException;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.model.exception.InsufficientFundsException;
import com.bankingsystem.model.exception.InvalidTransactionException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract bank account. An account cannot exist without a holder, so the holder is a
 * mandatory constructor argument and the account registers itself with that customer.
 */
public abstract class Account {
    /** Maximum amount that may be withdrawn in a single transaction (use-case: Enforce Withdrawal Limit). */
    public static final double MAX_WITHDRAWAL_PER_TRANSACTION = 5000.0;

    private final String accountNumber;
    private double balance;
    private String branch;
    private final AccountType accountType;
    private AccountStatus status;
    private final LocalDate openedDate;
    private final Customer holder;
    private final List<Transaction> transactions = new ArrayList<>();

    /** Creates a brand new account in the PENDING ("Created") state. */
    protected Account(Customer holder, String accountNumber, String branch, AccountType accountType) {
        this(holder, accountNumber, branch, accountType, 0.0, AccountStatus.PENDING, LocalDate.now());
    }

    /** Rebuilds an existing account (e.g. loaded from the database). */
    protected Account(Customer holder, String accountNumber, String branch, AccountType accountType,
                      double balance, AccountStatus status, LocalDate openedDate) {
        this.holder = Objects.requireNonNull(holder, "An account cannot exist without a customer");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.branch = branch;
        this.accountType = accountType;
        this.balance = Money.round(balance);
        this.status = status;
        this.openedDate = openedDate;
        holder.addAccount(this);
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Created -> Active: verifies the account details and the opening deposit.
     *
     * @return the opening deposit transaction, or null if the opening deposit is zero
     */
    public Transaction open(double initialDeposit) throws BankingException {
        if (status != AccountStatus.PENDING) {
            throw new AccountStateException("Account " + accountNumber + " has already been opened");
        }
        try {
            verifyOpeningRules(initialDeposit);
        } catch (BankingException e) {
            holder.removeAccount(this);
            throw e;
        }
        status = AccountStatus.ACTIVE;
        return initialDeposit > 0 ? deposit(initialDeposit, "Opening deposit") : null;
    }

    private void verifyOpeningRules(double initialDeposit) throws BankingException {
        if (initialDeposit < 0) {
            throw new InvalidTransactionException("Initial deposit cannot be negative");
        }
        if (initialDeposit < getMinimumOpeningDeposit()) {
            throw new InvalidTransactionException(String.format("A %s requires a minimum opening deposit of %s",
                    accountType.getDisplayName(), Money.format(getMinimumOpeningDeposit())));
        }
        validateOpening();
    }

    /** Hook for account-specific opening rules (e.g. employment verification). */
    protected void validateOpening() throws BankingException {
    }

    public void suspend() throws AccountStateException {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountStateException("Only an ACTIVE account can be suspended");
        }
        status = AccountStatus.SUSPENDED;
    }

    public void reactivate() throws AccountStateException {
        if (status != AccountStatus.SUSPENDED) {
            throw new AccountStateException("Only a SUSPENDED account can be reactivated");
        }
        status = AccountStatus.ACTIVE;
    }

    /**
     * Active -> Closed. Any remaining balance is paid out to the customer.
     *
     * @return the pay-out transaction, or null if the balance was zero
     */
    public Transaction close() throws AccountStateException {
        if (status == AccountStatus.CLOSED) {
            throw new AccountStateException("Account " + accountNumber + " is already closed");
        }
        Transaction payout = null;
        if (balance > 0) {
            payout = new Transaction(accountNumber, -balance, TransactionType.WITHDRAWAL, "Closing balance pay-out");
            payout.validate();
            balance = 0;
            payout.complete(balance);
            transactions.add(payout);
        }
        status = AccountStatus.CLOSED;
        return payout;
    }

    // ---------------------------------------------------------------- transactions

    public Transaction deposit(double amount) throws BankingException {
        return deposit(amount, "Deposit");
    }

    /** Overloaded deposit with a custom description. */
    public Transaction deposit(double amount, String description) throws BankingException {
        ensureCanDeposit();
        validateAmount(amount);
        return credit(amount, TransactionType.DEPOSIT, description);
    }

    public Transaction withdraw(double amount) throws BankingException {
        return withdraw(amount, "Withdrawal");
    }

    /** Overloaded withdraw with a custom description. */
    public Transaction withdraw(double amount, String description) throws BankingException {
        ensureCanWithdraw();
        validateAmount(amount);
        if (amount > MAX_WITHDRAWAL_PER_TRANSACTION) {
            throw new InvalidTransactionException("The maximum withdrawal per transaction is "
                    + Money.format(MAX_WITHDRAWAL_PER_TRANSACTION));
        }
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        return debit(amount, TransactionType.WITHDRAWAL, description);
    }

    /** Debits this account as the source of a transfer. Uses the same rules as a withdrawal. */
    public Transaction transferOut(double amount, String toAccountNumber) throws BankingException {
        ensureCanWithdraw();
        validateAmount(amount);
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        return debit(amount, TransactionType.TRANSFER, "Transfer to " + toAccountNumber);
    }

    /** Credits this account as the destination of a transfer. */
    public Transaction transferIn(double amount, String fromAccountNumber) throws BankingException {
        ensureCanDeposit();
        validateAmount(amount);
        return credit(amount, TransactionType.TRANSFER, "Transfer from " + fromAccountNumber);
    }

    /**
     * Pays one month's interest into the account.
     *
     * @return the interest transaction, or null if no interest is due
     */
    public Transaction applyMonthlyInterest() throws BankingException {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountStateException("Interest is only paid on ACTIVE accounts");
        }
        double interest = calculateInterest();
        if (interest <= 0) {
            return null;
        }
        return credit(interest, TransactionType.INTEREST,
                String.format("Monthly interest @ %.2f%%", getInterestRate() * 100));
    }

    private Transaction credit(double amount, TransactionType type, String description) {
        Transaction tx = new Transaction(accountNumber, amount, type, description);
        tx.validate();
        balance = Money.round(balance + amount);
        tx.complete(balance);
        transactions.add(tx);
        return tx;
    }

    private Transaction debit(double amount, TransactionType type, String description) {
        Transaction tx = new Transaction(accountNumber, -amount, type, description);
        tx.validate();
        balance = Money.round(balance - amount);
        tx.complete(balance);
        transactions.add(tx);
        return tx;
    }

    protected void ensureCanDeposit() throws AccountStateException {
        if (status == AccountStatus.CLOSED) {
            throw new AccountStateException("Account " + accountNumber + " is closed - all transactions are blocked");
        }
        if (status == AccountStatus.PENDING) {
            throw new AccountStateException("Account " + accountNumber + " has not been opened yet");
        }
    }

    protected void ensureCanWithdraw() throws BankingException {
        ensureCanDeposit();
        if (status == AccountStatus.SUSPENDED) {
            throw new AccountStateException("Account " + accountNumber + " is suspended - withdrawals and transfers are blocked");
        }
    }

    private static void validateAmount(double amount) throws InvalidTransactionException {
        if (Double.isNaN(amount) || amount <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }
    }

    // ---------------------------------------------------------------- polymorphic behaviour

    /** @return interest earned on the current balance for one month */
    public abstract double calculateInterest();

    /** @return monthly interest rate as a fraction */
    public abstract double getInterestRate();

    public double getMinimumOpeningDeposit() {
        return 0.0;
    }

    public boolean isInterestBearing() {
        return getInterestRate() > 0;
    }

    /** @return true if money can currently be paid into this account (ACTIVE or SUSPENDED). */
    public boolean canReceiveFunds() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.SUSPENDED;
    }

    public boolean allowsWithdrawals() {
        return true;
    }

    public String getAccountDetails() {
        return String.format("%s %s | Holder: %s | Branch: %s | Balance: %s | Status: %s | Interest: %.2f%% monthly",
                accountType.getDisplayName(), accountNumber, holder.getDisplayName(), branch,
                Money.format(balance), status, getInterestRate() * 100);
    }

    // ---------------------------------------------------------------- accessors

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    public Customer getHolder() {
        return holder;
    }

    /** Transactions performed on this object during the current session. */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return getAccountDetails();
    }
}
