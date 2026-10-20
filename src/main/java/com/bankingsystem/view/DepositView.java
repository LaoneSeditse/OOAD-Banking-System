package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.TransactionRow;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.function.Predicate;

/**
 * Deposit Page (UI) in the Deposit sequence diagram: collects account, amount and PIN/password.
 */
public class DepositView {
    private static final Predicate<AccountSummary> OPEN = a -> !"CLOSED".equals(a.status());

    private final AccountController controller;

    public DepositView(AccountController controller) {
        this.controller = controller;
    }

    public Node getView() {
        ComboBox<AccountSummary> account = AccountSelector.create("depositAccountCombo", controller.getMyAccounts(), OPEN);
        TextField amount = new TextField();
        amount.setId("depositAmountField");
        amount.setPromptText("Amount in BWP");
        PasswordField pin = new PasswordField();
        pin.setId("depositPinField");
        pin.setPromptText("Your login password");

        GridPane form = ViewUtils.form();
        ViewUtils.addRow(form, 0, "Account", account);
        ViewUtils.addRow(form, 1, "Amount (BWP)", amount);
        ViewUtils.addRow(form, 2, "Password / PIN", pin);

        Label message = ViewUtils.messageLabel();
        Button deposit = ViewUtils.primary("Deposit");
        deposit.setId("depositButton");
        deposit.setOnAction(e -> {
            ActionResult<TransactionRow> result = controller.processDeposit(AccountSelector.number(account),
                    amount.getText(), pin.getText());
            ViewUtils.showResult(message, result);
            pin.clear();
            if (result.success()) {
                amount.clear();
                AccountSelector.refresh(account, controller.getMyAccounts(), OPEN);
            }
        });

        return ViewUtils.card(ViewUtils.title("Deposit Funds"),
                ViewUtils.hint("Deposit into any of your Savings, Investment or Cheque accounts."), form, deposit, message);
    }
}
