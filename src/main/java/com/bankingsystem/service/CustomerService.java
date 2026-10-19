package com.bankingsystem.service;

import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.SessionDAO;
import com.bankingsystem.model.CompanyCustomer;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.IndividualCustomer;
import com.bankingsystem.model.UserStatus;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.util.PasswordUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Customer management and authentication (CustomerService lifeline in the Login sequence diagram).
 */
public class CustomerService {
    private final CustomerDAO customerDAO;
    private final SessionDAO sessionDAO;

    public CustomerService(CustomerDAO customerDAO, SessionDAO sessionDAO) {
        this.customerDAO = customerDAO;
        this.sessionDAO = sessionDAO;
    }

    public AuthResult authenticateUser(String username, String rawPassword) {
        Optional<Customer> found = customerDAO.findCustomer(username);
        if (found.isEmpty()) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null);
        }
        Customer customer = found.get();
        if (customer.getStatus() == UserStatus.LOCKED || !customer.isActive()) {
            return new AuthResult(AuthStatus.ACCOUNT_LOCKED, customer);
        }
        boolean ok = customer.login(username, rawPassword);
        customerDAO.updateLoginState(customer);
        if (ok) {
            return new AuthResult(AuthStatus.SUCCESS, customer);
        }
        return customer.getStatus() == UserStatus.LOCKED
                ? new AuthResult(AuthStatus.LOCKED_NOW, customer)
                : new AuthResult(AuthStatus.INVALID_CREDENTIALS, customer);
    }

    public boolean verifyPassword(String customerId, String rawPassword) {
        return customerDAO.findById(customerId)
                .filter(c -> c.isActive() && c.getStatus() == UserStatus.ACTIVE)
                .map(c -> PasswordUtil.matches(rawPassword, c.getPassword()))
                .orElse(false);
    }

    /** @return a new session token stored in the database */
    public String createSession(String customerId) {
        String token = UUID.randomUUID().toString();
        sessionDAO.saveSession(customerId, token);
        return token;
    }

    public void endSession(String token) {
        sessionDAO.deleteSession(token);
    }

    public void lockAccount(String customerId) {
        customerDAO.updateStatus(customerId, UserStatus.LOCKED);
    }

    public void unlockAccount(String customerId) {
        customerDAO.updateStatus(customerId, UserStatus.ACTIVE);
    }

    public Customer registerIndividual(String username, String rawPassword, LocalDate dateOfBirth,
                                       String firstName, String lastName, String idNumber,
                                       String postalAddress, String physicalAddress, double income)
            throws BankingException {
        ensureUsernameAvailable(username);
        Customer customer = new IndividualCustomer(null, username, PasswordUtil.hash(rawPassword),
                dateOfBirth, firstName, lastName, idNumber, postalAddress, physicalAddress, income);
        return register(customer);
    }

    public Customer registerCompany(String username, String rawPassword, LocalDate representativeDateOfBirth,
                                    String companyName, String registrationNumber,
                                    String postalAddress, String physicalAddress, double income)
            throws BankingException {
        ensureUsernameAvailable(username);
        Customer customer = new CompanyCustomer(null, username, PasswordUtil.hash(rawPassword),
                representativeDateOfBirth, companyName, registrationNumber, postalAddress, physicalAddress, income);
        return register(customer);
    }

    private Customer register(Customer customer) throws BankingException {
        customer.register();
        customerDAO.insert(customer);
        return customer;
    }

    private void ensureUsernameAvailable(String username) throws BankingException {
        if (customerDAO.usernameExists(username)) {
            throw new BankingException("Username '" + username + "' is already taken");
        }
    }

    public Optional<Customer> findCustomer(String customerId) {
        return customerDAO.findById(customerId);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public void updateDetails(String customerId, String postalAddress, String physicalAddress, double income)
            throws BankingException {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new BankingException("Customer not found"));
        customer.updateDetails(postalAddress, physicalAddress, income);
        customerDAO.update(customer);
    }

    public void deleteCustomer(String customerId) {
        customerDAO.delete(customerId);
    }
}
