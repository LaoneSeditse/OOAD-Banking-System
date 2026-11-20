package banking;

import banking.service.BankService;
import banking.service.BankServiceImpl;
import banking.service.BankingSystem;
import banking.view.AppNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    private BankingSystem system;

    @Override
    public void start(Stage stage) {
        BankService bankService = new BankServiceImpl();
        system = new BankingSystem(bankService);
        system.startSystem();                       // connects to bank.db and inserts sample data once

        AppNavigator navigator = new AppNavigator(stage, bankService);
        navigator.showLogin();
        stage.show();
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
