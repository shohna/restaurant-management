package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class SignupApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CaféDine - Sign Up");

        // Header
        Label titleLabel = new Label("Create Your Account");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setTextFill(Color.web("#2C3E50"));

        // Input fields
        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField contactField = new TextField();
        contactField.setPromptText("Contact Number");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create a Password");

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Customer", "Staff");
        roleComboBox.setPromptText("Select Role");

        Button signupButton = new Button("Sign Up");
        signupButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white;");

        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#E74C3C"));

        // Layout
        VBox layout = new VBox(10, titleLabel, nameField, contactField, usernameField, passwordField, roleComboBox, signupButton, statusLabel);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #ECF0F1; -fx-border-color: #BDC3C7; -fx-border-width: 2;");

        signupButton.setOnAction(event -> {
            String name = nameField.getText();
            String contact = contactField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = roleComboBox.getValue();

            if (name.isEmpty() || contact.isEmpty() || username.isEmpty() || password.isEmpty() || role == null) {
                statusLabel.setText("Please fill in all fields.");
                return;
            }

            if (registerUser(name, contact, username, password, role)) {
                statusLabel.setTextFill(Color.web("#27AE60"));
                statusLabel.setText("Sign-up successful! You can now log in.");
                primaryStage.close();
            } else {
                statusLabel.setText("Sign-up failed. Username might already exist.");
            }
        });

        Scene scene = new Scene(layout, 350, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean registerUser(String name, String contact, String username, String password, String role) {
        String query = "INSERT INTO users (name, contact, username, password, role) VALUES (?, ?, ?, ?, ?)";
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(query)) {
            statement.setString(1, name);
            statement.setString(2, contact);
            statement.setString(3, username);
            statement.setString(4, password);
            statement.setString(5, role);
            statement.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
