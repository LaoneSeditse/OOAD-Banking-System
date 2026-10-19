package com.bankingsystem.service;

import com.bankingsystem.model.Transaction;

import java.util.List;

/**
 * Records and retrieves transactions for auditing and history (see Deposit sequence diagram).
 */
public interface TransactionService {

    void recordTransaction(Transaction transaction);

    List<Transaction> getTransactionHistory(String accountNumber);

    List<Transaction> getAllTransactions();
}
