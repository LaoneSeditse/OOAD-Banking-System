package com.bankingsystem.controller;

import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.RegistrationRequest;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.User;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.service.CustomerService;

import java.time.LocalDate;
import java.time.Period;
import java.util.OptionalDouble;

/**
 * Validates registration form input and registers new customers (use case: Register Customer).
 */
public class RegistrationController {
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final CustomerService customerService;

    public RegistrationController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** @return on success, the new customer id */
    public ActionResult<String> register(RegistrationRequest r) {
        String error = validate(r);
        if (error != null) {
            return ActionResult.fail(error);
        }
        double income = InputValidator.parseNonNegative(r.income()).orElse(0);
        try {
            Customer customer;
            if ("COMPANY".equals(r.customerType())) {
                customer = customerService.registerCompany(r.username().trim(), r.password(), r.dateOfBirth(),
                        r.companyName().trim(), r.registrationNumber().trim(), r.postalAddress().trim(),
                        r.physicalAddress().trim(), income);
            } else {
                customer = customerService.registerIndividual(r.username().trim(), r.password(), r.dateOfBirth(),
                        r.firstName().trim(), r.lastName().trim(), r.idNumber().trim(), r.postalAddress().trim(),
                        r.physicalAddress().trim(), income);
            }
            return ActionResult.ok("Registration successful! Your customer ID is " + customer.getCustomerId()
                    + ". You can now log in.", customer.getCustomerId());
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    private String validate(RegistrationRequest r) {
        boolean company = "COMPANY".equals(r.customerType());
        if (company) {
            if (InputValidator.isBlank(r.companyName()) || InputValidator.isBlank(r.registrationNumber())) {
                return "Company name and registration number are required";
            }
        } else if (InputValidator.isBlank(r.firstName()) || InputValidator.isBlank(r.lastName())
                || InputValidator.isBlank(r.idNumber())) {
            return "First name, last name and ID number are required";
        }
        if (r.dateOfBirth() == null) {
            return "Date of birth is required";
        }
        if (Period.between(r.dateOfBirth(), LocalDate.now()).getYears() < User.MINIMUM_AGE) {
            return "You must be at least " + User.MINIMUM_AGE + " years old to register";
        }
        if (InputValidator.isBlank(r.postalAddress()) || InputValidator.isBlank(r.physicalAddress())) {
            return "Postal and physical address are required";
        }
        OptionalDouble income = InputValidator.parseNonNegative(r.income());
        if (!InputValidator.isBlank(r.income()) && income.isEmpty()) {
            return "Income must be a valid amount";
        }
        if (InputValidator.isBlank(r.username()) || !r.username().trim().matches("[A-Za-z0-9_.]{3,30}")) {
            return "Username must be 3-30 characters (letters, digits, '_' or '.')";
        }
        if (r.password() == null || r.password().length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        if (!r.password().equals(r.confirmPassword())) {
            return "Passwords do not match";
        }
        return null;
    }
}
