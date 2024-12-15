package application;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.PreparedStatement;

public class SignupApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CaféDine - Sign Up");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField contactField = new TextField();
        contactField.setPromptText("Contact Number");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Customer", "Staff");
        roleComboBox.setPromptText("Role");

        Button signupButton = new Button("Sign Up");
        Label statusLabel = new Label();

        VBox layout = new VBox(10, nameField, contactField, usernameField, passwordField, roleComboBox, signupButton,
                statusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setMinWidth(300);

        // Handle Sign-Up Button
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
                statusLabel.setText("Sign-up successful! You can now log in.");
                nameField.clear();
                contactField.clear();
                usernameField.clear();
                passwordField.clear();
                roleComboBox.getSelectionModel().clearSelection();
                primaryStage.close();
            } else {
                statusLabel.setText("Sign-up failed. Username might already exist.");
            }
        });

        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Register a new user in the database.
     */
    private boolean registerUser(String name, String contact, String username, String password, String role) {
        String query = "INSERT INTO users (name, contact, username, password, role) VALUES (?, ?, ?, ?, ?)";
        try (var connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {

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
