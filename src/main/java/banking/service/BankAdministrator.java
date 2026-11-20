package banking.service;

import banking.model.Account;
import banking.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class BankAdministrator {
    private final String adminId;
    private final String name;
    private final String email;
    private final BankService bankService;

    public BankAdministrator(String adminId, String name, String email, BankService bankService) {
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.bankService = bankService;
    }

    public List<Customer> manageUsers() {
        return bankService.getAllCustomers();
    }

    public List<Account> manageAccounts() {
        List<Account> all = new ArrayList<>();
        for (Customer c : bankService.getAllCustomers()) {
            all.addAll(c.getAccounts());
        }
        return all;
    }

    public String generateReports() {
        return bankService.generateSummaryReport();
    }

    public String getAdminId() { return adminId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
