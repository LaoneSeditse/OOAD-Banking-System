package com.bankingsystem.service;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.model.exception.BankingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service that loads accounts, applies domain operations via {@link BankService}
 * and persists the results (AccountService lifeline in the Deposit sequence diagram).
 */
public class AccountService {
    private final BankService bankService;
    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;
    private final TransactionService transactionService;

    public AccountService(BankService bankService, AccountDAO accountDAO, CustomerDAO customerDAO,
                          TransactionService transactionService) {
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
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new BankingException("Customer " + customerId + " not found"));
        accountDAO.findByCustomer(customer);
        Account account = bankService.createAccount(customer, type, initialDeposit, branch, employmentDetails);
        accountDAO.insert(account);
        account.getTransactions().forEach(transactionService::recordTransaction);
        return account;
    }

    public Transaction deposit(String accountNumber, double amount) throws BankingException {
        Account account = requireAccount(accountNumber);
        return execute(account, amount, TransactionType.DEPOSIT, "Deposit", () -> bankService.deposit(account, amount));
    }

    public Transaction withdraw(String accountNumber, double amount) throws BankingException {
        Account account = requireAccount(accountNumber);
        return execute(account, -amount, TransactionType.WITHDRAWAL, "Withdrawal",
                () -> bankService.withdraw(account, amount));
    }

    public List<Transaction> transfer(String fromAccountNumber, String toAccountNumber, double amount)
            throws BankingException {
        Account from = requireAccount(fromAccountNumber);
        Account to = requireAccount(toAccountNumber);
        try {
            List<Transaction> legs = bankService.transfer(from, to, amount);
            accountDAO.saveAccount(from);
            accountDAO.saveAccount(to);
            legs.forEach(transactionService::recordTransaction);
            return legs;
        } catch (BankingException e) {
            recordFailure(from, -amount, TransactionType.TRANSFER, "Transfer to " + to.getAccountNumber(), e);
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

    /** Pays one month's interest into every ACTIVE interest-bearing account. */
    public List<Transaction> payMonthlyInterest() throws BankingException {
        List<Transaction> paid = new ArrayList<>();
        for (Account account : accountDAO.findAll()) {
            if (checkInterestEligibility(account)) {
                Transaction tx = bankService.payInterest(account);
                if (tx != null) {
                    accountDAO.saveAccount(account);
                    transactionService.recordTransaction(tx);
                    paid.add(tx);
                }
            }
        }
        return paid;
    }

    /** @return the pay-out transaction of the remaining balance, or null if the balance was zero */
    public Transaction closeAccount(String accountNumber) throws BankingException {
        Account account = requireAccount(accountNumber);
        Transaction payout = account.close();
        accountDAO.saveAccount(account);
        transactionService.recordTransaction(payout);
        return payout;
    }

    public void suspendAccount(String accountNumber) throws BankingException {
        Account account = requireAccount(accountNumber);
        account.suspend();
        accountDAO.saveAccount(account);
    }

    public void reactivateAccount(String accountNumber) throws BankingException {
        Account account = requireAccount(accountNumber);
        account.reactivate();
        accountDAO.saveAccount(account);
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

    @FunctionalInterface
    private interface BankingOperation {
        Transaction run() throws BankingException;
    }

    private Transaction execute(Account account, double signedAmount, TransactionType type, String description,
                                BankingOperation operation) throws BankingException {
        try {
            Transaction tx = operation.run();
            accountDAO.saveAccount(account);
            transactionService.recordTransaction(tx);
            return tx;
        } catch (BankingException e) {
            recordFailure(account, signedAmount, type, description, e);
            throw e;
        }
    }

    private void recordFailure(Account account, double signedAmount, TransactionType type, String description,
                               BankingException cause) {
        Transaction failed = new Transaction(account.getAccountNumber(), signedAmount, type, description);
        failed.fail(cause.getMessage(), account.getBalance());
        transactionService.recordTransaction(failed);
    }
}
