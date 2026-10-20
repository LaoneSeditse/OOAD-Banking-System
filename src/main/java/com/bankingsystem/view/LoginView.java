package com.bankingsystem.view;

import com.bankingsystem.controller.AuthenticationController;
import com.bankingsystem.controller.dto.ActionResult;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login screen (Login Page (UI) in the Login sequence diagram). Collects credentials and
 * forwards them to the {@link AuthenticationController}.
 */
public class LoginView {
    private final ViewNavigator navigator;
    private final AuthenticationController controller;

    public LoginView(ViewNavigator navigator, AuthenticationController controller) {
        this.navigator = navigator;
        this.controller = controller;
    }

    public Parent getView() {
        Label bank = new Label(navigator.bankName());
        bank.getStyleClass().add("page-title");
        bank.setStyle("-fx-font-size: 26px;");
        Label subtitle = ViewUtils.hint("Sign in to manage your accounts");

        TextField username = new TextField();
        username.setId("usernameField");
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setId("passwordField");
        password.setPromptText("Password");
        CheckBox adminLogin = new CheckBox("Log in as bank administrator");
        adminLogin.setId("adminCheckBox");

        GridPane form = ViewUtils.form();
        ViewUtils.addRow(form, 0, "Username", username);
        ViewUtils.addRow(form, 1, "Password", password);
        form.add(adminLogin, 1, 2);

        Label message = ViewUtils.messageLabel();
        Button login = ViewUtils.primary("Login");
        login.setId("loginButton");
        login.setOnAction(e -> {
            ActionResult<String> result = adminLogin.isSelected()
                    ? controller.loginAdministrator(username.getText(), password.getText())
                    : controller.validateCredentials(username.getText(), password.getText());
            if (result.success()) {
                if (adminLogin.isSelected()) {
                    navigator.showAdmin();
                } else {
                    navigator.showDashboard();
                }
            } else {
                password.clear();
                ViewUtils.showResult(message, result);
            }
        });

        Button register = new Button("New customer? Register here");
        register.setId("registerLink");
        register.getStyleClass().add("link-button");
        register.setOnAction(e -> navigator.showRegister());

        HBox buttons = new HBox(12, login, register);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox card = ViewUtils.card(bank, subtitle, form, buttons, message,
                ViewUtils.hint("Demo: customer kmolefe / password123  -  administrator admin / admin123"));
        card.setMaxWidth(520);
        card.setMaxHeight(380);
        return new StackPane(card);
    }
}
