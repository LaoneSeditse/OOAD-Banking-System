package com.bankingsystem.service;

import com.bankingsystem.dao.TransactionDAO;
import com.bankingsystem.model.Transaction;

import java.util.List;

public class TransactionServiceImpl implements TransactionService {
    private final TransactionDAO transactionDAO;

    public TransactionServiceImpl(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    @Override
    public void recordTransaction(Transaction transaction) {
        if (transaction != null) {
            transactionDAO.insert(transaction);
        }
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionDAO.findByAccount(accountNumber);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }
}
