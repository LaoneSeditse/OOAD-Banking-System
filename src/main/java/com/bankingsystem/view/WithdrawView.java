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
 * Withdrawal form (use case: Withdraw Funds, extended by Enforce Withdrawal Limit).
 */
public class WithdrawView {
    private static final Predicate<AccountSummary> WITHDRAWABLE = a -> a.allowsWithdrawals() && "ACTIVE".equals(a.status());

    private final AccountController controller;

    public WithdrawView(AccountController controller) {
        this.controller = controller;
    }

    public Node getView() {
        ComboBox<AccountSummary> account = AccountSelector.create("withdrawAccountCombo", controller.getMyAccounts(),
                WITHDRAWABLE);
        TextField amount = new TextField();
        amount.setId("withdrawAmountField");
        amount.setPromptText("Amount in BWP (max 5,000 per withdrawal)");
        PasswordField pin = new PasswordField();
        pin.setId("withdrawPinField");

        GridPane form = ViewUtils.form();
        ViewUtils.addRow(form, 0, "Account", account);
        ViewUtils.addRow(form, 1, "Amount (BWP)", amount);
        ViewUtils.addRow(form, 2, "Password / PIN", pin);

        Label message = ViewUtils.messageLabel();
        Button withdraw = ViewUtils.primary("Withdraw");
        withdraw.setId("withdrawButton");
        withdraw.setOnAction(e -> {
            ActionResult<TransactionRow> result = controller.processWithdrawal(AccountSelector.number(account),
                    amount.getText(), pin.getText());
            ViewUtils.showResult(message, result);
            pin.clear();
            if (result.success()) {
                amount.clear();
                AccountSelector.refresh(account, controller.getMyAccounts(), WITHDRAWABLE);
            }
        });

        return ViewUtils.card(ViewUtils.title("Withdraw Funds"),
                ViewUtils.hint("Savings accounts do not allow withdrawals, so they are not listed. "
                        + "Maximum BWP 5,000.00 per withdrawal."), form, withdraw, message);
    }
}
