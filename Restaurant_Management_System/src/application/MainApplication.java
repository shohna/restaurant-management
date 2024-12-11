package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import application.DatabaseConnection;

public class MainApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
        	
        	DatabaseConnection.createTables();
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/customer_interface.fxml"));           
        	VBox root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/resources/style.css").toExternalForm());            
            // Set fixed window size
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.setResizable(false);  // Make window non-resizable
            
            primaryStage.setTitle("CaféDine Restaurant");
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