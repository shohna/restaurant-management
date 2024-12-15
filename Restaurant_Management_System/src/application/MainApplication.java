package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class MainApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseConnection.createTables();
            
            // Create interface selection dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("CaféDine Interface Selection");
            alert.setHeaderText("Select Interface");
            alert.setContentText("Choose which interface to launch:");
            
            ButtonType customerButton = new ButtonType("Customer Interface");
            ButtonType staffButton = new ButtonType("Staff Dashboard");
            
            alert.getButtonTypes().setAll(customerButton, staffButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            
            // Load appropriate interface based on selection
            FXMLLoader loader;
            if (result.isPresent() && result.get() == staffButton) {
                // Load staff interface
                loader = new FXMLLoader(getClass().getResource("/resources/staff_interface.fxml"));
                primaryStage.setWidth(800);
                primaryStage.setHeight(600);
                primaryStage.setResizable(true);
                primaryStage.setTitle("CaféDine Restaurant - Staff Dashboard");
            } else {
                // Load customer interface
                loader = new FXMLLoader(getClass().getResource("/resources/customer_interface.fxml"));
                primaryStage.setWidth(800);
                primaryStage.setHeight(600);
                primaryStage.setResizable(true);
                primaryStage.setTitle("CaféDine Restaurant");
            }
            
            VBox root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/resources/style.css").toExternalForm());
            
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