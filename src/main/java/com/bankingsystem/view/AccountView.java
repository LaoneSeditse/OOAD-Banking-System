package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.dto.AccountSummary;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Shows the logged-in customer's accounts and total balance (use case: View Account Details / Check Balance).
 */
public class AccountView {
    private final AccountController controller;
    private final DashboardView dashboard;

    public AccountView(AccountController controller, DashboardView dashboard) {
        this.controller = controller;
        this.dashboard = dashboard;
    }

    public Node getView() {
        List<AccountSummary> accounts = controller.getMyAccounts();

        Label total = new Label(ViewUtils.money(controller.getMyTotalBalance()));
        total.setId("totalBalanceLabel");
        total.getStyleClass().add("summary-value");
        long open = accounts.stream().filter(a -> !"CLOSED".equals(a.status())).count();
        VBox totalBox = new VBox(4, ViewUtils.hint("Total balance"), total);
        Label count = new Label(String.valueOf(open));
        count.getStyleClass().add("summary-value");
        VBox countBox = new VBox(4, ViewUtils.hint("Open accounts"), count);

        TableView<AccountSummary> table = ViewUtils.table();
        table.setId("accountsTable");
        table.getColumns().add(ViewUtils.column("Account No.", AccountSummary::accountNumber, 110));
        table.getColumns().add(ViewUtils.column("Type", AccountSummary::accountTypeName, 150));
        table.getColumns().add(ViewUtils.column("Branch", AccountSummary::branch, 120));
        table.getColumns().add(ViewUtils.column("Balance", a -> ViewUtils.money(a.balance()), 130));
        table.getColumns().add(ViewUtils.column("Interest", a -> String.format("%.2f%% / month", a.monthlyInterestPercent()), 110));
        table.getColumns().add(ViewUtils.column("Withdrawals", a -> a.allowsWithdrawals() ? "Allowed" : "Not allowed", 100));
        table.getColumns().add(ViewUtils.column("Status", AccountSummary::status, 90));
        table.getColumns().add(ViewUtils.column("Opened", AccountSummary::openedDate, 100));
        table.setItems(FXCollections.observableArrayList(accounts));
        table.setPlaceholder(new Label("You do not have any accounts yet."));
        table.setPrefHeight(320);

        Button openNew = ViewUtils.secondary("Open a new account");
        openNew.setOnAction(e -> dashboard.showSection(1));

        return ViewUtils.card(ViewUtils.title("My Accounts"), new HBox(60, totalBox, countBox), table, openNew);
    }
}
