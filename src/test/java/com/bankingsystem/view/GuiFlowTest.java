package com.bankingsystem.view;

import com.bankingsystem.BankingSystem;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.dao.DataSeeder;
import com.bankingsystem.dao.Database;
import com.bankingsystem.model.Account;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GUI integration test: drives the real JavaFX views (headless) through
 * login -> deposit -> persisted balance, matching the Login and Deposit sequence diagrams.
 */
@ExtendWith(ApplicationExtension.class)
class GuiFlowTest {
    private BankingSystem system;

    @Start
    void start(Stage stage) {
        system = new BankingSystem(Database.inMemory("gui-" + UUID.randomUUID()));
        system.startSystem(true);
        new ViewNavigator(stage, system).showLogin();
    }

    @AfterEach
    void tearDown() {
        system.stopSystem();
    }

    @Test
    void invalidLoginShowsErrorMessage(FxRobot robot) {
        robot.clickOn("#usernameField").write("kmolefe");
        robot.clickOn("#passwordField").write("wrong");
        robot.clickOn("#loginButton");
        Label message = robot.lookup("#messageLabel").queryAs(Label.class);
        assertFalse(message.getText().isBlank());
        assertTrue(robot.lookup("#welcomeLabel").tryQuery().isEmpty());
    }

    @Test
    void customerCanLoginAndDeposit(FxRobot robot) {
        robot.clickOn("#usernameField").write("kmolefe");
        robot.clickOn("#passwordField").write(DataSeeder.SAMPLE_PASSWORD);
        robot.clickOn("#loginButton");
        assertTrue(robot.lookup("#welcomeLabel").queryAs(Label.class).getText().contains("Kagiso"));

        robot.clickOn("#navDeposit");
        @SuppressWarnings("unchecked")
        ComboBox<AccountSummary> combo = robot.lookup("#depositAccountCombo").queryAs(ComboBox.class);
        robot.interact(() -> combo.getSelectionModel().selectFirst());
        AccountSummary selected = combo.getValue();
        robot.clickOn("#depositAmountField").write("150.25");
        robot.clickOn("#depositPinField").write(DataSeeder.SAMPLE_PASSWORD);
        robot.clickOn("#depositButton");

        Label message = robot.lookup("#messageLabel").queryAs(Label.class);
        assertTrue(message.getText().startsWith("Deposit successful"), message.getText());
        Account persisted = system.getAccountDAO().getAccount(selected.accountNumber()).orElseThrow();
        assertEquals(selected.balance() + 150.25, persisted.getBalance(), 0.001);

        robot.clickOn("#logoutButton");
        assertTrue(robot.lookup("#loginButton").tryQuery().isPresent());
    }

    @Test
    void administratorCanOpenAdminDashboard(FxRobot robot) {
        robot.clickOn("#usernameField").write(DataSeeder.ADMIN_USERNAME);
        robot.clickOn("#passwordField").write(DataSeeder.ADMIN_PASSWORD);
        robot.clickOn("#adminCheckBox");
        robot.clickOn("#loginButton");
        assertTrue(robot.lookup("#customersTable").tryQuery().isPresent());
    }
}
