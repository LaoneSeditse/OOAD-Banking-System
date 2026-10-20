package com.bankingsystem.view;

import com.bankingsystem.controller.AdminController;
import com.bankingsystem.controller.AuthenticationController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.CustomerSummary;
import com.bankingsystem.controller.dto.TransactionRow;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Bank administrator screen (use cases: Manage Customer Accounts, Monitor Transactions,
 * Generate Reports, Pay Interest, Unlock Customer Account).
 */
public class AdminView {
    private final ViewNavigator navigator;
    private final AuthenticationController authController;
    private final AdminController controller;
    private final Label message = ViewUtils.messageLabel();

    private final TableView<CustomerSummary> customers = ViewUtils.table();
    private final TableView<AccountSummary> accounts = ViewUtils.table();
    private final TableView<TransactionRow> transactions = ViewUtils.table();

    public AdminView(ViewNavigator navigator, AuthenticationController authController, AdminController controller) {
        this.navigator = navigator;
        this.authController = authController;
        this.controller = controller;
    }

    public Parent getView() {
        Label title = new Label(navigator.bankName() + " - Administration");
        title.getStyleClass().add("header-title");
        Label who = new Label("Logged in as " + authController.getSession().getDisplayName());
        who.getStyleClass().add("header-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = ViewUtils.secondary("Logout");
        logout.setId("adminLogoutButton");
        logout.setOnAction(e -> {
            authController.logout();
            navigator.showLogin();
        });
        HBox header = new HBox(16, new VBox(2, title, who), spacer, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        TabPane tabs = new TabPane(
                new Tab("Customers", customersTab()),
                new Tab("Accounts", accountsTab()),
                new Tab("Transactions", transactionsTab()),
                new Tab("Reports", reportsTab()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setId("adminTabs");

        VBox center = new VBox(10, tabs, message);
        center.setStyle("-fx-padding: 16;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        BorderPane root = new BorderPane(center);
        root.setTop(header);
        refresh();
        return root;
    }

    private VBox customersTab() {
        customers.setId("customersTable");
        customers.getColumns().add(ViewUtils.column("ID", CustomerSummary::customerId, 80));
        customers.getColumns().add(ViewUtils.column("Name", CustomerSummary::name, 200));
        customers.getColumns().add(ViewUtils.column("Type", CustomerSummary::customerType, 90));
        customers.getColumns().add(ViewUtils.column("Username", CustomerSummary::username, 110));
        customers.getColumns().add(ViewUtils.column("Age", c -> String.valueOf(c.age()), 50));
        customers.getColumns().add(ViewUtils.column("Accounts", c -> String.valueOf(c.accountCount()), 70));
        customers.getColumns().add(ViewUtils.column("Total balance", c -> ViewUtils.money(c.totalBalance()), 130));
        customers.getColumns().add(ViewUtils.column("Status", CustomerSummary::status, 80));

        Button unlock = ViewUtils.secondary("Unlock customer");
        unlock.setId("unlockButton");
        unlock.setOnAction(e -> onSelectedCustomer(c -> controller.unlockCustomer(c.customerId())));
        Button lock = ViewUtils.secondary("Lock customer");
        lock.setOnAction(e -> onSelectedCustomer(c -> controller.lockCustomer(c.customerId())));
        return tabContent(customers, new HBox(10, unlock, lock));
    }

    private VBox accountsTab() {
        accounts.setId("allAccountsTable");
        accounts.getColumns().add(ViewUtils.column("Account No.", AccountSummary::accountNumber, 100));
        accounts.getColumns().add(ViewUtils.column("Holder", AccountSummary::holderName, 180));
        accounts.getColumns().add(ViewUtils.column("Type", AccountSummary::accountTypeName, 140));
        accounts.getColumns().add(ViewUtils.column("Branch", AccountSummary::branch, 110));
        accounts.getColumns().add(ViewUtils.column("Balance", a -> ViewUtils.money(a.balance()), 120));
        accounts.getColumns().add(ViewUtils.column("Status", AccountSummary::status, 90));

        Button suspend = ViewUtils.secondary("Suspend account");
        suspend.setOnAction(e -> onSelectedAccount(a -> controller.suspendAccount(a.accountNumber())));
        Button reactivate = ViewUtils.secondary("Reactivate account");
        reactivate.setOnAction(e -> onSelectedAccount(a -> controller.reactivateAccount(a.accountNumber())));
        Button payInterest = ViewUtils.primary("Pay monthly interest");
        payInterest.setId("payInterestButton");
        payInterest.setDefaultButton(false);
        payInterest.setOnAction(e -> {
            ActionResult<List<TransactionRow>> result = controller.payMonthlyInterest();
            ViewUtils.showResult(message, result);
            refresh();
        });
        return tabContent(accounts, new HBox(10, suspend, reactivate, payInterest));
    }

    private VBox transactionsTab() {
        transactions.setId("allTransactionsTable");
        transactions.getColumns().add(ViewUtils.column("Date", TransactionRow::date, 130));
        transactions.getColumns().add(ViewUtils.column("Account", TransactionRow::accountNumber, 90));
        transactions.getColumns().add(ViewUtils.column("Type", TransactionRow::type, 90));
        transactions.getColumns().add(ViewUtils.column("Amount", t -> ViewUtils.money(t.amount()), 110));
        transactions.getColumns().add(ViewUtils.column("Description", TransactionRow::description, 220));
        transactions.getColumns().add(ViewUtils.column("Balance after", t -> ViewUtils.money(t.balanceAfter()), 120));
        transactions.getColumns().add(ViewUtils.column("Status", TransactionRow::status, 90));
        Button reload = ViewUtils.secondary("Refresh");
        reload.setOnAction(e -> refresh());
        return tabContent(transactions, new HBox(10, reload));
    }

    private VBox reportsTab() {
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "CUSTOMERS", "ACCOUNTS", "TRANSACTIONS", "INTEREST"));
        type.setId("reportTypeCombo");
        type.getSelectionModel().selectFirst();
        TextArea output = new TextArea();
        output.setId("reportArea");
        output.getStyleClass().add("report-area");
        output.setEditable(false);
        VBox.setVgrow(output, Priority.ALWAYS);
        Button generate = ViewUtils.primary("Generate report");
        generate.setId("generateReportButton");
        generate.setOnAction(e -> output.setText(controller.generateReport(type.getValue())));
        return tabContent(output, new HBox(10, type, generate));
    }

    private VBox tabContent(javafx.scene.Node main, HBox actions) {
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, actions, main);
        box.setStyle("-fx-padding: 12 0 0 0;");
        VBox.setVgrow(main, Priority.ALWAYS);
        return box;
    }

    private void onSelectedCustomer(java.util.function.Function<CustomerSummary, ActionResult<?>> action) {
        CustomerSummary selected = customers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ViewUtils.show(message, false, "Select a customer first");
            return;
        }
        ViewUtils.showResult(message, action.apply(selected));
        refresh();
    }

    private void onSelectedAccount(java.util.function.Function<AccountSummary, ActionResult<?>> action) {
        AccountSummary selected = accounts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ViewUtils.show(message, false, "Select an account first");
            return;
        }
        ViewUtils.showResult(message, action.apply(selected));
        refresh();
    }

    private void refresh() {
        customers.setItems(FXCollections.observableArrayList(controller.getAllCustomers()));
        accounts.setItems(FXCollections.observableArrayList(controller.getAllAccounts()));
        transactions.setItems(FXCollections.observableArrayList(controller.getAllTransactions()));
    }
}
