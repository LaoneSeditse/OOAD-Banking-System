package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Form for opening a Savings, Investment or Cheque account (use case: Open Account / Verify Employment).
 */
public class OpenAccountView {
    private final AccountController controller;
    private final DashboardView dashboard;

    public OpenAccountView(AccountController controller, DashboardView dashboard) {
        this.controller = controller;
        this.dashboard = dashboard;
    }

    public Node getView() {
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(controller.getAccountTypes()));
        type.setId("accountTypeCombo");
        ComboBox<String> branch = new ComboBox<>(FXCollections.observableArrayList(controller.getBranches()));
        branch.setId("branchCombo");
        branch.getSelectionModel().selectFirst();
        TextField deposit = new TextField();
        deposit.setId("initialDepositField");
        deposit.setPromptText("Opening deposit in BWP");
        TextField employer = new TextField();
        employer.setId("employerField");
        TextField employerAddress = new TextField();
        employerAddress.setId("employerAddressField");
        TextField position = new TextField();
        position.setId("positionField");
        Label description = ViewUtils.hint("Select an account type to see its rules.");

        GridPane form = ViewUtils.form();
        Runnable layout = () -> {
            form.getChildren().clear();
            ViewUtils.addRow(form, 0, "Account type", type);
            ViewUtils.addRow(form, 1, "Branch", branch);
            ViewUtils.addRow(form, 2, "Opening deposit", deposit);
            if ("CHEQUE".equals(type.getValue())) {
                ViewUtils.addRow(form, 3, "Employer name", employer);
                ViewUtils.addRow(form, 4, "Employer address", employerAddress);
                ViewUtils.addRow(form, 5, "Your position", position);
            }
        };
        type.valueProperty().addListener((obs, o, n) -> {
            description.setText(n == null ? "" : controller.describeAccountType(n));
            layout.run();
        });
        layout.run();

        Label message = ViewUtils.messageLabel();
        Button open = ViewUtils.primary("Open account");
        open.setId("openAccountButton");
        open.setOnAction(e -> {
            ActionResult<AccountSummary> result = controller.openAccount(type.getValue(), branch.getValue(),
                    deposit.getText(), employer.getText(), employerAddress.getText(), position.getText());
            ViewUtils.showResult(message, result);
            if (result.success()) {
                deposit.clear();
            }
        });
        Button viewAccounts = ViewUtils.secondary("View my accounts");
        viewAccounts.setOnAction(e -> dashboard.showSection(0));

        return ViewUtils.card(ViewUtils.title("Open a New Account"), description, form,
                new javafx.scene.layout.HBox(12, open, viewAccounts), message);
    }
}
