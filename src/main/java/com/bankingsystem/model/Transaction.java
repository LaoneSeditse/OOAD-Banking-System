package com.bankingsystem.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * A single movement of money on an account. Credits have a positive amount and
 * debits a negative amount. Follows the Transaction State Machine:
 * INITIATED -> VALIDATED -> COMPLETED, or FAILED / CANCELLED.
 */
public class Transaction {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String transactionId;
    private final String accountNumber;
    private final LocalDateTime date;
    private final double amount;
    private final TransactionType type;
    private String description;
    private double balanceAfter;
    private TransactionStatus status;

    /** Creates a new transaction in the INITIATED state. */
    public Transaction(String accountNumber, double amount, TransactionType type, String description) {
        this(newId(), accountNumber, LocalDateTime.now(), amount, type, description, 0.0, TransactionStatus.INITIATED);
    }

    /** Overloaded constructor used to rebuild a transaction loaded from the database. */
    public Transaction(String transactionId, String accountNumber, LocalDateTime date, double amount,
                       TransactionType type, String description, double balanceAfter, TransactionStatus status) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.date = date;
        this.amount = Money.round(amount);
        this.type = type;
        this.description = description;
        this.balanceAfter = Money.round(balanceAfter);
        this.status = status;
    }

    private static String newId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public void validate() {
        requireStatus(TransactionStatus.INITIATED);
        status = TransactionStatus.VALIDATED;
    }

    public void complete(double balanceAfter) {
        requireStatus(TransactionStatus.VALIDATED);
        this.balanceAfter = Money.round(balanceAfter);
        status = TransactionStatus.COMPLETED;
    }

    public void fail(String reason, double currentBalance) {
        this.description = description + " - FAILED: " + reason;
        this.balanceAfter = Money.round(currentBalance);
        status = TransactionStatus.FAILED;
    }

    public void cancel() {
        if (status == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("A completed transaction cannot be cancelled");
        }
        status = TransactionStatus.CANCELLED;
    }

    private void requireStatus(TransactionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Transaction " + transactionId + " is " + status + ", expected " + expected);
        }
    }

    public boolean isCredit() {
        return amount >= 0;
    }

    public String getTransactionDetails() {
        return String.format("%s | %s | %-10s | %12s | %s | balance %s | %s",
                transactionId, date.format(FORMAT), type, Money.format(amount), description,
                Money.format(balanceAfter), status);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getTransactionDetails();
    }
}
