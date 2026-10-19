package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.TransactionRow;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.function.Predicate;

/**
 * Transfer form (use case: Transfer Funds) - between own accounts or to another customer's account.
 */
public class TransferView {
    private static final Predicate<AccountSummary> SOURCE = a -> a.allowsWithdrawals() && "ACTIVE".equals(a.status());

    private final AccountController controller;

    public TransferView(AccountController controller) {
        this.controller = controller;
    }

    public Node getView() {
        List<AccountSummary> accounts = controller.getMyAccounts();
        ComboBox<AccountSummary> from = AccountSelector.create("transferFromCombo", accounts, SOURCE);
        ComboBox<String> to = new ComboBox<>(FXCollections.observableArrayList(
                accounts.stream().filter(a -> !"CLOSED".equals(a.status())).map(AccountSummary::accountNumber).toList()));
        to.setId("transferToCombo");
        to.setEditable(true);
        to.setPromptText("Select or type an account number");
        to.setPrefWidth(360);
        TextField amount = new TextField();
        amount.setId("transferAmountField");
        PasswordField pin = new PasswordField();
        pin.setId("transferPinField");

        GridPane form = ViewUtils.form();
        ViewUtils.addRow(form, 0, "From account", from);
        ViewUtils.addRow(form, 1, "To account", to);
        ViewUtils.addRow(form, 2, "Amount (BWP)", amount);
        ViewUtils.addRow(form, 3, "Password / PIN", pin);

        Label message = ViewUtils.messageLabel();
        Button transfer = ViewUtils.primary("Transfer");
        transfer.setId("transferButton");
        transfer.setOnAction(e -> {
            String destination = to.getEditor().getText();
            ActionResult<TransactionRow> result = controller.processTransfer(AccountSelector.number(from),
                    destination, amount.getText(), pin.getText());
            ViewUtils.showResult(message, result);
            pin.clear();
            if (result.success()) {
                amount.clear();
                AccountSelector.refresh(from, controller.getMyAccounts(), SOURCE);
            }
        });

        return ViewUtils.card(ViewUtils.title("Transfer Funds"),
                ViewUtils.hint("Transfers out of Savings accounts are not allowed."), form, transfer, message);
    }
}
