package banking.controller;

import banking.model.Customer;
import banking.model.User;
import banking.service.BankService;
import banking.util.Validator;

public class LoginController {
    private final BankService bankService;

    public LoginController(BankService bankService) {
        this.bankService = bankService;
    }
    public User login(String username, String password) {
        String name = Validator.requireNotBlank(username, "Username");
        Validator.requireNotBlank(password, "Password");
        return bankService.authenticate(name, password);
    }

    public String getCustomerId(User user) {
        Customer customer = bankService.findCustomerByUserId(user.getUserId());
        return customer == null ? null : customer.getCustomerId();
    }

    public void logout(User user) {
        user.logout();
    }

    public void changePassword(User user, String oldPassword, String newPassword, String confirmPassword) {
        Validator.requireNotBlank(oldPassword, "Current password");
        Validator.requireNotBlank(newPassword, "New password");
        if (!newPassword.equals(confirmPassword)) {
            throw new banking.exception.ValidationException("The new passwords do not match.");
        }
        bankService.changePassword(user.getUserId(), oldPassword, newPassword);
    }
}
