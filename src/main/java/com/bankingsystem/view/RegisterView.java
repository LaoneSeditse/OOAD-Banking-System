package com.bankingsystem.view;

import com.bankingsystem.controller.RegistrationController;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.RegistrationRequest;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Customer registration form (use case: Register Customer / Provide Personal or Business Information).
 */
public class RegisterView {
    private final ViewNavigator navigator;
    private final RegistrationController controller;

    public RegisterView(ViewNavigator navigator, RegistrationController controller) {
        this.navigator = navigator;
        this.controller = controller;
    }

    public Parent getView() {
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton individual = new RadioButton("Individual");
        individual.setToggleGroup(typeGroup);
        individual.setSelected(true);
        RadioButton company = new RadioButton("Company / Business");
        company.setToggleGroup(typeGroup);
        company.setId("companyRadio");

        TextField firstName = field("firstNameField", "e.g. Kagiso");
        TextField lastName = field("lastNameField", "e.g. Molefe");
        TextField idNumber = field("idNumberField", "Omang / passport number");
        TextField companyName = field("companyNameField", "Registered company name");
        TextField regNumber = field("registrationNumberField", "Company registration number");
        DatePicker dob = new DatePicker(LocalDate.now().minusYears(25));
        dob.setId("dobPicker");
        TextField postal = field("postalField", "P.O. Box ...");
        TextField physical = field("physicalField", "Plot / street / town");
        TextField income = field("incomeField", "Monthly income in BWP");
        TextField username = field("regUsernameField", "Choose a username");
        PasswordField password = new PasswordField();
        password.setId("regPasswordField");
        PasswordField confirm = new PasswordField();
        confirm.setId("confirmPasswordField");

        GridPane form = ViewUtils.form();
        Label dobLabel = new Label("Date of birth");
        Runnable layout = () -> {
            form.getChildren().clear();
            int row = 0;
            form.add(new HBox(16, individual, company), 1, row++);
            if (company.isSelected()) {
                ViewUtils.addRow(form, row++, "Company name", companyName);
                ViewUtils.addRow(form, row++, "Registration no.", regNumber);
                dobLabel.setText("Representative DOB");
            } else {
                ViewUtils.addRow(form, row++, "First name", firstName);
                ViewUtils.addRow(form, row++, "Last name", lastName);
                ViewUtils.addRow(form, row++, "ID number", idNumber);
                dobLabel.setText("Date of birth");
            }
            form.add(dobLabel, 0, row);
            form.add(dob, 1, row++);
            ViewUtils.addRow(form, row++, "Postal address", postal);
            ViewUtils.addRow(form, row++, "Physical address", physical);
            ViewUtils.addRow(form, row++, "Income (BWP)", income);
            ViewUtils.addRow(form, row++, "Username", username);
            ViewUtils.addRow(form, row++, "Password", password);
            ViewUtils.addRow(form, row, "Confirm password", confirm);
        };
        typeGroup.selectedToggleProperty().addListener((obs, o, n) -> layout.run());
        layout.run();

        Label message = ViewUtils.messageLabel();
        Button submit = ViewUtils.primary("Register");
        submit.setId("submitRegistration");
        Button back = ViewUtils.secondary("Back to login");
        back.setOnAction(e -> navigator.showLogin());
        submit.setOnAction(e -> {
            RegistrationRequest request = new RegistrationRequest(
                    company.isSelected() ? "COMPANY" : "INDIVIDUAL",
                    firstName.getText(), lastName.getText(), idNumber.getText(),
                    companyName.getText(), regNumber.getText(), dob.getValue(),
                    postal.getText(), physical.getText(), income.getText(),
                    username.getText(), password.getText(), confirm.getText());
            ActionResult<String> result = controller.register(request);
            ViewUtils.showResult(message, result);
            if (result.success()) {
                submit.setDisable(true);
            }
        });

        HBox buttons = new HBox(12, submit, back);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox card = ViewUtils.card(ViewUtils.title("Register as a customer"),
                ViewUtils.hint("All customers must be 18 years or older."), form, buttons, message);
        card.setMaxWidth(620);

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private static TextField field(String id, String prompt) {
        TextField f = new TextField();
        f.setId(id);
        f.setPromptText(prompt);
        return f;
    }
}
