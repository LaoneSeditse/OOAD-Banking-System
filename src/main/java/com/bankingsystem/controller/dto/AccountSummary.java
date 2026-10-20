package com.bankingsystem.controller.dto;

/** Read-only account data shown by the views. */
public record AccountSummary(String accountNumber, String accountType, String accountTypeName, double balance,
                             String status, double monthlyInterestPercent, String branch, String openedDate,
                             boolean allowsWithdrawals, String holderName, String customerId) {

    public String label() {
        return accountNumber + " - " + accountTypeName + String.format(" (BWP %,.2f)", balance);
    }
}
