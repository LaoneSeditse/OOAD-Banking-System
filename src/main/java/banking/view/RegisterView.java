package banking.view;

import banking.controller.RegistrationController;
import banking.exception.BankException;
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

public class RegisterView {
    private final StackPane root = new StackPane();

    public RegisterView(RegistrationController controller, AppNavigator navigator) {
        Label title = new Label("Register as a customer");
        title.getStyleClass().add("title");

        RadioButton individualRadio = new RadioButton("Individual");
        RadioButton companyRadio = new RadioButton("Company / Business");
        ToggleGroup group = new ToggleGroup();
        individualRadio.setToggleGroup(group);
        companyRadio.setToggleGroup(group);
        individualRadio.setSelected(true);

        // ----- shared fields -----
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        TextField postal = new TextField();
        TextField physical = new TextField();
        TextField income = new TextField();

        // ----- individual-only fields -----
        TextField firstName = new TextField();
        TextField lastName = new TextField();
        DatePicker dob = new DatePicker();
        dob.setPromptText("yyyy-MM-dd");

        // ----- company-only fields -----
        TextField companyName = new TextField();
        TextField registrationNumber = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int row = 0;
        grid.addRow(row++, new Label("Username"), username);
        grid.addRow(row++, new Label("Password"), password);
        grid.addRow(row++, new Label("Confirm password"), confirm);

        Label firstLabel = new Label("First name");
        Label lastLabel = new Label("Surname");
        Label dobLabel = new Label("Date of birth");
        Label companyLabel = new Label("Company name");
        Label regLabel = new Label("Registration number");
        grid.addRow(row++, firstLabel, firstName);
        grid.addRow(row++, lastLabel, lastName);
        grid.addRow(row++, dobLabel, dob);
        grid.addRow(row++, companyLabel, companyName);
        grid.addRow(row++, regLabel, registrationNumber);

        grid.addRow(row++, new Label("Postal address"), postal);
        grid.addRow(row++, new Label("Physical address"), physical);
        grid.addRow(row, new Label("Income (BWP)"), income);

        // show only the fields that apply to the chosen customer type
        Runnable switchType = () -> {
            boolean individual = individualRadio.isSelected();
            for (javafx.scene.Node n : new javafx.scene.Node[]{firstLabel, firstName, lastLabel, lastName, dobLabel, dob}) {
                n.setVisible(individual);
                n.setManaged(individual);
            }
            for (javafx.scene.Node n : new javafx.scene.Node[]{companyLabel, companyName, regLabel, registrationNumber}) {
                n.setVisible(!individual);
                n.setManaged(!individual);
            }
        };
        group.selectedToggleProperty().addListener((obs, o, n) -> switchType.run());
        switchType.run();

        Label message = new Label();
        message.setWrapText(true);
        message.getStyleClass().add("error-text");

        Button submit = new Button("Create account");
        submit.setDefaultButton(true);
        submit.setOnAction(e -> {
            try {
                if (individualRadio.isSelected()) {
                    controller.registerIndividual(username.getText(), password.getText(), confirm.getText(),
                            firstName.getText(), lastName.getText(), dob.getValue(),
                            postal.getText(), physical.getText(), income.getText());
                } else {
                    controller.registerCompany(username.getText(), password.getText(), confirm.getText(),
                            companyName.getText(), registrationNumber.getText(),
                            postal.getText(), physical.getText(), income.getText());
                }
                Dialogs.info("Registration successful. You can now log in.");
                navigator.showLogin();
            } catch (BankException ex) {
                message.setText(ex.getMessage());
            }
        });
        Button back = new Button("Back to login");
        back.getStyleClass().add("secondary");
        back.setOnAction(e -> navigator.showLogin());

        HBox buttons = new HBox(10, submit, back);
        buttons.setAlignment(Pos.CENTER_LEFT);
        HBox typeRow = new HBox(15, new Label("I am registering as:"), individualRadio, companyRadio);
        typeRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(14, title, typeRow, grid, message, buttons);
        form.setPadding(new Insets(24));
        form.getStyleClass().add("card");

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        root.getChildren().add(scroll);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("background");
    }

    public Parent getRoot() {
        return root;
    }
}
