package banking.controller;

import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.Customer;
import banking.service.BankAdministrator;
import banking.service.BankService;
import banking.service.InterestSummary;
import banking.util.Money;

import java.util.List;

public class AdminController {
    private final BankService bankService;

    public AdminController(BankService bankService) {
        this.bankService = bankService;
    }

    public BankAdministrator createAdministrator(String adminId, String name) {
        return new BankAdministrator(adminId, name, name + "@bank.bw", bankService);
    }

    public List<Customer> getCustomers() {
        return bankService.getAllCustomers();
    }

    public List<Account> getAccounts(String customerId) {
        return bankService.getAccounts(customerId);
    }

    public String payMonthlyInterest() {
        InterestSummary s = bankService.payMonthlyInterest();
        return "Interest paid to " + s.accountsCredited() + " account(s), total " + Money.format(s.totalInterest())
                + ".\n" + s.accountsSkipped() + " account(s) skipped (no interest, inactive, or already paid this month).";
    }

    public void unlockCustomer(String customerId) {
        bankService.unlockCustomer(customerId);
    }

    public void suspendAccount(String accountNumber) {
        bankService.setAccountStatus(accountNumber, AccountStatus.SUSPENDED);
    }

    public void reactivateAccount(String accountNumber) {
        bankService.setAccountStatus(accountNumber, AccountStatus.ACTIVE);
    }

    public String getReport() {
        return bankService.generateSummaryReport();
    }
}
