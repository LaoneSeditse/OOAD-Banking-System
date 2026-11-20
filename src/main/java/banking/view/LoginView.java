package banking.view;

import banking.controller.LoginController;
import banking.exception.BankException;
import banking.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginView {
    private final StackPane root = new StackPane();

    public LoginView(LoginController controller, AppNavigator navigator) {
        Label title = new Label("Banking System");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Sign in to your accounts");
        subtitle.getStyleClass().add("subtitle");

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label message = new Label();
        message.getStyleClass().add("error-text");
        message.setWrapText(true);

        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> {
            try {
                User user = controller.login(username.getText(), password.getText());
                navigator.onLoginSuccess(user);
            } catch (BankException ex) {
                message.setText(ex.getMessage());
                password.clear();
            }
        });

        Hyperlink register = new Hyperlink("New customer? Register here");
        register.setOnAction(e -> navigator.showRegister());

        VBox form = new VBox(12, title, subtitle, username, password, loginButton, message, register);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.setMaxWidth(340);
        form.getStyleClass().add("card");

        root.getChildren().add(form);
        root.getStyleClass().add("background");
    }

    public Parent getRoot() {
        return root;
    }
}
