package com.bankingsystem.model.exception;

/**
 * Base checked exception for every business-rule violation in the banking domain.
 */
public class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }
}
