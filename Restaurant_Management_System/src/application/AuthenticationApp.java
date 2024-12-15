package application;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AuthenticationApp extends Application {

    private String authenticatedRole;

    public String authenticateUser(Stage ownerStage) {
        Stage loginStage = new Stage();
        loginStage.initOwner(ownerStage);
        loginStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
        loginStage.setTitle("Login");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Button signupButton = new Button("Sign Up");

        Label statusLabel = new Label();

        VBox layout = new VBox(10, usernameField, passwordField, loginButton, signupButton, statusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setMinWidth(300);

        Scene scene = new Scene(layout);
        loginStage.setScene(scene);

        // Handle Login
        loginButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = authenticate(username, password);

            if (role != null) {
                authenticatedRole = role;
                loginStage.close(); // Close the login window
            } else {
                statusLabel.setText("Invalid credentials. Please try again.");
            }
        });

        // Handle Sign-Up
        signupButton.setOnAction(event -> {
            SignupApp signupApp = new SignupApp();
            signupApp.start(new Stage()); // Open a new sign-up window
        });

        loginStage.showAndWait(); // Block until the login window is closed
        return authenticatedRole;
    }

    private String authenticate(String username, String password) {
        try {
            String query = "SELECT role FROM users WHERE username = ? AND password = ?";
            var connection = DatabaseConnection.getConnection();
            var statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("role"); // Return the user's role
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Authentication failed
    }

    @Override
    public void start(Stage primaryStage) {
        // This method is not used in this flow
    }
}
