package com.bankingsystem.service;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.exception.BankingException;

import java.util.List;

/**
 * Core banking operations on the domain model (see Class Diagram: «interface» BankService).
 */
public interface BankService {

    Account createAccount(Customer customer, AccountType type, double initialDeposit) throws BankingException;

    /** Overloaded: open an account at a specific branch, with employment details for a Cheque account. */
    Account createAccount(Customer customer, AccountType type, double initialDeposit, String branch,
                          EmploymentDetails employmentDetails) throws BankingException;

    Transaction deposit(Account account, double amount) throws BankingException;

    Transaction withdraw(Account account, double amount) throws BankingException;

    double calculateInterest(Account account);

    Transaction payInterest(Account account) throws BankingException;

    double getAccountBalance(Account account);

    /** @return the debit and credit transactions of the transfer */
    List<Transaction> transfer(Account from, Account to, double amount) throws BankingException;
}
