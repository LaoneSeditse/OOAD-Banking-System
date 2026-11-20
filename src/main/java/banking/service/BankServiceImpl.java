package banking.service;

import banking.dao.AccountDAO;
import banking.dao.CustomerDAO;
import banking.dao.Database;
import banking.dao.TransactionDAO;
import banking.dao.UserDAO;
import banking.exception.BankException;
import banking.exception.ValidationException;
import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.AccountType;
import banking.model.ChequeAccount;
import banking.model.CompanyCustomer;
import banking.model.Customer;
import banking.model.EmploymentDetails;
import banking.model.IndividualCustomer;
import banking.model.InvestmentAccount;
import banking.model.Role;
import banking.model.SavingsAccount;
import banking.model.Transaction;
import banking.model.TransactionType;
import banking.model.User;
import banking.util.IdGenerator;
import banking.util.Money;
import banking.util.PasswordUtil;
import banking.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class BankServiceImpl implements BankService {
    private final UserDAO userDAO = new UserDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    // ===================== registration / authentication =====================

    @Override
    public Customer registerIndividual(String username, String password, String firstName, String lastName,
                                       LocalDate dateOfBirth, String postalAddress, String physicalAddress,
                                       double socialIncome) {
        User user = newCustomerUser(username, password, dateOfBirth);
        IndividualCustomer customer = new IndividualCustomer(IdGenerator.customerId(), user,
                firstName == null ? null : firstName.trim(), lastName == null ? null : lastName.trim(),
                postalAddress == null ? null : postalAddress.trim(),
                physicalAddress == null ? null : physicalAddress.trim(), socialIncome);
        customer.register();
        customerDAO.insert(customer);
        return customer;
    }

    @Override
    public Customer registerCompany(String username, String password, String companyName, String registrationName,
                                    String postalAddress, String physicalAddress, double socialIncome) {
        User user = newCustomerUser(username, password, null);
        CompanyCustomer customer = new CompanyCustomer(IdGenerator.customerId(), user,
                companyName == null ? null : companyName.trim(),
                registrationName == null ? null : registrationName.trim(),
                postalAddress == null ? null : postalAddress.trim(),
                physicalAddress == null ? null : physicalAddress.trim(), socialIncome);
        customer.register();
        customerDAO.insert(customer);
        return customer;
    }

    private User newCustomerUser(String username, String password, LocalDate dateOfBirth) {
        String name = validateCredentials(username, password);
        return new User(IdGenerator.userId(), name, PasswordUtil.hash(password), dateOfBirth, Role.CUSTOMER, true, 0);
    }

    @Override
    public User createAdministrator(String username, String password) {
        String name = validateCredentials(username, password);
        User admin = new User(IdGenerator.userId(), name, PasswordUtil.hash(password), null, Role.ADMIN, true, 0);
        userDAO.insert(admin);
        return admin;
    }

    private String validateCredentials(String username, String password) {
        String name = Validator.requireNotBlank(username, "Username");
        if (name.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters.");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (usernameExists(name)) {
            throw new ValidationException("That username is already taken.");
        }
        return name;
    }

    @Override
    public boolean usernameExists(String username) {
        return userDAO.findByUsername(username) != null;
    }

    @Override
    public User authenticate(String username, String password) {
        User user = userDAO.findByUsername(username == null ? "" : username.trim());
        if (user == null) {
            throw new BankException("Invalid username or password.");
        }
        if (!user.isActive()) {
            throw new BankException("This login is locked after too many failed attempts. Please contact the bank administrator.");
        }
        boolean ok = user.login(user.getUsername(), password);
        userDAO.update(user);   // saves the reset or the extra failed attempt
        if (ok) {
            return user;
        }
        if (!user.isActive()) {
            throw new BankException("Too many failed attempts. Your login has been locked.");
        }
        throw new BankException("Invalid username or password. Attempts left: " + user.getRemainingAttempts());
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BankException("User not found.");
        }
        if (!user.passwordMatches(oldPassword)) {
            throw new BankException("The current password is incorrect.");
        }
        user.updatePassword(newPassword);
        userDAO.update(user);
    }

    @Override
    public void unlockCustomer(String customerId) {
        Customer customer = requireCustomer(customerId);
        customer.getUser().unlock();
        userDAO.update(customer.getUser());
    }

    // ===================== account operations =====================

    @Override
    public Account createAccount(String customerId, AccountType type, double initialDeposit,
                                 String branch, EmploymentDetails employment) {
        Customer customer = requireCustomer(customerId);
        if (type == null) {
            throw new ValidationException("Please choose an account type.");
        }
        String accountNumber = IdGenerator.accountNumber();
        String branchName = Validator.requireNotBlank(branch, "Branch");

        Account account = switch (type) {
            case SAVINGS -> SavingsAccount.open(accountNumber, customer, branchName, initialDeposit);
            case INVESTMENT -> InvestmentAccount.open(accountNumber, customer, branchName, initialDeposit);
            case CHEQUE -> ChequeAccount.open(accountNumber, customer, branchName, initialDeposit, employment);
        };

        Database.getInstance().runInTransaction(() -> {
            accountDAO.insert(account);
            account.getTransactions().forEach(transactionDAO::insert);
        });
        return account;
    }

    @Override
    public void deposit(String accountNumber, double amount) {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);                 // polymorphic call
        persist(account, account.getLastTransaction());
    }

    @Override
    public void withdraw(String accountNumber, double amount) {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);                // polymorphic call
        persist(account, account.getLastTransaction());
    }

    @Override
    public double calculateInterest(String accountNumber) {
        return requireAccount(accountNumber).calculateInterest();
    }

    @Override
    public double getAccountBalance(String accountNumber) {
        return requireAccount(accountNumber).getBalance();
    }

    @Override
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new ValidationException("You cannot transfer to the same account.");
        }
        Account from = requireAccount(fromAccountNumber);
        Account to = requireAccount(toAccountNumber);
        from.transferOut(amount, toAccountNumber);
        to.transferIn(amount, fromAccountNumber);
        Database.getInstance().runInTransaction(() -> {
            persist(from, from.getLastTransaction());
            persist(to, to.getLastTransaction());
        });
    }

    @Override
    public void closeAccount(String accountNumber) {
        Account account = requireAccount(accountNumber);
        Transaction payout = account.close();
        persist(account, payout);
    }

    @Override
    public void setAccountStatus(String accountNumber, AccountStatus status) {
        Account account = requireAccount(accountNumber);
        switch (status) {
            case SUSPENDED -> account.suspend();
            case ACTIVE -> account.reactivate();
            case CLOSED -> throw new BankException("Use Close Account to close an account.");
        }
        persist(account, null);
    }

    @Override
    public InterestSummary payMonthlyInterest() {
        YearMonth thisMonth = YearMonth.now();
        int credited = 0;
        int skipped = 0;
        double total = 0;
        for (Customer customer : customerDAO.findAll()) {
            for (Account account : customer.getAccounts()) {
                if (account.getStatus() != AccountStatus.ACTIVE || account.calculateInterest() <= 0
                        || hasInterestFor(account, thisMonth)) {
                    skipped++;
                    continue;
                }
                Transaction t = account.applyInterest();
                persist(account, t);
                credited++;
                total += t.getAmount();
            }
        }
        return new InterestSummary(credited, skipped, Money.round(total));
    }

    private boolean hasInterestFor(Account account, YearMonth month) {
        for (Transaction t : account.getTransactions()) {
            LocalDateTime d = t.getDate();
            if (t.getType() == TransactionType.INTEREST && YearMonth.from(d).equals(month)) {
                return true;
            }
        }
        return false;
    }

    // ===================== queries =====================

    @Override
    public Customer getCustomer(String customerId) {
        return requireCustomer(customerId);
    }

    @Override
    public Customer findCustomerByUserId(String userId) {
        return customerDAO.findByUserId(userId);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    @Override
    public List<Account> getAccounts(String customerId) {
        return new ArrayList<>(requireCustomer(customerId).getAccounts());
    }

    @Override
    public List<Transaction> getTransactions(String accountNumber) {
        return new ArrayList<>(requireAccount(accountNumber).getTransactions());
    }

    @Override
    public String generateSummaryReport() {
        List<Customer> customers = customerDAO.findAll();
        int individuals = 0, companies = 0, savings = 0, investment = 0, cheque = 0;
        double total = 0;
        for (Customer c : customers) {
            if (c instanceof IndividualCustomer) individuals++; else companies++;
            for (Account a : c.getAccounts()) {
                total += a.getBalance();
                switch (a.getAccountType()) {
                    case SAVINGS -> savings++;
                    case INVESTMENT -> investment++;
                    case CHEQUE -> cheque++;
                }
            }
        }
        return String.format("BANK SUMMARY%n"
                        + "Customers: %d (%d individual, %d company)%n"
                        + "Accounts: %d (%d Savings, %d Investment, %d Cheque)%n"
                        + "Total balance held: %s",
                customers.size(), individuals, companies,
                savings + investment + cheque, savings, investment, cheque, Money.format(total));
    }

    // ===================== helpers =====================

    private Customer requireCustomer(String customerId) {
        Customer customer = customerDAO.findById(customerId);
        if (customer == null) {
            throw new BankException("Customer not found.");
        }
        return customer;
    }

    private Account requireAccount(String accountNumber) {
        String number = Validator.requireNotBlank(accountNumber, "Account number");
        Customer owner = customerDAO.findByAccountNumber(number);
        Account account = owner == null ? null : owner.getAccount(number);
        if (account == null) {
            throw new BankException("Account " + number + " was not found.");
        }
        return account;
    }

    private void persist(Account account, Transaction newTransaction) {
        Database.getInstance().runInTransaction(() -> {
            accountDAO.update(account);
            if (newTransaction != null) {
                transactionDAO.insert(newTransaction);
            }
        });
    }
}
