package application;

import java.io.IOException;
import java.sql.SQLException;

import application.network.server.RMSServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

public class MainApplication extends Application {
	private static RMSServer server;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Ensure database tables are created
            DatabaseConnection.createTables();

            // Launch authentication window
            AuthenticationApp authenticationApp = new AuthenticationApp();
            String role = authenticationApp.authenticateUser(primaryStage); // Launch login window

            if (role == null) {
                // Exit if authentication fails
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Authentication Failed");
                alert.setHeaderText(null);
                alert.setContentText("Invalid credentials or cancelled login. Application will exit.");
                alert.showAndWait();
                return; // Exit the application
            }
            
            if (role.equals("Staff")) {
                new Thread(() -> {
                    try {
                        System.out.println("Starting RMS Server...");
                        server = new RMSServer();
                        server.start();
                    } catch (IOException | SQLException e) {
                        System.err.println("Failed to start server: " + e.getMessage());
                        e.printStackTrace();
                    }
                }).start();

                // Give the server a moment to start
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Load appropriate interface based on the authenticated role
            FXMLLoader loader;
            if (role.equals("Staff")) {
                loader = new FXMLLoader(getClass().getResource("/resources/staff_interface.fxml"));
                primaryStage.setTitle("CaféDine Restaurant - Staff Dashboard");
            } else {
                loader = new FXMLLoader(getClass().getResource("/resources/customer_interface.fxml"));
                primaryStage.setTitle("CaféDine Restaurant");
            }

            VBox root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/resources/style.css").toExternalForm());

            // Set up and display the main application window
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.setResizable(true);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
        	
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}