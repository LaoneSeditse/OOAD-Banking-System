package com.bankingsystem;

import com.bankingsystem.view.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point. Starts the {@link BankingSystem} (database, DAOs, services, controllers),
 * seeds the sample data on first run, and shows the login screen.
 */
public class BankingApp extends Application {
    private BankingSystem system;

    @Override
    public void start(Stage stage) {
        system = BankingSystem.createDefault();
        system.startSystem(true);
        new ViewNavigator(stage, system).showLogin();
    }

    @Override
    public void stop() {
        if (system != null) {
            system.stopSystem();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
