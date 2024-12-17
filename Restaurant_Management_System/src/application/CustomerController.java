package application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import application.model.entity.MenuItem;
import application.network.client.RMSClient;
import application.network.message.MessageType;
import application.network.message.NetworkMessage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CustomerController {

    private final List<MenuItem> selectedItems = new ArrayList<>();
    private RMSClient client;
    
    @FXML
    public void initialize() {
        try {
            client = new RMSClient("localhost", 5000);
        } catch (IOException e) {
            showAlert("Connection Error", "Could not connect to server: " + e.getMessage());
        }
    }

    @FXML
    public void showMenu() {
        Map<String, List<MenuItem>> menuItems = MenuItem.fetchMenuItems();

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Our Menu");

        VBox content = new VBox(10);
        content.getStyleClass().add("menu-content");

        // Add categories dynamically
        for (Map.Entry<String, List<MenuItem>> entry : menuItems.entrySet()) {
            content.getChildren().addAll(
                    createMenuCategory(entry.getKey(), entry.getValue()),
                    new Separator());
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(scrollPane);

        ButtonType addToOrderButton = new ButtonType("Add to Order", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(addToOrderButton, ButtonType.CLOSE);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addToOrderButton) {
                handleAddToOrder();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private VBox createMenuCategory(String categoryName, List<MenuItem> items) {
        VBox category = new VBox(5);

        // Category header
        Label header = new Label(categoryName);
        header.getStyleClass().add("category-header");
        category.getChildren().add(header);

        for (MenuItem item : items) {
            HBox itemBox = new HBox(10);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            // Checkbox for item selection
            CheckBox checkBox = new CheckBox(item.getName());
            checkBox.setUserData(item);

            // Quantity Spinner
            Spinner<Integer> quantitySpinner = new Spinner<>(1, 10, 1);
            quantitySpinner.setPrefWidth(60);
            quantitySpinner.setDisable(true); // Disabled until item is selected

            // Enable spinner when item is selected
            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                quantitySpinner.setDisable(!isSelected);
                if (isSelected) {
                    item.setQuantity(quantitySpinner.getValue());
                    selectedItems.add(item);
                } else {
                    selectedItems.remove(item);
                }
            });

            quantitySpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (checkBox.isSelected()) {
                    item.setQuantity(newValue); // Update quantity for the selected item
                }
            });

            itemBox.getChildren().addAll(checkBox, new Label("$" + item.getPrice()), quantitySpinner);
            category.getChildren().add(itemBox);
        }
        return category;
    }

    @FXML
    public void showReservationDialog() {
    	System.out.println("Sending reservation request...");
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Make a Reservation");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("custom-dialog");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Add Reservation Form
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label dateLabel = new Label("Date:");
        DatePicker datePicker = new DatePicker();

        Label timeLabel = new Label("Time:");
        ComboBox<String> timeComboBox = new ComboBox<>();
        timeComboBox.getItems().addAll("5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM");

        Label guestsLabel = new Label("Number of Guests:");
        Spinner<Integer> guestSpinner = new Spinner<>(1, 10, 2);

        content.getChildren().addAll(
                nameLabel, nameField,
                dateLabel, datePicker,
                timeLabel, timeComboBox,
                guestsLabel, guestSpinner);

        dialogPane.setContent(content);

        ButtonType reserveButton = new ButtonType("Reserve", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(reserveButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == reserveButton) {
                // Validate reservation fields
                StringBuilder errors = new StringBuilder();

                if (nameField.getText().isEmpty()) {
                    errors.append("Name is required.\n");
                }
                if (datePicker.getValue() == null) {
                    errors.append("Date is required.\n");
                }
                if (timeComboBox.getValue() == null) {
                    errors.append("Time is required.\n");
                }

                if (errors.length() > 0) {
                    showAlert("Missing Information", errors.toString());
                    return null;
                }

                // Check if network client is available
                if (client == null) {
                    showAlert("Network Error", 
                        "Cannot make reservation: Not connected to server");
                    return null;
                }

                // Collect and process reservation data
                Map<String, Object> reservationData = new HashMap<>();
                reservationData.put("name", nameField.getText());
                reservationData.put("date", datePicker.getValue().toString());
                reservationData.put("time", timeComboBox.getValue());
                reservationData.put("guests", guestSpinner.getValue());

                client.sendMessage(MessageType.MAKE_RESERVATION, reservationData)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.getType() == MessageType.SUCCESS) {
                                showAlert("Success", "Reservation made successfully!");
                            } else {
                                showAlert("Error", 
                                    "Failed to make reservation: " + response.getPayload());
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> 
                            showAlert("Error", 
                                "Network error while making reservation: " + e.getMessage())
                        );
                        return null;
                    });
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleAddToOrder() {
        if (selectedItems.isEmpty()) {
            showAlert("No Items Selected", "Please select at least one item to add to your order.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            // 1. Fetch an available table randomly
            int tableId = fetchRandomAvailableTable(conn);
            if (tableId == -1) {
                showAlert("Error", "No tables are currently available.");
                return;
            }

            // 2. Insert into orders table
            String insertOrder = "INSERT INTO orders (table_id, total_amount, order_time, status) VALUES (?, ?, ?, ?)";
            int orderId;
            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, tableId);
                orderStmt.setDouble(2, calculateTotal());
                orderStmt.setString(3, LocalDateTime.now().toString());
                orderStmt.setString(4, "NEW");
                orderStmt.executeUpdate();

                ResultSet generatedKeys = orderStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve order ID.");
                }
            }

            // 3. Insert into order_items table
            String insertOrderItem = "INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement itemStmt = conn.prepareStatement(insertOrderItem)) {
                for (MenuItem item : selectedItems) {
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, item.getId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            // 4. Update the table status to 'OCCUPIED'
            try (PreparedStatement updateTable = conn
                    .prepareStatement("UPDATE tables SET status = 'OCCUPIED' WHERE id = ?")) {
                updateTable.setInt(1, tableId);
                updateTable.executeUpdate();
            }

            conn.commit(); // Commit transaction

            showAlert("Order Placed", "Your order has been placed successfully! Your Table Number is: " + tableId);

            selectedItems.clear();
            // StaffController.updateStaffDashboard(); // Notify staff to refresh tables

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to place the order: " + e.getMessage());
        }
    }

    // Fetch a random available table
    private int fetchRandomAvailableTable(Connection conn) throws SQLException {
        String query = "SELECT id FROM tables WHERE status = 'AVAILABLE'";
        List<Integer> availableTables = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                availableTables.add(rs.getInt("id"));
            }
        }
        if (availableTables.isEmpty()) {
            return -1; // No tables available
        }
        Collections.shuffle(availableTables); // Shuffle to pick a random table
        return availableTables.get(0);
    }

    private double calculateTotal() {
        return selectedItems.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
    }

    private void showOrderSummary(double total) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Order Summary");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        for (MenuItem item : selectedItems) {
            Label itemLabel = new Label(item.getName() + " - $" + item.getPrice());
            content.getChildren().add(itemLabel);
        }

        Label totalLabel = new Label("Total: $" + String.format("%.2f", total));
        totalLabel.getStyleClass().add("total-label");
        content.getChildren().add(totalLabel);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}