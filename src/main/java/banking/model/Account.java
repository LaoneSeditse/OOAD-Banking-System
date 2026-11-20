package banking.model;

import banking.exception.BankException;
import banking.exception.InsufficientFundsException;
import banking.exception.ValidationException;
import banking.util.Money;
import banking.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Account {
    public static final double WITHDRAWAL_LIMIT = 5000.00;

    private final String accountNumber;
    private double balance;
    private final String branch;
    private AccountStatus status;
    private final Customer customer;
    private final List<Transaction> transactions = new ArrayList<>();

    protected Account(String accountNumber, Customer customer, String branch,
                      double balance, AccountStatus status) {
        if (customer == null) {
            throw new ValidationException("An account cannot exist without a customer.");
        }
        this.accountNumber = Validator.requireNotBlank(accountNumber, "Account number");
        this.branch = Validator.requireNotBlank(branch, "Branch");
        this.balance = Money.round(balance);
        this.status = status;
        this.customer = customer;
        customer.addAccount(this);
    }

    // ---------- deposits (overloaded + overridden) ----------

    public void deposit(double amount) {
        deposit(amount, "Deposit");
    }

    public final void deposit(double amount, String description) {
        credit(amount, TransactionType.DEPOSIT, description);
    }

    // ---------- withdrawals ----------

    public abstract void withdraw(double amount);

    protected abstract void checkWithdrawalRules(double amount);

    protected void enforceWithdrawalLimit(double amount) {
        if (amount > WITHDRAWAL_LIMIT) {
            throw new BankException("Withdrawals are limited to " + Money.format(WITHDRAWAL_LIMIT) + " per transaction.");
        }
    }

    protected void withdrawFunds(double amount) {
        checkWithdrawalRules(amount);
        debit(amount, TransactionType.WITHDRAWAL, "Withdrawal");
    }

    // ---------- transfers ----------

    public final void transferOut(double amount, String toAccountNumber) {
        checkWithdrawalRules(amount);
        debit(amount, TransactionType.TRANSFER, "Transfer to " + toAccountNumber);
    }

    public final void transferIn(double amount, String fromAccountNumber) {
        credit(amount, TransactionType.TRANSFER, "Transfer from " + fromAccountNumber);
    }

    // ---------- interest ----------

    public abstract double calculateInterest();

    public abstract double getInterestRate();

    public Transaction applyInterest() {
        requireActive();
        double interest = Money.round(calculateInterest());
        if (interest <= 0) {
            return null;
        }
        credit(interest, TransactionType.INTEREST, "Monthly interest");
        return getLastTransaction();
    }

    // ---------- lifecycle ----------

    public void suspend() {
        requireNotClosed();
        status = AccountStatus.SUSPENDED;
    }

    public void reactivate() {
        requireNotClosed();
        status = AccountStatus.ACTIVE;
    }

    public Transaction close() {
        requireNotClosed();
        Transaction payout = null;
        if (balance > 0) {
            payout = record(TransactionType.WITHDRAWAL, balance, "Account closure payout");
            balance = 0.0;
        }
        status = AccountStatus.CLOSED;
        return payout;
    }

    public abstract AccountType getAccountType();

    // ---------- internals ----------

    private void credit(double amount, TransactionType type, String description) {
        requireActive();
        Validator.requirePositive(amount, "Amount");
        double value = Money.round(amount);
        balance = Money.round(balance + value);
        record(type, value, description);
    }

    protected void debit(double amount, TransactionType type, String description) {
        requireActive();
        Validator.requirePositive(amount, "Amount");
        double value = Money.round(amount);
        if (value > balance) {
            throw new InsufficientFundsException("Insufficient funds. Available balance: " + Money.format(balance));
        }
        balance = Money.round(balance - value);
        record(type, value, description);
    }

    private Transaction record(TransactionType type, double amount, String description) {
        Transaction t = new Transaction(accountNumber, amount, type, description);
        transactions.add(t);
        return t;
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BankException("This account is " + status.name().toLowerCase() + " and cannot be used.");
        }
    }

    private void requireNotClosed() {
        if (status == AccountStatus.CLOSED) {
            throw new BankException("This account is already closed.");
        }
    }

    /** Used by the DAO to rebuild history from the database (does not change the balance). */
    public void attachTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public Transaction getLastTransaction() {
        return transactions.isEmpty() ? null : transactions.get(transactions.size() - 1);
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public String getBranch() { return branch; }
    public AccountStatus getStatus() { return status; }
    public Customer getCustomer() { return customer; }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }

    @Override
    public String toString() {
        return getAccountType().getLabel() + " " + accountNumber + " (" + Money.format(balance) + ")";
    }
}
