package com.bankingsystem.controller;

import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.CustomerSummary;
import com.bankingsystem.controller.dto.TransactionRow;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Converts domain objects into read-only DTOs so the views never touch the domain model.
 */
final class DtoMapper {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DtoMapper() {
    }

    static AccountSummary toSummary(Account a) {
        return new AccountSummary(a.getAccountNumber(), a.getAccountType().name(),
                a.getAccountType().getDisplayName(), a.getBalance(), a.getStatus().name(),
                a.getInterestRate() * 100, a.getBranch(), a.getOpenedDate().toString(), a.allowsWithdrawals(),
                a.getHolder().getDisplayName(), a.getHolder().getCustomerId());
    }

    static TransactionRow toRow(Transaction t) {
        return new TransactionRow(t.getTransactionId(), t.getDate().format(DATE_TIME), t.getAccountNumber(),
                t.getType().name(), t.getAmount(), t.getDescription(), t.getBalanceAfter(), t.getStatus().name());
    }

    static CustomerSummary toSummary(Customer c, List<Account> accounts) {
        return new CustomerSummary(c.getCustomerId(), c.getDisplayName(), c.getCustomerType(), c.getUsername(),
                c.getStatus().name(), c.getAge(), accounts.size(),
                accounts.stream().mapToDouble(Account::getBalance).sum());
    }
}
