package banking.view;

import banking.controller.AccountController;
import banking.controller.AdminController;
import banking.controller.LoginController;
import banking.controller.RegistrationController;
import banking.model.Role;
import banking.model.User;
import banking.service.BankService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class AppNavigator {
    private final Stage stage;
    private final LoginController loginController;
    private final RegistrationController registrationController;
    private final AccountController accountController;
    private final AdminController adminController;

    public AppNavigator(Stage stage, BankService bankService) {
        this.stage = stage;
        this.loginController = new LoginController(bankService);
        this.registrationController = new RegistrationController(bankService);
        this.accountController = new AccountController(bankService);
        this.adminController = new AdminController(bankService);
    }

    public void showLogin() {
        show(new LoginView(loginController, this).getRoot(), 460, 460, "Banking System - Login");
    }

    public void showRegister() {
        show(new RegisterView(registrationController, this).getRoot(), 560, 640, "Banking System - Register");
    }

    public void onLoginSuccess(User user) {
        if (user.getRole() == Role.ADMIN) {
            show(new AdminView(adminController, loginController, this, user).getRoot(), 1100, 700,
                    "Banking System - Administrator");
        } else {
            String customerId = loginController.getCustomerId(user);
            show(new AccountView(accountController, loginController, this, user, customerId).getRoot(), 1100, 720,
                    "Banking System - My Accounts");
        }
    }

    private void show(Parent root, double width, double height, String title) {
        Scene scene = new Scene(root, width, height);
        URL css = getClass().getResource("/banking/view/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle(title);
        stage.centerOnScreen();
    }
}
