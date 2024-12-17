package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.*;

public class StaffController {

	// FXML Injected Controls
	@FXML
	private TableView<Reservation> reservationsTable;
	@FXML
	private GridPane tablesGrid;
	@FXML
	private Label statusLabel;
	@FXML
	private Label timeLabel;
	@FXML
	private ListView<String> kitchenOrdersList;

	// Table Columns
	@FXML
	private TableColumn<Reservation, String> timeColumn;
	@FXML
	private TableColumn<Reservation, String> nameColumn;
	@FXML
	private TableColumn<Reservation, String> guestsColumn;
	@FXML
	private TableColumn<Reservation, String> statusColumn;

	// Reports
	@FXML
	private LineChart<String, Number> salesChart;
	@FXML
	private PieChart itemsChart;
	@FXML
	private BarChart<String, Number> tableUsageChart;
	@FXML
	private DatePicker reportStartDate;
	@FXML
	private DatePicker reportEndDate;
	@FXML
	private ComboBox<String> reportTypeCombo;

	private Timeline clock;
	private ScheduledExecutorService executorService;
	@FXML
	private Map<Integer, TableButton> tableButtons = new HashMap<>();
	private TableButton selectedTable = null;

	@FXML
	public void initialize() {
		setupClock();
		setupTables();
		loadReservations();
		initializeReportControls();
		setupPeriodicUIRefresh();
		inQueueOrdersList.setItems(inQueueOrders);
		readyToServeOrdersList.setItems(readyToServeOrders);
		processedOrdersList.setItems(processedOrders);

	}

