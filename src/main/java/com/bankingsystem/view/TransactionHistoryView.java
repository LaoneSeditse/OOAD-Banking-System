package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.TransactionRow;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

/**
 * Transaction history and statement for one account (use case: View Transaction History / View Statements).
 */
public class TransactionHistoryView {
    private final AccountController controller;

    public TransactionHistoryView(AccountController controller) {
        this.controller = controller;
    }

    public Node getView() {
        ComboBox<AccountSummary> account = AccountSelector.create("historyAccountCombo", controller.getMyAccounts(), a -> true);

        TableView<TransactionRow> table = ViewUtils.table();
        table.setId("historyTable");
        table.getColumns().add(ViewUtils.column("Date", TransactionRow::date, 130));
        table.getColumns().add(ViewUtils.column("Type", TransactionRow::type, 90));
        table.getColumns().add(ViewUtils.column("Amount", t -> ViewUtils.money(t.amount()), 110));
        table.getColumns().add(ViewUtils.column("Description", TransactionRow::description, 240));
        table.getColumns().add(ViewUtils.column("Balance after", t -> ViewUtils.money(t.balanceAfter()), 120));
        table.getColumns().add(ViewUtils.column("Status", TransactionRow::status, 90));
        table.setPlaceholder(new Label("Select an account to see its transactions."));
        table.setPrefHeight(340);

        TextArea statement = new TextArea();
        statement.getStyleClass().add("report-area");
        statement.setEditable(false);
        statement.setPrefRowCount(10);
        statement.setVisible(false);
        statement.setManaged(false);

        account.valueProperty().addListener((obs, o, n) -> {
            table.setItems(FXCollections.observableArrayList(
                    n == null ? java.util.List.of() : controller.getTransactionHistory(n.accountNumber())));
            statement.setVisible(false);
            statement.setManaged(false);
        });
        Button showStatement = ViewUtils.secondary("View statement");
        showStatement.setOnAction(e -> {
            if (account.getValue() != null) {
                statement.setText(controller.getStatement(account.getValue().accountNumber()));
                statement.setVisible(true);
                statement.setManaged(true);
            }
        });

        return ViewUtils.card(ViewUtils.title("Transaction History"), new HBox(12, account, showStatement), table, statement);
    }
}
