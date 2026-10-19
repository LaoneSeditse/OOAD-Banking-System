package com.bankingsystem.view;

import com.bankingsystem.controller.dto.ActionResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;

import java.util.function.Function;

/**
 * Small helpers for building consistent JavaFX screens. Presentation only - no business logic.
 */
final class ViewUtils {
    private ViewUtils() {
    }

    static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setWrapText(true);
        return label;
    }

    static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("primary-button");
        b.setDefaultButton(true);
        return b;
    }

    static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary-button");
        return b;
    }

    static Label messageLabel() {
        Label label = new Label();
        label.setId("messageLabel");
        label.setWrapText(true);
        label.setMaxWidth(560);
        return label;
    }

    static void showResult(Label label, ActionResult<?> result) {
        show(label, result.success(), result.message());
    }

    static void show(Label label, boolean success, String message) {
        label.getStyleClass().removeAll("message-success", "message-error");
        label.getStyleClass().add(success ? "message-success" : "message-error");
        label.setText(message);
    }

    static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);
        grid.getColumnConstraints().addAll(labels, new ColumnConstraints());
        return grid;
    }

    static void addRow(GridPane grid, int row, String label, Node field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    static VBox card(Node... children) {
        VBox box = new VBox(14, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(24));
        return box;
    }

    static <T> TableColumn<T, String> column(String title, Function<T, String> value, double width) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    static <T> TableView<T> table() {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    static String money(double amount) {
        return String.format("BWP %,.2f", amount);
    }
}
