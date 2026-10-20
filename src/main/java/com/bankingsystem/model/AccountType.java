package com.bankingsystem.model;

public enum AccountType {
    SAVINGS("Savings Account"),
    INVESTMENT("Investment Account"),
    CHEQUE("Cheque Account");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
