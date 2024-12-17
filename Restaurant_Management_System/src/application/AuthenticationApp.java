package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AuthenticationApp extends Application {

    private String authenticatedRole;

    public String authenticateUser(Stage ownerStage) {
        Stage loginStage = new Stage();
        loginStage.initOwner(ownerStage);
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setTitle("CaféDine - Login");

        // Header
        Label titleLabel = new Label("Welcome to CaféDine");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setTextFill(Color.web("#2C3E50"));

        // Input fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setMaxWidth(250);

        // Buttons
        Button loginButton = new Button("Login");
        Button signupButton = new Button("Sign Up");
        loginButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;");
        signupButton.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white;");

        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#E74C3C"));

        // Layout
        HBox buttonBox = new HBox(10, loginButton, signupButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(15, titleLabel, usernameField, passwordField, buttonBox, statusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #ECF0F1; -fx-border-color: #BDC3C7; -fx-border-width: 2;");

        Scene scene = new Scene(layout, 350, 300);
        loginStage.setScene(scene);

        // Handle Login
        loginButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = authenticate(username, password);

            if (role != null) {
                authenticatedRole = role;
                loginStage.close();
            } else {
                statusLabel.setText("Invalid credentials. Please try again.");
            }
        });

        // Handle Sign-Up
        signupButton.setOnAction(event -> {
            SignupApp signupApp = new SignupApp();
            signupApp.start(new Stage());
        });

        loginStage.showAndWait();
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
                return resultSet.getString("role");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void start(Stage primaryStage) {
        authenticateUser(primaryStage);
    }
}
