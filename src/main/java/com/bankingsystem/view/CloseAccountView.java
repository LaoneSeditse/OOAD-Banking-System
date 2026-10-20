package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import java.util.function.Predicate;

/**
 * Close an account (use case: Close Account). Any remaining balance is paid out.
 */
public class CloseAccountView {
    private static final Predicate<AccountSummary> OPEN = a -> !"CLOSED".equals(a.status());

    private final AccountController controller;

    public CloseAccountView(AccountController controller) {
        this.controller = controller;
    }

    public Node getView() {
        ComboBox<AccountSummary> account = AccountSelector.create("closeAccountCombo", controller.getMyAccounts(), OPEN);
        PasswordField pin = new PasswordField();
        pin.setId("closePinField");
        GridPane form = ViewUtils.form();
        ViewUtils.addRow(form, 0, "Account", account);
        ViewUtils.addRow(form, 1, "Password / PIN", pin);

        Label message = ViewUtils.messageLabel();
        Button close = new Button("Close account");
        close.getStyleClass().add("danger-button");
        close.setOnAction(e -> {
            if (account.getValue() == null) {
                ViewUtils.show(message, false, "Please select an account");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Close account " + account.getValue().accountNumber() + "? This cannot be undone.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm account closure");
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
            ActionResult<Void> result = controller.closeAccount(AccountSelector.number(account), pin.getText());
            ViewUtils.showResult(message, result);
            pin.clear();
            if (result.success()) {
                AccountSelector.refresh(account, controller.getMyAccounts(), OPEN);
                account.setValue(null);
            }
        });

        return ViewUtils.card(ViewUtils.title("Close Account"),
                ViewUtils.hint("A closed account blocks all transactions. Any remaining balance will be paid out to you."),
                form, close, message);
    }
}
