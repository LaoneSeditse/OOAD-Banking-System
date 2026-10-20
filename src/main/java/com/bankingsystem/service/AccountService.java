package com.bankingsystem.service;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.Database;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionStatus;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.model.exception.BankingException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service that loads accounts, applies domain operations via {@link BankService}
 * and persists the results (AccountService lifeline in the Deposit sequence diagram).
 */
public class AccountService {
    private final Database database;
    private final BankService bankService;
    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;
    private final TransactionService transactionService;

    public AccountService(Database database, BankService bankService, AccountDAO accountDAO, CustomerDAO customerDAO,
                          TransactionService transactionService) {
        this.database = database;
        this.bankService = bankService;
        this.accountDAO = accountDAO;
        this.customerDAO = customerDAO;
        this.transactionService = transactionService;
    }

    public Optional<Account> findAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return Optional.empty();
        }
        return accountDAO.getAccount(accountNumber.trim());
    }

    public List<Account> getAccountsForCustomer(String customerId) {
        return customerDAO.findById(customerId).map(accountDAO::findByCustomer).orElse(List.of());
    }

    public List<Account> getAllAccounts() {
        return accountDAO.findAll();
    }

    public Account openAccount(String customerId, AccountType type, double initialDeposit, String branch,
                               EmploymentDetails employmentDetails) throws BankingException {
        return database.inTransaction(() -> {
            Customer customer = customerDAO.findById(customerId)
                    .orElseThrow(() -> new BankingException("Customer " + customerId + " not found"));
            accountDAO.findByCustomer(customer);
            Account account = bankService.createAccount(customer, type, initialDeposit, branch, employmentDetails);
            accountDAO.insert(account);
            account.getTransactions().forEach(transactionService::recordTransaction);
            return account;
        });
    }

    public Transaction deposit(String accountNumber, double amount) throws BankingException {
        return execute(accountNumber, amount, TransactionType.DEPOSIT, "Deposit",
                account -> bankService.deposit(account, amount));
    }

    public Transaction withdraw(String accountNumber, double amount) throws BankingException {
        return execute(accountNumber, -amount, TransactionType.WITHDRAWAL, "Withdrawal",
                account -> bankService.withdraw(account, amount));
    }

    /** Debit and credit legs are committed together or not at all. */
    public List<Transaction> transfer(String fromAccountNumber, String toAccountNumber, double amount)
            throws BankingException {
        requireAccount(fromAccountNumber);
        requireAccount(toAccountNumber);
        try {
            return database.inTransaction(() -> {
                // lock in a fixed order so two opposite transfers cannot deadlock
                if (fromAccountNumber.compareTo(toAccountNumber) <= 0) {
                    accountDAO.lockForUpdate(fromAccountNumber);
                    accountDAO.lockForUpdate(toAccountNumber);
                } else {
                    accountDAO.lockForUpdate(toAccountNumber);
                    accountDAO.lockForUpdate(fromAccountNumber);
                }
                Account from = requireAccount(fromAccountNumber);
                Account to = requireAccount(toAccountNumber);
                List<Transaction> legs = bankService.transfer(from, to, amount);
                accountDAO.saveAccount(from);
                accountDAO.saveAccount(to);
                legs.forEach(transactionService::recordTransaction);
                return legs;
            });
        } catch (BankingException e) {
            recordFailure(fromAccountNumber, -amount, TransactionType.TRANSFER, "Transfer to " + toAccountNumber, e);
            throw e;
        }
    }

    /** Opt fragment of the Deposit sequence: is this an interest-bearing account? */
    public boolean checkInterestEligibility(Account account) {
        return account.isInterestBearing() && account.getStatus() == AccountStatus.ACTIVE;
    }

    /**
     * Recalculates the interest that the account will earn at the next monthly interest run.
     *
     * @return projected interest for the coming month
     */
    public double updateInterest(String accountNumber) throws BankingException {
        return bankService.calculateInterest(requireAccount(accountNumber));
    }

    /**
     * Pays one month's interest into every ACTIVE interest-bearing account that has not yet been
     * paid interest this calendar month, so running it twice in a month never pays twice.
     */
    public List<Transaction> payMonthlyInterest() throws BankingException {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return database.inTransaction(() -> {
            List<Transaction> paid = new ArrayList<>();
            for (Account listed : accountDAO.findAll()) {
                accountDAO.lockForUpdate(listed.getAccountNumber());
                Account account = requireAccount(listed.getAccountNumber());
                if (checkInterestEligibility(account) && !interestPaidSince(account, startOfMonth)) {
                    Transaction tx = bankService.payInterest(account);
                    if (tx != null) {
                        accountDAO.saveAccount(account);
                        transactionService.recordTransaction(tx);
                        paid.add(tx);
                    }
                }
            }
            return paid;
        });
    }

    public boolean interestPaidSince(Account account, LocalDateTime since) {
        return transactionService.getTransactionHistory(account.getAccountNumber()).stream()
                .anyMatch(t -> t.getType() == TransactionType.INTEREST
                        && t.getStatus() == TransactionStatus.COMPLETED
                        && !t.getDate().isBefore(since));
    }

    /** @return the pay-out transaction of the remaining balance, or null if the balance was zero */
    public Transaction closeAccount(String accountNumber) throws BankingException {
        return database.inTransaction(() -> {
            Account account = lockAndLoad(accountNumber);
            Transaction payout = account.close();
            accountDAO.saveAccount(account);
            transactionService.recordTransaction(payout);
            return payout;
        });
    }

    public void suspendAccount(String accountNumber) throws BankingException {
        database.inTransaction(() -> {
            Account account = lockAndLoad(accountNumber);
            account.suspend();
            accountDAO.saveAccount(account);
            return null;
        });
    }

    public void reactivateAccount(String accountNumber) throws BankingException {
        database.inTransaction(() -> {
            Account account = lockAndLoad(accountNumber);
            account.reactivate();
            accountDAO.saveAccount(account);
            return null;
        });
    }

    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionService.getTransactionHistory(accountNumber);
    }

    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    private Account requireAccount(String accountNumber) throws BankingException {
        return findAccount(accountNumber)
                .orElseThrow(() -> new BankingException("Account " + accountNumber + " not found"));
    }

    private Account lockAndLoad(String accountNumber) throws BankingException {
        requireAccount(accountNumber);
        accountDAO.lockForUpdate(accountNumber);
        return requireAccount(accountNumber);
    }

    @FunctionalInterface
    private interface AccountOperation {
        Transaction run(Account account) throws BankingException;
    }

    /** Loads, changes and saves the account and records the transaction in one database transaction. */
    private Transaction execute(String accountNumber, double signedAmount, TransactionType type, String description,
                                AccountOperation operation) throws BankingException {
        requireAccount(accountNumber);
        try {
            return database.inTransaction(() -> {
                Account account = lockAndLoad(accountNumber);
                Transaction tx = operation.run(account);
                accountDAO.saveAccount(account);
                transactionService.recordTransaction(tx);
                return tx;
            });
        } catch (BankingException e) {
            recordFailure(accountNumber, signedAmount, type, description, e);
            throw e;
        }
    }

    /** Failed attempts are logged outside the rolled-back transaction so they remain auditable. */
    private void recordFailure(String accountNumber, double signedAmount, TransactionType type, String description,
                               BankingException cause) {
        findAccount(accountNumber).ifPresent(account -> {
            Transaction failed = new Transaction(accountNumber, signedAmount, type, description);
            failed.fail(cause.getMessage(), account.getBalance());
            transactionService.recordTransaction(failed);
        });
    }
}
