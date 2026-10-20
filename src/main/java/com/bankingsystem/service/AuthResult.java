package com.bankingsystem.service;

import com.bankingsystem.model.Customer;
import com.bankingsystem.model.User;

public record AuthResult(AuthStatus status, Customer customer) {

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }

    public int attemptsRemaining() {
        return customer == null ? User.MAX_FAILED_ATTEMPTS
                : Math.max(0, User.MAX_FAILED_ATTEMPTS - customer.getFailedLoginAttempts());
    }
}
