package banking.model;

import banking.util.IdGenerator;
import banking.util.Money;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Transaction {
    private final String transactionId;
    private final String accountNumber;
    private final LocalDateTime date;
    private final double amount;
    private final TransactionType type;
    private final String description;

    public Transaction(String accountNumber, double amount, TransactionType type, String description) {
        this(IdGenerator.transactionId(), accountNumber,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), amount, type, description);
    }

    public Transaction(String transactionId, String accountNumber, LocalDateTime date,
                       double amount, TransactionType type, String description) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.date = date;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public LocalDateTime getDate() { return date; }
    public double getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }

    public String getTransactionDetails() {
        return String.format("%s | %s | %s | %s | %s", transactionId, date, type, Money.format(amount), description);
    }

    @Override
    public String toString() {
        return getTransactionDetails();
    }
}
