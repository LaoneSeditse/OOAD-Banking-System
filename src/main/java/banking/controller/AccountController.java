package banking.controller;

import banking.model.Account;
import banking.model.AccountType;
import banking.model.Customer;
import banking.model.EmploymentDetails;
import banking.model.Transaction;
import banking.service.BankService;
import banking.util.Validator;

import java.util.List;

public class AccountController {
    private final BankService bankService;

    public AccountController(BankService bankService) {
        this.bankService = bankService;
    }

    public Customer getCustomer(String customerId) {
        return bankService.getCustomer(customerId);
    }

    public List<Account> getAccounts(String customerId) {
        return bankService.getAccounts(customerId);
    }

    public List<Transaction> getTransactions(String accountNumber) {
        return bankService.getTransactions(accountNumber);
    }

    public Account openAccount(String customerId, AccountType type, String initialDepositText, String branch,
                               String employerName, String employerAddress, String employerPosition) {
        double initial = initialDepositText == null || initialDepositText.isBlank()
                ? 0.0 : Validator.parseNonNegative(initialDepositText, "Initial deposit");
        EmploymentDetails employment = type == AccountType.CHEQUE
                ? new EmploymentDetails(employerName, employerAddress, employerPosition) : null;
        return bankService.createAccount(customerId, type, initial, branch, employment);
    }

    public void deposit(String accountNumber, String amountText) {
        bankService.deposit(accountNumber, Validator.parseAmount(amountText, "Amount"));
    }

    public void withdraw(String accountNumber, String amountText) {
        bankService.withdraw(accountNumber, Validator.parseAmount(amountText, "Amount"));
    }

    public void transfer(String fromAccount, String toAccount, String amountText) {
        Validator.requireNotBlank(toAccount, "Destination account number");
        bankService.transfer(fromAccount, toAccount.trim(), Validator.parseAmount(amountText, "Amount"));
    }

    public double getBalance(String accountNumber) {
        return bankService.getAccountBalance(accountNumber);
    }

    public double previewInterest(String accountNumber) {
        return bankService.calculateInterest(accountNumber);
    }

    public void closeAccount(String accountNumber) {
        bankService.closeAccount(accountNumber);
    }
}
