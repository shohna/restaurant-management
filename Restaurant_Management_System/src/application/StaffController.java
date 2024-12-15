package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;

public class StaffController {

    // FXML Injected Controls
    @FXML private ListView<String> orderQueue;
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private GridPane tablesGrid;
    @FXML private Label statusLabel;
    @FXML private Label timeLabel;
    @FXML private ListView<String> kitchenOrdersList;
    @FXML private ComboBox<String> orderStatusCombo;
    
    // Table Columns
    @FXML private TableColumn<Reservation, String> timeColumn;
    @FXML private TableColumn<Reservation, String> nameColumn;
    @FXML private TableColumn<Reservation, String> guestsColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    
    // Reports
    @FXML private LineChart<String, Number> salesChart;
    @FXML private PieChart itemsChart;
    @FXML private BarChart<String, Number> tableUsageChart;
    @FXML private DatePicker reportStartDate;
    @FXML private DatePicker reportEndDate;
    @FXML private ComboBox<String> reportTypeCombo;
    
    private Timeline clock;
    @FXML private Map<Integer, TableButton> tableButtons = new HashMap<>();
    private TableButton selectedTable = null;

    @FXML
    public void initialize() {
        setupClock();
        loadOrders();
        setupTables();
        loadReservations();
        setupKitchenStatus();
        initializeReportControls();
    }

    private void setupClock() {
        clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void setupKitchenStatus() {
        // Initialize order status options
        orderStatusCombo.getItems().addAll(
            "Preparing",
            "Ready",
            "Served"
        );
        
        // Load sample kitchen orders
        ObservableList<String> orders = FXCollections.observableArrayList(
            "Order #1: Burger, Fries (Table 3)",
            "Order #2: Pizza, Salad (Table 7)",
            "Order #3: Pasta, Soup (Table 2)"
        );
        kitchenOrdersList.setItems(orders);
    }

    @FXML
    private void handleUpdateKitchenOrder() {
        String selectedOrder = kitchenOrdersList.getSelectionModel().getSelectedItem();
        String selectedStatus = orderStatusCombo.getValue();
        
        if (selectedOrder != null && selectedStatus != null) {
            updateStatus("Updated order status: " + selectedOrder + " -> " + selectedStatus);
            
            if (selectedStatus.equals("Served")) {
                kitchenOrdersList.getItems().remove(selectedOrder);
            }
        } else {
            showAlert("Error", "Please select both an order and a status");
        }
    }

    @FXML
    private void handleClearTable() {
        TableButton selectedTable = getSelectedTable();
        if (selectedTable == null) {
            showAlert("Error", "Please select a table first");
            return;
        }

        Optional<ButtonType> result = showConfirmationDialog(
            "Clear Table",
            "Are you sure you want to clear this table?",
            "This will remove all assignments and guests"
        );
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedTable.clearTable();
            updateStatus("Table cleared successfully");
        }
    }

    @FXML
    public void handleAssignTable() {
        TableButton selectedTable = getSelectedTable();
        if (selectedTable == null) {
            showAlert("Error", "Please select a table first");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Table");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField guestsField = new TextField();
        guestsField.setPromptText("Number of guests");
        
        ComboBox<String> serverCombo = new ComboBox<>();
        serverCombo.getItems().addAll("Server 1", "Server 2", "Server 3");
        serverCombo.setPromptText("Select server");

        grid.add(new Label("Guests:"), 0, 0);
        grid.add(guestsField, 1, 0);
        grid.add(new Label("Server:"), 0, 1);
        grid.add(serverCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int guests = Integer.parseInt(guestsField.getText());
                String server = serverCombo.getValue();
                if (server == null) throw new IllegalArgumentException("Server must be selected");
                
                selectedTable.assignTable(server, guests);
                updateStatus("Table assigned to " + server);
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid number of guests");
            } catch (IllegalArgumentException e) {
                showAlert("Error", e.getMessage());
            }
        }
    }

    private void loadOrders() {
        ObservableList<String> orders = FXCollections.observableArrayList(
            "Order #1: Table 3 - Burger, Fries",
            "Order #2: Table 7 - Pizza, Salad",
            "Order #3: Table 2 - Pasta, Soup"
        );
        orderQueue.setItems(orders);
    }
    
    private TableButton getSelectedTable() {
        return selectedTable;
    }
    
