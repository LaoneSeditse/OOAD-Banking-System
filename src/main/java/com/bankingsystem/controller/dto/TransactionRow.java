package com.bankingsystem.controller.dto;

/** Read-only transaction data shown by the views. */
public record TransactionRow(String transactionId, String date, String accountNumber, String type, double amount,
                             String description, double balanceAfter, String status) {
}
