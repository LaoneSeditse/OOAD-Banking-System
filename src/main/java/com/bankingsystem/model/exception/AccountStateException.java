package com.bankingsystem.model.exception;

/**
 * Thrown when an operation is not permitted in the account's current state
 * (e.g. withdrawing from a SUSPENDED account or depositing into a CLOSED one).
 */
public class AccountStateException extends BankingException {
    public AccountStateException(String message) {
        super(message);
    }
}
