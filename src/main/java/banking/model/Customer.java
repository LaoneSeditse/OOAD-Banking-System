package banking.model;

import banking.exception.ValidationException;
import banking.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Customer {
    private final String customerId;
    private final User user;
    private String postalAddress;
    private String physicalAddress;
    private double socialIncome;
    private final List<Account> accounts = new ArrayList<>();

    protected Customer(String customerId, User user, String postalAddress,
                       String physicalAddress, double socialIncome) {
        this.customerId = customerId;
        this.user = user;
        this.postalAddress = postalAddress;
        this.physicalAddress = physicalAddress;
        this.socialIncome = socialIncome;
    }

    public void register() {
        if (user == null) {
            throw new ValidationException("A customer needs login details.");
        }
        Validator.requireNotBlank(postalAddress, "Postal address");
        Validator.requireNotBlank(physicalAddress, "Physical address");
        if (socialIncome < 0) {
            throw new ValidationException("Income cannot be negative.");
        }
        validateSpecificDetails();
    }

    protected abstract void validateSpecificDetails();

    public void updateDetails(String postalAddress, String physicalAddress, double socialIncome) {
        this.postalAddress = Validator.requireNotBlank(postalAddress, "Postal address");
        this.physicalAddress = Validator.requireNotBlank(physicalAddress, "Physical address");
        if (socialIncome < 0) {
            throw new ValidationException("Income cannot be negative.");
        }
        this.socialIncome = socialIncome;
    }

    public abstract String getCustomerType();

    public abstract String getDisplayName();

    /** Called only by Account's constructor. */
    void addAccount(Account account) {
        accounts.add(account);
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Account getAccount(String accountNumber) {
        for (Account a : accounts) {
            if (a.getAccountNumber().equals(accountNumber)) {
                return a;
            }
        }
        return null;
    }

    public String getCustomerId() { return customerId; }
    public User getUser() { return user; }
    public String getPostalAddress() { return postalAddress; }
    public String getPhysicalAddress() { return physicalAddress; }
    public double getSocialIncome() { return socialIncome; }
}
