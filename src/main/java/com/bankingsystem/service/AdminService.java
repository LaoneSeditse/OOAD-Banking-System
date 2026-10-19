package com.bankingsystem.service;

import com.bankingsystem.dao.AdministratorDAO;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAdministrator;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.Money;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionStatus;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.util.PasswordUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Administrator authentication and report generation (use case: Manage System / Generate Reports).
 */
public class AdminService {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdministratorDAO administratorDAO;
    private final CustomerService customerService;
    private final AccountService accountService;

    public AdminService(AdministratorDAO administratorDAO, CustomerService customerService,
                        AccountService accountService) {
        this.administratorDAO = administratorDAO;
        this.customerService = customerService;
        this.accountService = accountService;
    }

    public Optional<BankAdministrator> authenticate(String username, String rawPassword) {
        return administratorDAO.findByUsername(username)
                .filter(admin -> admin.login(username, rawPassword));
    }

    public String generateCustomerReport() {
        List<Customer> customers = customerService.getAllCustomers();
        StringBuilder sb = header("CUSTOMER REPORT");
        sb.append(String.format("%-9s %-28s %-11s %-12s %-7s %4s%n", "ID", "Name", "Type", "Username", "Status", "Age"));
        for (Customer c : customers) {
            sb.append(String.format("%-9s %-28s %-11s %-12s %-7s %4d%n", c.getCustomerId(), c.getDisplayName(),
                    c.getCustomerType(), c.getUsername(), c.getStatus(), c.getAge()));
        }
        sb.append(String.format("%nTotal customers: %d%n", customers.size()));
        return sb.toString();
    }

    public String generateAccountReport() {
        List<Account> accounts = accountService.getAllAccounts();
        StringBuilder sb = header("ACCOUNT REPORT");
        for (AccountType type : AccountType.values()) {
            List<Account> ofType = accounts.stream().filter(a -> a.getAccountType() == type).toList();
            double total = ofType.stream().filter(a -> a.getStatus() != AccountStatus.CLOSED)
                    .mapToDouble(Account::getBalance).sum();
            sb.append(String.format("%-20s accounts: %3d   total balance: %s%n",
                    type.getDisplayName(), ofType.size(), Money.format(total)));
        }
        sb.append(String.format("%n%-10s %-12s %-25s %-10s %15s%n", "Number", "Type", "Holder", "Status", "Balance"));
        for (Account a : accounts) {
            sb.append(String.format("%-10s %-12s %-25s %-10s %15s%n", a.getAccountNumber(), a.getAccountType(),
                    a.getHolder().getDisplayName(), a.getStatus(), Money.format(a.getBalance())));
        }
        sb.append(String.format("%nTotal accounts: %d   Total deposits held: %s%n", accounts.size(),
                Money.format(accounts.stream().filter(a -> a.getStatus() != AccountStatus.CLOSED)
                        .mapToDouble(Account::getBalance).sum())));
        return sb.toString();
    }

    public String generateTransactionReport() {
        List<Transaction> all = accountService.getAllTransactions();
        StringBuilder sb = header("TRANSACTION REPORT");
        for (TransactionType type : TransactionType.values()) {
            List<Transaction> ofType = all.stream()
                    .filter(t -> t.getType() == type && t.getStatus() == TransactionStatus.COMPLETED).toList();
            sb.append(String.format("%-11s completed: %4d   value: %s%n", type, ofType.size(),
                    Money.format(ofType.stream().mapToDouble(t -> Math.abs(t.getAmount())).sum())));
        }
        long failed = all.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count();
        sb.append(String.format("Failed transactions: %d%n%nMost recent transactions:%n", failed));
        all.stream().limit(25).forEach(t -> sb.append(t.getAccountNumber()).append(" | ")
                .append(t.getTransactionDetails()).append(System.lineSeparator()));
        return sb.toString();
    }

    public String generateInterestReport() {
        List<Account> accounts = accountService.getAllAccounts();
        StringBuilder sb = header("INTEREST REPORT");
        sb.append("Rates: Investment = 5% monthly, Savings = 0.05% monthly, Cheque = none\n\n");
        sb.append(String.format("%-10s %-12s %-25s %15s %8s %18s%n", "Number", "Type", "Holder", "Balance", "Rate",
                "Next month's int."));
        double projected = 0;
        for (Account a : accounts) {
            if (a.isInterestBearing() && a.getStatus() == AccountStatus.ACTIVE) {
                double interest = a.calculateInterest();
                projected += interest;
                sb.append(String.format("%-10s %-12s %-25s %15s %7.2f%% %18s%n", a.getAccountNumber(),
                        a.getAccountType(), a.getHolder().getDisplayName(), Money.format(a.getBalance()),
                        a.getInterestRate() * 100, Money.format(interest)));
            }
        }
        double paid = accountService.getAllTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INTEREST && t.getStatus() == TransactionStatus.COMPLETED)
                .mapToDouble(Transaction::getAmount).sum();
        sb.append(String.format("%nProjected interest for next month: %s%nTotal interest paid to date: %s%n",
                Money.format(projected), Money.format(paid)));
        return sb.toString();
    }

    private static StringBuilder header(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" - generated ").append(LocalDateTime.now().format(STAMP)).append('\n');
        sb.append("=".repeat(90)).append('\n');
        return sb;
    }
}
