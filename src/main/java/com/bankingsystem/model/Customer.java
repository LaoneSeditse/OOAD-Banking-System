package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A bank customer ("a User is a Customer"). A customer owns zero or more accounts
 * (composition: an account cannot exist without its holder).
 */
public abstract class Customer extends User {
    private String customerId;
    private String postalAddress;
    private String physicalAddress;
    private double socialIncome;
    private final List<Account> accounts = new ArrayList<>();

    protected Customer(String customerId, String username, String password, LocalDate dateOfBirth,
                       String postalAddress, String physicalAddress, double socialIncome) {
        super(customerId, username, password, dateOfBirth);
        this.customerId = customerId;
        this.postalAddress = postalAddress;
        this.physicalAddress = physicalAddress;
        this.socialIncome = socialIncome;
    }

    /**
     * Validates the customer's details before registration.
     *
     * @throws BankingException if a business rule is violated
     */
    public void register() throws BankingException {
        if (!isAdult()) {
            throw new BankingException("Customers must be at least " + MINIMUM_AGE + " years old");
        }
        if (isBlank(postalAddress) || isBlank(physicalAddress)) {
            throw new BankingException("Postal and physical address are required");
        }
        if (socialIncome < 0) {
            throw new BankingException("Income cannot be negative");
        }
        validateSpecificDetails();
    }

    public void updateDetails(String postalAddress, String physicalAddress, double socialIncome) {
        this.postalAddress = postalAddress;
        this.physicalAddress = physicalAddress;
        this.socialIncome = socialIncome;
    }

    /** Subclass-specific validation (template method used by {@link #register()}). */
    protected abstract void validateSpecificDetails() throws BankingException;

    /** @return "INDIVIDUAL" or "COMPANY" */
    public abstract String getCustomerType();

    /** @return the customer's display name (full name or company name) */
    public abstract String getDisplayName();

    void addAccount(Account account) {
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    void removeAccount(Account account) {
        accounts.remove(account);
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Optional<Account> findAccount(String accountNumber) {
        return accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber)).findFirst();
    }

    public double getTotalBalance() {
        return accounts.stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .mapToDouble(Account::getBalance)
                .sum();
    }

    protected static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
        setUserId(customerId);
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public String getPhysicalAddress() {
        return physicalAddress;
    }

    public double getSocialIncome() {
        return socialIncome;
    }

    @Override
    public String toString() {
        return getCustomerType() + " customer " + getDisplayName() + " (" + customerId + ")";
    }
}
