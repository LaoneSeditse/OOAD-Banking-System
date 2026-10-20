package com.bankingsystem.model.exception;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(double balance, double requested) {
        super(String.format("Insufficient funds: balance BWP %.2f, requested BWP %.2f", balance, requested));
    }
}
