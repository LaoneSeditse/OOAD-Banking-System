package banking.service;

import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.AccountType;
import banking.model.Customer;
import banking.model.EmploymentDetails;
import banking.model.Transaction;
import banking.model.User;

import java.time.LocalDate;
import java.util.List;

public interface BankService {

    // ----- operations from the class diagram -----
    Account createAccount(String customerId, AccountType type, double initialDeposit,
                          String branch, EmploymentDetails employment);

    void deposit(String accountNumber, double amount);

    void withdraw(String accountNumber, double amount);

    double calculateInterest(String accountNumber);

    double getAccountBalance(String accountNumber);

    void transfer(String fromAccountNumber, String toAccountNumber, double amount);

    // ----- registration and authentication -----
    Customer registerIndividual(String username, String password, String firstName, String lastName,
                                LocalDate dateOfBirth, String postalAddress, String physicalAddress,
                                double socialIncome);

    Customer registerCompany(String username, String password, String companyName, String registrationName,
                             String postalAddress, String physicalAddress, double socialIncome);

    User createAdministrator(String username, String password);

    boolean usernameExists(String username);

    User authenticate(String username, String password);

    void changePassword(String userId, String oldPassword, String newPassword);

    void unlockCustomer(String customerId);

    // ----- queries -----
    Customer getCustomer(String customerId);

    Customer findCustomerByUserId(String userId);

    List<Customer> getAllCustomers();

    List<Account> getAccounts(String customerId);

    List<Transaction> getTransactions(String accountNumber);

    // ----- account administration -----
    void closeAccount(String accountNumber);

    void setAccountStatus(String accountNumber, AccountStatus status);

    InterestSummary payMonthlyInterest();

    String generateSummaryReport();
}