    private void setupTables() {
        // Create a 3x4 grid of tables (12 tables total)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                int tableNumber = (i * 4) + j + 1;
                TableButton tableButton = new TableButton("Table " + tableNumber);
                tableButton.setPrefSize(150, 150);
                
                tableButtons.put(tableNumber, tableButton);
                
                // Modify click handler
                final int tNum = tableNumber;
                tableButton.setOnAction(e -> {
                    if (selectedTable != null) {
                        selectedTable.setSelected(false);
                    }
                    tableButton.setSelected(true);
                    selectedTable = tableButton;
                    handleTableClick(tNum);
                });
                
                GridPane.setFillWidth(tableButton, true);
                GridPane.setFillHeight(tableButton, true);
                tablesGrid.add(tableButton, j, i);
            }
        }
        
        tablesGrid.setHgap(20);
        tablesGrid.setVgap(20);
        tablesGrid.setPadding(new Insets(20));
    }
    
    private void handleTableClick(int tableNumber) {
        TableButton clickedTable = tableButtons.get(tableNumber);
        if (clickedTable != null) {
            // Show table details in a dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Table " + tableNumber + " Details");
            
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            
            String status = clickedTable.isOccupied() ? "Occupied" : "Available";
            content.getChildren().addAll(
                new Label("Status: " + status)
            );
            
            if (clickedTable.isOccupied()) {
                content.getChildren().addAll(
                    new Label("Server: " + clickedTable.getAssignedServer()),
                    new Label("Guests: " + clickedTable.getNumGuests())
                );
            }

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            dialog.showAndWait();
        }
    }

    private void loadReservations() {
        ObservableList<Reservation> reservations = FXCollections.observableArrayList(
            new Reservation(LocalDateTime.now().plusHours(1), "John Smith", 4, "Confirmed"),
            new Reservation(LocalDateTime.now().plusHours(2), "Sarah Johnson", 2, "Pending"),
            new Reservation(LocalDateTime.now().plusHours(3), "Mike Brown", 6, "Confirmed")
        );

        timeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTime().format(
                DateTimeFormatter.ofPattern("HH:mm")
            ))
        );
        nameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName())
        );
        guestsColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getGuests()))
        );
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus())
        );

        reservationsTable.setItems(reservations);
    }

    @FXML
    private void handleCompleteOrder() {
        String selectedOrder = orderQueue.getSelectionModel().getSelectedItem();
        if (selectedOrder != null) {
            orderQueue.getItems().remove(selectedOrder);
            updateStatus("Completed: " + selectedOrder);
        }
    }

    @FXML
    private void handleCancelOrder() {
        String selectedOrder = orderQueue.getSelectionModel().getSelectedItem();
        if (selectedOrder != null) {
            Optional<ButtonType> result = showConfirmationDialog(
                "Cancel Order",
                "Are you sure you want to cancel this order?",
                selectedOrder
            );
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                orderQueue.getItems().remove(selectedOrder);
                updateStatus("Cancelled: " + selectedOrder);
            }
        }
    }

    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = showConfirmationDialog(
            "Logout",
            "Are you sure you want to logout?",
            "Any unsaved changes will be lost."
        );
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateStatus("Logged out");
        }
    }

    // Report Methods
    @FXML
    public void handleGenerateReport() {
        if (reportStartDate.getValue() == null || reportEndDate.getValue() == null) {
            showAlert("Error", "Please select both start and end dates");
            return;
        }

        setupSalesChart();
        setupPopularItemsChart();
        setupTableUsageChart();
        
        updateStatus("Report generated successfully");
    }

    private void setupSalesChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Sales");
        
        series.getData().add(new XYChart.Data<>("Mon", 1500));
        series.getData().add(new XYChart.Data<>("Tue", 1800));
        series.getData().add(new XYChart.Data<>("Wed", 2100));
        series.getData().add(new XYChart.Data<>("Thu", 2400));
        series.getData().add(new XYChart.Data<>("Fri", 3200));
        
        salesChart.getData().clear();
        salesChart.getData().add(series);
    }

    private void setupPopularItemsChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Burger", 25),
            new PieChart.Data("Pizza", 30),
            new PieChart.Data("Pasta", 20),
            new PieChart.Data("Salad", 15),
            new PieChart.Data("Dessert", 10)
        );
        
        itemsChart.setData(pieChartData);
    }

    private void setupTableUsageChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hours Used");
        
        for (int i = 1; i <= 10; i++) {
            series.getData().add(new XYChart.Data<>("Table " + i, Math.random() * 8));
        }
        
        tableUsageChart.getData().clear();
        tableUsageChart.getData().add(series);
    }

    private void initializeReportControls() {
        reportTypeCombo.getItems().addAll(
            "Daily Sales Report",
            "Popular Items Report",
            "Table Usage Report",
            "Server Performance Report"
        );
    }

    // Utility Methods
    private Optional<ButtonType> showConfirmationDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    // Reservation Class
    public static class Reservation {
        private LocalDateTime time;
        private String name;
        private int guests;
        private String status;
        
        public Reservation(LocalDateTime time, String name, int guests, String status) {
            this.time = time;
            this.name = name;
            this.guests = guests;
            this.status = status;
        }
        
        public LocalDateTime getTime() { return time; }
        public String getName() { return name; }
        public int getGuests() { return guests; }
        public String getStatus() { return status; }
        
        public void setTime(LocalDateTime time) { this.time = time; }
        public void setName(String name) { this.name = name; }
        public void setGuests(int guests) { this.guests = guests; }
        public void setStatus(String status) { this.status = status; }
        
        @Override
        public String toString() {
            return String.format("%s - %s (Guests: %d, Status: %s)",
                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                name,
                guests,
                status);
        }
    }
}