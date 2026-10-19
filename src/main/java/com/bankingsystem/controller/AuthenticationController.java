package com.bankingsystem.controller;

import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.model.BankAdministrator;
import com.bankingsystem.model.Customer;
import com.bankingsystem.service.AdminService;
import com.bankingsystem.service.AuthResult;
import com.bankingsystem.service.CustomerService;

import java.util.Optional;

/**
 * Handles login / logout requests from the LoginView (AuthenticationController in the Login sequence diagram).
 */
public class AuthenticationController {
    private final CustomerService customerService;
    private final AdminService adminService;
    private final UserSession session;

    public AuthenticationController(CustomerService customerService, AdminService adminService, UserSession session) {
        this.customerService = customerService;
        this.adminService = adminService;
        this.session = session;
    }

    /** Steps 2-15 of the Login sequence diagram. On success the user session is started. */
    public ActionResult<String> validateCredentials(String username, String password) {
        if (InputValidator.isBlank(username) || InputValidator.isBlank(password)) {
            return ActionResult.fail("Please enter both your username and password");
        }
        AuthResult result = customerService.authenticateUser(username.trim(), password);
        return switch (result.status()) {
            case SUCCESS -> {
                Customer customer = result.customer();
                String token = customerService.createSession(customer.getCustomerId());
                session.startCustomer(customer.getCustomerId(), customer.getDisplayName(),
                        customer.getCustomerType(), token);
                yield ActionResult.ok("Login successful. Welcome, " + customer.getDisplayName() + "!",
                        customer.getDisplayName());
            }
            case INVALID_CREDENTIALS -> ActionResult.fail(result.customer() == null
                    ? "Invalid username or password"
                    : "Invalid username or password. " + result.attemptsRemaining() + " attempt(s) remaining before your account is locked");
            case LOCKED_NOW -> ActionResult.fail(
                    "Your account has been locked after 3 failed login attempts. Please contact the bank administrator.");
            case ACCOUNT_LOCKED -> ActionResult.fail(
                    "This account is locked. Please contact the bank administrator to unlock it.");
        };
    }

    public ActionResult<String> loginAdministrator(String username, String password) {
        if (InputValidator.isBlank(username) || InputValidator.isBlank(password)) {
            return ActionResult.fail("Please enter both your username and password");
        }
        Optional<BankAdministrator> admin = adminService.authenticate(username.trim(), password);
        if (admin.isEmpty()) {
            return ActionResult.fail("Invalid administrator credentials");
        }
        session.startAdmin(admin.get().getAdminId(), admin.get().getName());
        return ActionResult.ok("Welcome, " + admin.get().getName(), admin.get().getName());
    }

    public void logout() {
        if (session.isCustomer() && session.getToken() != null) {
            customerService.endSession(session.getToken());
        }
        session.clear();
    }

    public UserSession getSession() {
        return session;
    }
}
