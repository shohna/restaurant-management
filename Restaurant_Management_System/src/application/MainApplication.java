package application;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class MainApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
    	
    	try {
        // Load the customer interface
    	FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/customer_interface.fxml"));
        Region root = loader.load();

        // Apply CSS styling
        root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Restaurant Management System");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    	} catch (Exception e) {
            e.printStackTrace(); // Print the error
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
