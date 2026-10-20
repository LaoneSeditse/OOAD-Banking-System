package com.bankingsystem.view;

import com.bankingsystem.BankingSystem;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Switches between the application's screens and gives each view access to its controllers.
 */
public class ViewNavigator {
    private static final String STYLESHEET = "/styles/app.css";

    private final Stage stage;
    private final BankingSystem system;
    private final Scene scene;

    public ViewNavigator(Stage stage, BankingSystem system) {
        this.stage = stage;
        this.system = system;
        this.scene = new Scene(new javafx.scene.layout.Pane(), 1100, 720);
        scene.getStylesheets().add(ViewNavigator.class.getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.setTitle(system.getName() + " - Banking System");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
    }

    public void showLogin() {
        show(new LoginView(this, system.getAuthenticationController()).getView());
    }

    public void showRegister() {
        show(new RegisterView(this, system.getRegistrationController()).getView());
    }

    public void showDashboard() {
        show(new DashboardView(this, system.getAuthenticationController(), system.getAccountController()).getView());
    }

    public void showAdmin() {
        show(new AdminView(this, system.getAuthenticationController(), system.getAdminController()).getView());
    }

    String bankName() {
        return system.getName();
    }

    private void show(Parent root) {
        scene.setRoot(root);
        if (!stage.isShowing()) {
            stage.show();
        }
    }
}
