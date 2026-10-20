package com.bankingsystem.view;

import com.bankingsystem.controller.dto.AccountSummary;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.List;
import java.util.function.Predicate;

/**
 * Combo box listing the customer's accounts.
 */
final class AccountSelector {
    private AccountSelector() {
    }

    static ComboBox<AccountSummary> create(String id, List<AccountSummary> accounts, Predicate<AccountSummary> filter) {
        ComboBox<AccountSummary> combo = new ComboBox<>(FXCollections.observableArrayList(
                accounts.stream().filter(filter).toList()));
        combo.setId(id);
        combo.setPrefWidth(360);
        combo.setPromptText("Select an account");
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AccountSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
        return combo;
    }

    static void refresh(ComboBox<AccountSummary> combo, List<AccountSummary> accounts,
                        Predicate<AccountSummary> filter) {
        String selected = combo.getValue() == null ? null : combo.getValue().accountNumber();
        combo.setItems(FXCollections.observableArrayList(accounts.stream().filter(filter).toList()));
        combo.getItems().stream().filter(a -> a.accountNumber().equals(selected)).findFirst()
                .ifPresent(combo::setValue);
    }

    static String number(ComboBox<AccountSummary> combo) {
        return combo.getValue() == null ? null : combo.getValue().accountNumber();
    }
}
