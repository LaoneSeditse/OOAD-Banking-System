package com.bankingsystem.service;

import com.bankingsystem.model.AccountType;

@FunctionalInterface
public interface AccountNumberGenerator {
    String nextAccountNumber(AccountType type);
}
