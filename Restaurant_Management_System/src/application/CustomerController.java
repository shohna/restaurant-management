package application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class CustomerController {

    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private Button reserveButton;
    @FXML private ListView<String> menuList;

    public void initialize() {
        loadMenuItems();
    }

    @FXML
    private void handleReservation(MouseEvent event) {
        String customerName = nameField.getText();
        String customerContact = contactField.getText();
        if (!customerName.isEmpty() && !customerContact.isEmpty()) {
            System.out.println("Reservation made for: " + customerName + ", Contact: " + customerContact);
        } else {
            System.out.println("Please enter valid details.");
        }
    }

    private void loadMenuItems() {
        // Mock data for the menu
        menuList.getItems().addAll("Pasta - $12", "Burger - $10", "Pizza - $15", "Salad - $8");
    }
}
