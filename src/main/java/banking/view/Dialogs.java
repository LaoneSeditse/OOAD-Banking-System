package banking.view;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;

import java.util.Optional;

final class Dialogs {
    private Dialogs() { }

    static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Something went wrong");
        alert.showAndWait();
    }

    static void report(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setPrefRowCount(6);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    static Optional<String> askText(String title, String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }

    static Optional<String[]> askTwoTexts(String title, String firstLabel, String secondLabel) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle(title);
        TextField first = new TextField();
        TextField second = new TextField();
        GridPane grid = grid();
        grid.addRow(0, new Label(firstLabel), first);
        grid.addRow(1, new Label(secondLabel), second);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == ButtonType.OK ? new String[]{first.getText(), second.getText()} : null);
        return dialog.showAndWait();
    }

    static Optional<String[]> askPasswordChange() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Change password");
        PasswordField current = new PasswordField();
        PasswordField newPass = new PasswordField();
        PasswordField confirm = new PasswordField();
        GridPane grid = grid();
        grid.addRow(0, new Label("Current password"), current);
        grid.addRow(1, new Label("New password"), newPass);
        grid.addRow(2, new Label("Confirm new password"), confirm);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == ButtonType.OK
                ? new String[]{current.getText(), newPass.getText(), confirm.getText()} : null);
        return dialog.showAndWait();
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        return grid;
    }
}
