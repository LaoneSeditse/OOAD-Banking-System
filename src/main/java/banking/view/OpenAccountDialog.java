package banking.view;

import banking.controller.AccountController;
import banking.exception.BankException;
import banking.model.AccountType;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

class OpenAccountDialog {
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private boolean accountOpened = false;

    OpenAccountDialog(AccountController controller, String customerId) {
        dialog.setTitle("Open a new account");

        ChoiceBox<AccountType> type = new ChoiceBox<>();
        type.getItems().addAll(AccountType.values());
        type.setValue(AccountType.SAVINGS);

        TextField initialDeposit = new TextField();
        initialDeposit.setPromptText("0.00");
        TextField branch = new TextField("Gaborone Main");
        TextField employer = new TextField();
        TextField employerAddress = new TextField();
        TextField position = new TextField();

        Label rule = new Label();
        rule.setWrapText(true);
        rule.setMaxWidth(380);
        rule.getStyleClass().add("subtitle");

        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(380);
        error.getStyleClass().add("error-text");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Node[] employmentNodes = {
                new Label("Employer (company name)"), employer,
                new Label("Employer address"), employerAddress,
                new Label("Position (optional)"), position};
        grid.addRow(0, new Label("Account type"), type);
        grid.addRow(1, new Label("Initial deposit (BWP)"), initialDeposit);
        grid.addRow(2, new Label("Branch"), branch);
        grid.addRow(3, employmentNodes[0], employmentNodes[1]);
        grid.addRow(4, employmentNodes[2], employmentNodes[3]);
        grid.addRow(5, employmentNodes[4], employmentNodes[5]);

        Runnable refreshForm = () -> {
            boolean cheque = type.getValue() == AccountType.CHEQUE;
            for (Node n : employmentNodes) {
                n.setVisible(cheque);
                n.setManaged(cheque);
            }
            rule.setText(switch (type.getValue()) {
                case SAVINGS -> "Pays 0.05% interest monthly. Withdrawals are not allowed.";
                case INVESTMENT -> "Pays 5% interest monthly. Minimum opening deposit: BWP 500.00.";
                case CHEQUE -> "For salary payments. Only for people who are working, so employer details are required.";
            });
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        };
        type.valueProperty().addListener((obs, o, n) -> refreshForm.run());

        VBox content = new VBox(12, grid, rule, error);
        content.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Open account");
        // Keep the dialog open and show the message when a rule is broken.
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                controller.openAccount(customerId, type.getValue(), initialDeposit.getText(), branch.getText(),
                        employer.getText(), employerAddress.getText(), position.getText());
                accountOpened = true;
                error.setText("");
            } catch (BankException ex) {
                error.setText(ex.getMessage());
                ev.consume();
            }
        });

        dialog.setOnShown(e -> refreshForm.run());
    }

    /** Returns true when an account was created. */
    boolean showAndWait() {
        dialog.showAndWait();
        return accountOpened;
    }
}
