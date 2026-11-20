package banking.controller;

import banking.exception.ValidationException;
import banking.service.BankService;
import banking.util.Validator;

import java.time.LocalDate;

public class RegistrationController {
    private final BankService bankService;

    public RegistrationController(BankService bankService) {
        this.bankService = bankService;
    }

    public void registerIndividual(String username, String password, String confirmPassword,
                                   String firstName, String lastName, LocalDate dateOfBirth,
                                   String postalAddress, String physicalAddress, String incomeText) {
        checkPasswords(password, confirmPassword);
        if (dateOfBirth == null) {
            throw new ValidationException("Date of birth is required.");
        }
        double income = Validator.parseNonNegative(incomeText, "Income");
        bankService.registerIndividual(username, password, firstName, lastName, dateOfBirth,
                postalAddress, physicalAddress, income);
    }

    public void registerCompany(String username, String password, String confirmPassword,
                                String companyName, String registrationNumber,
                                String postalAddress, String physicalAddress, String incomeText) {
        checkPasswords(password, confirmPassword);
        double income = Validator.parseNonNegative(incomeText, "Income");
        bankService.registerCompany(username, password, companyName, registrationNumber,
                postalAddress, physicalAddress, income);
    }

    private void checkPasswords(String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new ValidationException("The passwords do not match.");
        }
    }
}
