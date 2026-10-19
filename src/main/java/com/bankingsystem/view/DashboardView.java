package com.bankingsystem.view;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.AuthenticationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main customer screen: header, side navigation and the currently selected customer view.
 */
public class DashboardView {
    private final ViewNavigator navigator;
    private final AuthenticationController authController;
    private final AccountController accountController;
    private final BorderPane root = new BorderPane();
    private final List<Button> navButtons = new ArrayList<>();

    public DashboardView(ViewNavigator navigator, AuthenticationController authController,
                         AccountController accountController) {
        this.navigator = navigator;
        this.authController = authController;
        this.accountController = accountController;
    }

    public Parent getView() {
        Label title = new Label(navigator.bankName());
        title.getStyleClass().add("header-title");
        Label welcome = new Label("Welcome, " + authController.getSession().getDisplayName()
                + "  (" + authController.getSession().getUserId() + ")");
        welcome.setId("welcomeLabel");
        welcome.getStyleClass().add("header-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = ViewUtils.secondary("Logout");
        logout.setId("logoutButton");
        logout.setOnAction(e -> {
            authController.logout();
            navigator.showLogin();
        });
        HBox header = new HBox(16, new VBox(2, title, welcome), spacer, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        VBox nav = new VBox();
        nav.getStyleClass().add("side-nav");
        nav.getChildren().addAll(
                navButton("navAccounts", "My Accounts", () -> new AccountView(accountController, this).getView()),
                navButton("navOpenAccount", "Open Account", () -> new OpenAccountView(accountController, this).getView()),
                navButton("navDeposit", "Deposit Funds", () -> new DepositView(accountController).getView()),
                navButton("navWithdraw", "Withdraw Funds", () -> new WithdrawView(accountController).getView()),
                navButton("navTransfer", "Transfer Funds", () -> new TransferView(accountController).getView()),
                navButton("navHistory", "Transaction History", () -> new TransactionHistoryView(accountController).getView()),
                navButton("navClose", "Close Account", () -> new CloseAccountView(accountController).getView()));

        root.setTop(header);
        root.setLeft(nav);
        navButtons.get(0).fire();
        return root;
    }

    /** Lets child views jump to another section (e.g. "Open your first account"). */
    void showSection(int index) {
        navButtons.get(index).fire();
    }

    private Button navButton(String id, String text, Supplier<Node> content) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("nav-button");
        button.setOnAction(e -> {
            navButtons.forEach(b -> b.getStyleClass().remove("nav-button-selected"));
            button.getStyleClass().add("nav-button-selected");
            VBox wrapper = new VBox(content.get());
            wrapper.setPadding(new Insets(24));
            ScrollPane scroll = new ScrollPane(wrapper);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            root.setCenter(scroll);
        });
        navButtons.add(button);
        return button;
    }
}
