package com.bankingsystem.controller;

import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.CustomerSummary;
import com.bankingsystem.controller.dto.TransactionRow;
import com.bankingsystem.model.Money;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.AdminService;
import com.bankingsystem.service.CustomerService;

import java.util.List;

/**
 * Handles requests from the AdminView (use case: Manage System).
 */
public class AdminController {
    private final CustomerService customerService;
    private final AccountService accountService;
    private final AdminService adminService;
    private final UserSession session;

    public AdminController(CustomerService customerService, AccountService accountService,
                           AdminService adminService, UserSession session) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.adminService = adminService;
        this.session = session;
    }

    public List<CustomerSummary> getAllCustomers() {
        if (!session.isAdmin()) {
            return List.of();
        }
        return customerService.getAllCustomers().stream()
                .map(c -> DtoMapper.toSummary(c, accountService.getAccountsForCustomer(c.getCustomerId())))
                .toList();
    }

    public List<AccountSummary> getAllAccounts() {
        if (!session.isAdmin()) {
            return List.of();
        }
        return accountService.getAllAccounts().stream().map(DtoMapper::toSummary).toList();
    }

    public List<TransactionRow> getAllTransactions() {
        if (!session.isAdmin()) {
            return List.of();
        }
        return accountService.getAllTransactions().stream().map(DtoMapper::toRow).toList();
    }

    public ActionResult<Void> unlockCustomer(String customerId) {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        if (InputValidator.isBlank(customerId)) {
            return ActionResult.fail("Please select a customer");
        }
        customerService.unlockAccount(customerId);
        return ActionResult.ok("Customer " + customerId + " has been unlocked");
    }

    public ActionResult<Void> lockCustomer(String customerId) {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        if (InputValidator.isBlank(customerId)) {
            return ActionResult.fail("Please select a customer");
        }
        customerService.lockAccount(customerId);
        return ActionResult.ok("Customer " + customerId + " has been locked");
    }

    public ActionResult<Void> deleteCustomer(String customerId) {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        if (InputValidator.isBlank(customerId)) {
            return ActionResult.fail("Please select a customer");
        }
        customerService.deleteCustomer(customerId);
        return ActionResult.ok("Customer " + customerId + " and all their accounts have been deleted");
    }

    public ActionResult<Void> suspendAccount(String accountNumber) {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        try {
            accountService.suspendAccount(accountNumber);
            return ActionResult.ok("Account " + accountNumber + " suspended");
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    public ActionResult<Void> reactivateAccount(String accountNumber) {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        try {
            accountService.reactivateAccount(accountNumber);
            return ActionResult.ok("Account " + accountNumber + " reactivated");
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    /** Use case: Pay Interest (includes Calculate Interest). */
    public ActionResult<List<TransactionRow>> payMonthlyInterest() {
        if (!session.isAdmin()) {
            return ActionResult.fail("Administrator access required");
        }
        try {
            List<Transaction> paid = accountService.payMonthlyInterest();
            double total = paid.stream().mapToDouble(Transaction::getAmount).sum();
            return ActionResult.ok(String.format("Monthly interest of %s paid into %d account(s)",
                    Money.format(total), paid.size()), paid.stream().map(DtoMapper::toRow).toList());
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    public String generateReport(String reportType) {
        if (!session.isAdmin()) {
            return "";
        }
        return switch (reportType) {
            case "CUSTOMERS" -> adminService.generateCustomerReport();
            case "TRANSACTIONS" -> adminService.generateTransactionReport();
            case "INTEREST" -> adminService.generateInterestReport();
            case "ACCOUNTS" -> adminService.generateAccountReport();
            default -> adminService.generateAccountReport();
        };
    }
}
