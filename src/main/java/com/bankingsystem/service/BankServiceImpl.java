package com.bankingsystem.service;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.ChequeAccount;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.InvestmentAccount;
import com.bankingsystem.model.SavingsAccount;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.exception.AccountStateException;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.model.exception.InvalidTransactionException;

import java.util.List;

public class BankServiceImpl implements BankService {
    public static final String DEFAULT_BRANCH = "Gaborone Main";

    private final AccountNumberGenerator accountNumberGenerator;

    public BankServiceImpl(AccountNumberGenerator accountNumberGenerator) {
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Override
    public Account createAccount(Customer customer, AccountType type, double initialDeposit) throws BankingException {
        return createAccount(customer, type, initialDeposit, DEFAULT_BRANCH, null);
    }

    @Override
    public Account createAccount(Customer customer, AccountType type, double initialDeposit, String branch,
                                 EmploymentDetails employmentDetails) throws BankingException {
        if (customer == null) {
            throw new InvalidTransactionException("An account cannot exist without a customer");
        }
        String number = accountNumberGenerator.nextAccountNumber(type);
        String accountBranch = branch == null || branch.isBlank() ? DEFAULT_BRANCH : branch;
        Account account = switch (type) {
            case SAVINGS -> new SavingsAccount(customer, number, accountBranch);
            case INVESTMENT -> new InvestmentAccount(customer, number, accountBranch);
            case CHEQUE -> new ChequeAccount(customer, number, accountBranch, employmentDetails);
        };
        account.open(initialDeposit);
        return account;
    }

    @Override
    public Transaction deposit(Account account, double amount) throws BankingException {
        return account.deposit(amount);
    }

    @Override
    public Transaction withdraw(Account account, double amount) throws BankingException {
        return account.withdraw(amount);
    }

    @Override
    public double calculateInterest(Account account) {
        return account.calculateInterest();
    }

    @Override
    public Transaction payInterest(Account account) throws BankingException {
        return account.applyMonthlyInterest();
    }

    @Override
    public double getAccountBalance(Account account) {
        return account.getBalance();
    }

    @Override
    public List<Transaction> transfer(Account from, Account to, double amount) throws BankingException {
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        if (!to.canReceiveFunds()) {
            throw new AccountStateException("Destination account " + to.getAccountNumber() + " cannot receive funds");
        }
        Transaction debit = from.transferOut(amount, to.getAccountNumber());
        Transaction credit = to.transferIn(amount, from.getAccountNumber());
        return List.of(debit, credit);
    }
}
