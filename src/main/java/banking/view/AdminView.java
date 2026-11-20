package banking.view;

import banking.controller.AdminController;
import banking.controller.LoginController;
import banking.exception.BankException;
import banking.model.Account;
import banking.model.Customer;
import banking.model.User;
import banking.service.BankAdministrator;
import banking.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

public class AdminView {
    private final BorderPane root = new BorderPane();
    private final AdminController controller;
    private final LoginController loginController;
    private final AppNavigator navigator;
    private final User user;
    private final BankAdministrator administrator;

    private final TableView<Customer> customerTable = new TableView<>();
    private final TableView<Account> accountTable = new TableView<>();

    public AdminView(AdminController controller, LoginController loginController, AppNavigator navigator, User user) {
        this.controller = controller;
        this.loginController = loginController;
        this.navigator = navigator;
        this.user = user;
        this.administrator = controller.createAdministrator(user.getUserId(), user.getUsername());

        root.setTop(buildHeader());
        root.setCenter(buildBody());
        root.getStyleClass().add("background");
        refreshCustomers();
    }

    private Parent buildHeader() {
        Label title = new Label("Administrator: " + administrator.getName());
        title.getStyleClass().add("title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button interest = new Button("Pay monthly interest");
        interest.setOnAction(e -> attempt(() -> {
            if (Dialogs.confirm("Pay this month's interest to all eligible accounts?")) {
                Dialogs.info(controller.payMonthlyInterest());
                refreshCustomers();
            }
        }));
        Button report = new Button("Summary report");
        report.getStyleClass().add("secondary");
        report.setOnAction(e -> attempt(() -> Dialogs.report("Summary report", administrator.generateReports())));
        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary");
        logout.setOnAction(e -> {
            loginController.logout(user);
            navigator.showLogin();
        });

        HBox header = new HBox(10, title, spacer, interest, report, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 5, 20));
        return header;
    }

    private Parent buildBody() {
        customerTable.getColumns().addAll(List.of(
                column("Customer ID", Customer::getCustomerId, 100),
                column("Name", Customer::getDisplayName, 190),
                column("Type", Customer::getCustomerType, 80),
                column("Username", c -> c.getUser().getUsername(), 100),
                column("Login", c -> c.getUser().isActive() ? "Active" : "LOCKED", 70)));
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, o, c) -> showAccounts(c));

        Label customersTitle = new Label("Customers");
        customersTitle.getStyleClass().add("section");
        Button unlock = new Button("Unlock login");
        unlock.setOnAction(e -> {
            Customer c = customerTable.getSelectionModel().getSelectedItem();
            if (c == null) {
                Dialogs.info("Select a customer first.");
                return;
            }
            attempt(() -> {
                controller.unlockCustomer(c.getCustomerId());
                refreshCustomers();
            });
        });
        VBox left = new VBox(10, customersTitle, customerTable, unlock);
        VBox.setVgrow(customerTable, Priority.ALWAYS);
        left.setPadding(new Insets(15));
        left.getStyleClass().add("card");
        left.setPrefWidth(520);

        accountTable.getColumns().addAll(List.of(
                column("Account number", Account::getAccountNumber, 120),
                column("Type", a -> a.getAccountType().getLabel(), 90),
                column("Balance", a -> Money.format(a.getBalance()), 120),
                column("Status", a -> a.getStatus().name(), 90)));
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTable.setPlaceholder(new Label("Select a customer to see their accounts."));

        Label accountsTitle = new Label("Accounts of selected customer");
        accountsTitle.getStyleClass().add("section");
        Button suspend = new Button("Suspend account");
        suspend.getStyleClass().add("danger");
        suspend.setOnAction(e -> changeStatus(true));
        Button reactivate = new Button("Reactivate account");
        reactivate.setOnAction(e -> changeStatus(false));
        VBox right = new VBox(10, accountsTitle, accountTable, new HBox(10, suspend, reactivate));
        VBox.setVgrow(accountTable, Priority.ALWAYS);
        right.setPadding(new Insets(15));
        right.getStyleClass().add("card");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(15, left, right);
        body.setPadding(new Insets(10, 20, 20, 20));
        return body;
    }

    private void changeStatus(boolean suspend) {
        Account a = accountTable.getSelectionModel().getSelectedItem();
        if (a == null) {
            Dialogs.info("Select an account first.");
            return;
        }
        attempt(() -> {
            if (suspend) {
                controller.suspendAccount(a.getAccountNumber());
            } else {
                controller.reactivateAccount(a.getAccountNumber());
            }
            showAccounts(customerTable.getSelectionModel().getSelectedItem());
        });
    }

    private void refreshCustomers() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? null : selected.getCustomerId();
        List<Customer> customers = administrator.manageUsers();
        customerTable.setItems(FXCollections.observableArrayList(customers));
        for (Customer c : customers) {
            if (c.getCustomerId().equals(selectedId)) {
                customerTable.getSelectionModel().select(c);
                return;
            }
        }
        showAccounts(null);
    }

    private void showAccounts(Customer customer) {
        if (customer == null) {
            accountTable.setItems(FXCollections.observableArrayList());
        } else {
            accountTable.setItems(FXCollections.observableArrayList(controller.getAccounts(customer.getCustomerId())));
        }
    }

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (BankException ex) {
            Dialogs.error(ex.getMessage());
        }
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, String> getter, double width) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    public Parent getRoot() {
        return root;
    }
}
