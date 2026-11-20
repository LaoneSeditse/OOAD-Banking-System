package banking.view;

import banking.controller.AccountController;
import banking.controller.LoginController;
import banking.exception.BankException;
import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.ChequeAccount;
import banking.model.Transaction;
import banking.model.User;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AccountView {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BorderPane root = new BorderPane();
    private final AccountController controller;
    private final LoginController loginController;
    private final AppNavigator navigator;
    private final User user;
    private final String customerId;

    private final TableView<Account> accountTable = new TableView<>();
    private final TableView<Transaction> transactionTable = new TableView<>();
    private final Label detailsTitle = new Label("Select an account");
    private final Label detailsBody = new Label();

    public AccountView(AccountController controller, LoginController loginController,
                       AppNavigator navigator, User user, String customerId) {
        this.controller = controller;
        this.loginController = loginController;
        this.navigator = navigator;
        this.user = user;
        this.customerId = customerId;

        root.setTop(buildHeader());
        root.setCenter(buildBody());
        root.getStyleClass().add("background");
        refresh();
    }

    // ------------------------------------------------------------------ layout

    private Parent buildHeader() {
        Label welcome = new Label("Welcome, " + controller.getCustomer(customerId).getDisplayName());
        welcome.getStyleClass().add("title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button changePassword = new Button("Change password");
        changePassword.getStyleClass().add("secondary");
        changePassword.setOnAction(e -> changePassword());
        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary");
        logout.setOnAction(e -> {
            loginController.logout(user);
            navigator.showLogin();
        });

        HBox header = new HBox(10, welcome, spacer, changePassword, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 5, 20));
        return header;
    }

    private Parent buildBody() {
        // --- accounts table ---
        TableColumn<Account, String> numberCol = column("Account number", a -> a.getAccountNumber(), 130);
        TableColumn<Account, String> typeCol = column("Type", a -> a.getAccountType().getLabel(), 90);
        TableColumn<Account, String> balanceCol = column("Balance", a -> Money.format(a.getBalance()), 130);
        TableColumn<Account, String> statusCol = column("Status", a -> a.getStatus().name(), 90);
        accountTable.getColumns().addAll(List.of(numberCol, typeCol, balanceCol, statusCol));
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTable.setPlaceholder(new Label("You have no accounts yet. Click \"Open new account\"."));
        accountTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showSelected(selected));

        Label accountsTitle = new Label("My accounts");
        accountsTitle.getStyleClass().add("section");
        Button openAccount = new Button("Open new account");
        openAccount.setOnAction(e -> {
            if (new OpenAccountDialog(controller, customerId).showAndWait()) {
                refresh();
            }
        });
        VBox left = new VBox(10, accountsTitle, accountTable, openAccount);
        VBox.setVgrow(accountTable, Priority.ALWAYS);
        left.setPadding(new Insets(15));
        left.getStyleClass().add("card");
        left.setPrefWidth(480);

        // --- details + actions ---
        detailsTitle.getStyleClass().add("section");
        Button deposit = new Button("Deposit");
        deposit.setOnAction(e -> onDeposit());
        Button withdraw = new Button("Withdraw");
        withdraw.setOnAction(e -> onWithdraw());
        Button transfer = new Button("Transfer");
        transfer.setOnAction(e -> onTransfer());
        Button interest = new Button("Interest this month");
        interest.getStyleClass().add("secondary");
        interest.setOnAction(e -> onInterest());
        Button close = new Button("Close account");
        close.getStyleClass().add("danger");
        close.setOnAction(e -> onClose());
        HBox actions = new HBox(8, deposit, withdraw, transfer, interest, close);

        // --- statement ---
        transactionTable.getColumns().addAll(List.of(
                column("Date", t -> t.getDate().format(DATE_FORMAT), 140),
                column("Type", t -> t.getType().name(), 100),
                column("Amount", t -> Money.format(t.getAmount()), 120),
                column("Description", t -> t.getDescription(), 220)));
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPlaceholder(new Label("No transactions to show."));
        Label statementTitle = new Label("Transaction history");
        statementTitle.getStyleClass().add("section");

        VBox right = new VBox(10, detailsTitle, detailsBody, actions, statementTitle, transactionTable);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);
        right.setPadding(new Insets(15));
        right.getStyleClass().add("card");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(15, left, right);
        body.setPadding(new Insets(10, 20, 20, 20));
        return body;
    }

    // ------------------------------------------------------------------ button handlers

    private void onDeposit() {
        Account account = selectedOrWarn();
        if (account == null) return;
        Dialogs.askText("Deposit", "Amount to deposit (BWP):").ifPresent(text -> attempt(() -> {
            controller.deposit(account.getAccountNumber(), text);
            refresh();
            Dialogs.info("Deposit successful.");
        }));
    }

    private void onWithdraw() {
        Account account = selectedOrWarn();
        if (account == null) return;
        Dialogs.askText("Withdraw", "Amount to withdraw (BWP):").ifPresent(text -> attempt(() -> {
            controller.withdraw(account.getAccountNumber(), text);
            refresh();
            Dialogs.info("Withdrawal successful.");
        }));
    }

    private void onTransfer() {
        Account account = selectedOrWarn();
        if (account == null) return;
        Dialogs.askTwoTexts("Transfer", "Destination account number", "Amount (BWP)").ifPresent(values -> attempt(() -> {
            controller.transfer(account.getAccountNumber(), values[0], values[1]);
            refresh();
            Dialogs.info("Transfer successful.");
        }));
    }

    private void onInterest() {
        Account account = selectedOrWarn();
        if (account == null) return;
        attempt(() -> Dialogs.info(String.format("Interest this month on this balance: %s (%s%% monthly).",
                Money.format(controller.previewInterest(account.getAccountNumber())),
                trimmed(account.getInterestRate() * 100))));
    }

    private void onClose() {
        Account account = selectedOrWarn();
        if (account == null) return;
        if (Dialogs.confirm("Close account " + account.getAccountNumber() + "?\nAny remaining balance ("
                + Money.format(account.getBalance()) + ") will be paid out. This cannot be undone.")) {
            attempt(() -> {
                controller.closeAccount(account.getAccountNumber());
                refresh();
            });
        }
    }

    private void changePassword() {
        Dialogs.askPasswordChange().ifPresent(p -> attempt(() -> {
            loginController.changePassword(user, p[0], p[1], p[2]);
            Dialogs.info("Password changed.");
        }));
    }

    // ------------------------------------------------------------------ helpers

    private void refresh() {
        Account previous = accountTable.getSelectionModel().getSelectedItem();
        String previousNumber = previous == null ? null : previous.getAccountNumber();
        List<Account> accounts = controller.getAccounts(customerId);
        accountTable.setItems(FXCollections.observableArrayList(accounts));
        for (Account a : accounts) {
            if (a.getAccountNumber().equals(previousNumber)) {
                accountTable.getSelectionModel().select(a);
                return;
            }
        }
        showSelected(null);
    }

    private void showSelected(Account account) {
        if (account == null) {
            detailsTitle.setText("Select an account");
            detailsBody.setText("");
            transactionTable.setItems(FXCollections.observableArrayList());
            return;
        }
        detailsTitle.setText(account.getAccountType().getLabel() + " account " + account.getAccountNumber());
        StringBuilder sb = new StringBuilder();
        sb.append("Balance: ").append(Money.format(account.getBalance())).append("    Status: ")
                .append(account.getStatus()).append("\n");
        sb.append("Branch: ").append(account.getBranch()).append("    Monthly interest rate: ")
                .append(trimmed(account.getInterestRate() * 100)).append("%");
        if (account instanceof ChequeAccount cheque) {
            sb.append("\nEmployer: ").append(cheque.getEmploymentDetails().getCompanyName());
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            sb.append("\nThis account is closed.");
        }
        detailsBody.setText(sb.toString());
        transactionTable.setItems(FXCollections.observableArrayList(
                controller.getTransactions(account.getAccountNumber())));
    }

    private Account selectedOrWarn() {
        Account account = accountTable.getSelectionModel().getSelectedItem();
        if (account == null) {
            Dialogs.info("Please select an account first.");
        }
        return account;
    }

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (BankException ex) {
            Dialogs.error(ex.getMessage());
        }
    }

    private static String trimmed(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static <T> TableColumn<T, String> column(String title, java.util.function.Function<T, String> getter, double width) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    public Parent getRoot() {
        return root;
    }
}
