package application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class StaffController {

    @FXML private ListView<String> orderQueue;
    @FXML private Button completeOrderButton;

    public void initialize() {
        loadOrders();
    }

    private void loadOrders() {
        // Mock data for orders
        orderQueue.getItems().addAll("Order #1: Burger, Fries", "Order #2: Pizza, Soda");
    }

    @FXML
    private void handleCompleteOrder() {
        String selectedOrder = orderQueue.getSelectionModel().getSelectedItem();
        if (selectedOrder != null) {
            System.out.println("Completed: " + selectedOrder);
            orderQueue.getItems().remove(selectedOrder);
        } else {
            System.out.println("Please select an order to complete.");
        }
    }
}