	private void setupClock() {
		clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
			LocalDateTime now = LocalDateTime.now();
			timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		}), new KeyFrame(Duration.seconds(1)));
		clock.setCycleCount(Timeline.INDEFINITE);
		clock.play();
	}

	private void setupPeriodicUIRefresh() {
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(() -> {
			Platform.runLater(() -> {
				reloadOrdersAndKitchen(); // Update kitchen orders
				refreshTablesStatus(); // Refresh table statuses
				updateStatus("UI refreshed at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
			});
		}, 0, 10, TimeUnit.SECONDS);
	}

	@FXML
	private void refreshTablesStatus() {
		try (Connection conn = DatabaseConnection.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT id, status FROM tables")) {

			while (rs.next()) {
				int tableId = rs.getInt("id");
				String status = rs.getString("status");
				TableButton tableButton = tableButtons.get(tableId);
				if (tableButton != null) {
					if (status.equalsIgnoreCase("OCCUPIED")) {
						tableButton.setStyle("-fx-background-color: red;");
					} else {
						tableButton.setStyle("-fx-background-color: green;");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void reloadOrdersAndKitchen() {
	    List<String> orders = fetchKitchenOrders();

	    // Add only new orders to In Queue
	    for (String order : orders) {
	        if (!inQueueOrders.contains(order) && !readyToServeOrders.contains(order) && !processedOrders.contains(order)) {
	            inQueueOrders.add(order);
	        }
	    }

	    // Update ListViews
	    inQueueOrdersList.setItems(inQueueOrders);
	    readyToServeOrdersList.setItems(readyToServeOrders);
	    processedOrdersList.setItems(processedOrders);
	}


	

	// Fetch orders for display in Kitchen Orders
	private List<String> fetchKitchenOrders() {
		List<String> kitchenOrders = new ArrayList<>();
		String query = """
				    SELECT o.id, o.table_id, m.name, oi.quantity
				    FROM orders o
				    JOIN order_items oi ON o.id = oi.order_id
				    JOIN menu_items m ON oi.menu_item_id = m.id
				    WHERE o.status != 'COMPLETED'
				""";

		try (Connection conn = DatabaseConnection.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {

			while (rs.next()) {
				int orderId = rs.getInt("id");
				int tableId = rs.getInt("table_id");
				String itemName = rs.getString("name");
				int quantity = rs.getInt("quantity");

				// Format orders for display
				String orderDetails = String.format("Order #%d (Table %d): %s x%d",
						orderId, tableId, itemName, quantity);
				kitchenOrders.add(orderDetails);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return kitchenOrders;
	}

	private void shutdownExecutorService() {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	@FXML
	private void handleClearTable() {
	    TableButton selectedTable = tableButtons.values().stream()
	            .filter(TableButton::isSelected) // Check for selection
	            .findFirst()
	            .orElse(null);

	    if (selectedTable == null) {
	        showAlert("No Table Selected", "Please select a table to clear.");
	        return;
	    }

	    Optional<ButtonType> result = showConfirmationDialog(
	            "Clear Table", "Are you sure you want to clear this table?", 
	            "This will mark the table as available."
	    );

	    if (result.isPresent() && result.get() == ButtonType.OK) {
	        int tableId = selectedTable.getTableId();
	        updateTableStatusInDatabase(tableId, "AVAILABLE");

	        // Immediately clear the table and force UI update
	        selectedTable.clearTable();
	        Platform.runLater(() -> {
	            selectedTable.setStyle("-fx-background-color: green; -fx-text-fill: white;");
	        });

	        statusLabel.setText("Table " + tableId + " cleared successfully.");
	    }
	}


	
	private void updateTableStatusInDatabase(int tableId, String status) {
	    String query = "UPDATE tables SET status = ? WHERE id = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {

	        stmt.setString(1, status);
	        stmt.setInt(2, tableId);
	        stmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	        showAlert("Database Error", "Failed to update the table status.");
	    }
	}
	
	@FXML
	private ListView<String> inQueueOrdersList;
	@FXML
	private ListView<String> readyToServeOrdersList;
	@FXML
	private ListView<String> processedOrdersList;

	private ObservableList<String> inQueueOrders = FXCollections.observableArrayList();
	private ObservableList<String> readyToServeOrders = FXCollections.observableArrayList();
	private ObservableList<String> processedOrders = FXCollections.observableArrayList();
	private Map<String, ScheduledExecutorService> orderSchedulers = new HashMap<>();

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
				if (server == null)
					throw new IllegalArgumentException("Server must be selected");

				selectedTable.assignTable(server, guests);
				updateStatus("Table assigned to " + server);
			} catch (NumberFormatException e) {
				showAlert("Error", "Please enter a valid number of guests");
			} catch (IllegalArgumentException e) {
				showAlert("Error", e.getMessage());
			}
		}
	}

	private TableButton getSelectedTable() {
		return selectedTable;
	}

	private void setupTables() {
	    // Create a 3x4 grid of tables (12 tables total)
	    for (int i = 0; i < 3; i++) {
	        for (int j = 0; j < 4; j++) {
	            int tableId = (i * 4) + j + 1; // Unique table ID for each table
	            TableButton tableButton = new TableButton(tableId, "Table " + tableId);
	            tableButton.setPrefSize(150, 150);

	            tableButtons.put(tableId, tableButton); // Add tableButton to the map

	            // Click handler for selecting the table
	            tableButton.setOnAction(e -> {
	                if (selectedTable != null) {
	                    selectedTable.setSelected(false);
	                }
	                tableButton.setSelected(true);
	                selectedTable = tableButton;
	                handleTableClick(tableId);
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
	        if (selectedTable != null) {
	            selectedTable.setSelected(false); // Deselect the previous table
	        }
	        clickedTable.setSelected(true); // Select the new table
	        selectedTable = clickedTable;
	    }
	}


	private void loadReservations() {
		ObservableList<Reservation> reservations = FXCollections.observableArrayList(
				new Reservation(LocalDateTime.now().plusHours(1), "John Smith", 4, "Confirmed"),
				new Reservation(LocalDateTime.now().plusHours(2), "Sarah Johnson", 2, "Pending"),
				new Reservation(LocalDateTime.now().plusHours(3), "Mike Brown", 6, "Confirmed"));

		timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime().format(
				DateTimeFormatter.ofPattern("HH:mm"))));
		nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
		guestsColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getGuests())));
		statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

		reservationsTable.setItems(reservations);
	}

	@FXML
	private void handleCompleteOrder() {
		String selectedOrder = kitchenOrdersList.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			int orderId = extractOrderId(selectedOrder);
			updateOrderStatus(orderId, "COMPLETED");
			reloadOrdersAndKitchen(); // Refresh both lists
		}
	}

	@FXML
	private void handleCancelOrder() {
		String selectedOrder = kitchenOrdersList.getSelectionModel().getSelectedItem();
		if (selectedOrder != null) {
			int orderId = extractOrderId(selectedOrder);
			updateOrderStatus(orderId, "CANCELLED");
			reloadOrdersAndKitchen(); // Refresh both lists
		}
	}

	private int extractOrderId(String orderString) {
		try {
			// Debugging: Print the order string being parsed
			System.out.println("Parsing Order String: " + orderString);

			// Extract the order ID from the string
			return Integer.parseInt(orderString.split("#")[1].split(" ")[0]);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid order format: " + orderString);
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
				new PieChart.Data("Dessert", 10));

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
				"Server Performance Report");
	}
	
	private void moveOrderToReady(String orderDetails) {
	    inQueueOrders.remove(orderDetails);
	    readyToServeOrders.add(orderDetails);

	    // Update order status in the database
	    int orderId = extractOrderId(orderDetails);
	    updateOrderStatus(orderId, "READY_TO_SERVE");
	}
	
	@FXML
	private void handleMoveOrderToReady() {
	    String selectedOrder = inQueueOrdersList.getSelectionModel().getSelectedItem();

	    if (selectedOrder != null) {
	        // Remove from In Queue list
	        inQueueOrders.remove(selectedOrder);

	        // Add to Ready to Serve list
	        readyToServeOrders.add(selectedOrder);

	        // Update status in the database
	        int orderId = extractOrderId(selectedOrder);
	        updateOrderStatus(orderId, "READY_TO_SERVE");

	        showAlert("Order Moved", "Order has been moved to 'Ready to Serve'.");
	    } else {
	        showAlert("No Order Selected", "Please select an order to move to 'Ready to Serve'.");
	    }
	}


	private void updateOrderStatus(int orderId, String status) {
	    String query = "UPDATE orders SET status = ? WHERE id = ?";
	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setString(1, status);
	        stmt.setInt(2, orderId);
	        stmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}



	
	@FXML
	private void handleMarkAsServed() {
	    String selectedOrder = readyToServeOrdersList.getSelectionModel().getSelectedItem();
	    if (selectedOrder != null) {
	        readyToServeOrders.remove(selectedOrder);
	        processedOrders.add(selectedOrder);
	        showAlert("Order Processed", "Order has been marked as served.");
	    } else {
	        showAlert("No Order Selected", "Please select an order to mark as served.");
	    }
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

		public LocalDateTime getTime() {
			return time;
		}

		public String getName() {
			return name;
		}

		public int getGuests() {
			return guests;
		}

		public String getStatus() {
			return status;
		}

		public void setTime(LocalDateTime time) {
			this.time = time;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setGuests(int guests) {
			this.guests = guests;
		}

		public void setStatus(String status) {
			this.status = status;
		}

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