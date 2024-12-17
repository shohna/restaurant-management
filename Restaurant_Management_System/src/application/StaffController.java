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
import java.util.stream.Collectors;
import java.io.IOException;
import java.sql.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import application.model.entity.Order;
import application.network.client.RMSClient;
import application.network.message.MessageType;

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
	@FXML
	private ComboBox<String> orderStatusCombo;

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
	@FXML
	private Map<Integer, TableButton> tableButtons = new HashMap<>();
	private TableButton selectedTable = null;
	
	 private RMSClient client;
	 private Timeline networkUpdateTimer;

	@FXML
	public void initialize() throws IOException {
		
		// Initialize network client
        initializeNetworkConnection();
        
        timeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null && cellData.getValue().getTime() != null) {
                LocalDateTime dateTime = cellData.getValue().getTime();
                String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a"));
                return new SimpleStringProperty(formattedTime);
            }
            return new SimpleStringProperty("");
        });
        
        nameColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getName());
            }
            return new SimpleStringProperty("");
        });
        
        guestsColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(String.valueOf(cellData.getValue().getGuests()));
            }
            return new SimpleStringProperty("");
        });
        
        statusColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getStatus());
            }
            return new SimpleStringProperty("");
        });

        // Make sure the table is not null and visible
        if (reservationsTable != null) {
            reservationsTable.setVisible(true);
        } else {
            System.err.println("reservationsTable is null!");
        }
        
        // Setup periodic updates
        setupNetworkUpdates();
        
		setupClock();
		setupTables();
		loadReservations();
		setupKitchenStatus();
		initializeReportControls();
		
		startListeningForUpdates();
	}
	
	private void initializeNetworkConnection() {
        try {
            System.out.println("Attempting to connect to server...");
            client = new RMSClient("localhost", 5000);
            System.out.println("Successfully connected to server");
        } catch (IOException e) {
            Platform.runLater(() -> {
                showAlert("Connection Error", 
                    "Could not connect to server: " + e.getMessage() + 
                    "\nSome features may not be available.");
            });
            e.printStackTrace();
        }
    }
	
	private void startListeningForUpdates() {
	    Thread listenerThread = new Thread(() -> {
	        try {
	            while (!Thread.currentThread().isInterrupted()) {
	                client.sendMessage(MessageType.GET_UPDATES, null)
	                    .thenAccept(response -> {
	                        if (response.getType() == MessageType.SUCCESS) {
	                            @SuppressWarnings("unchecked")
	                            Map<String, Object> updates = (Map<String, Object>) response.getPayload();
	                            
	                            Platform.runLater(() -> {
	                                if (updates.containsKey("reservations")) {
	                                    @SuppressWarnings("unchecked")
	                                    List<Map<String, Object>> reservations = 
	                                        (List<Map<String, Object>>) updates.get("reservations");
	                                    updateReservationsFromMaps(reservations);
	                                }
	                            });
	                        }
	                    })
	                    .exceptionally(e -> {
	                        System.err.println("Update error: " + e.getMessage());
	                        return null;
	                    });
	                
	                // Increase the delay between updates to reduce server load
	                Thread.sleep(5000);  // Check every 5 seconds instead of 2
	            }
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
	    });
	    
	    listenerThread.setDaemon(true);
	    listenerThread.start();
	}

	private void updateReservationsFromMaps(List<Map<String, Object>> reservationMaps) {
	    List<Reservation> reservations = reservationMaps.stream()
	        .map(map -> {
	            try {
	                LocalDateTime time = LocalDateTime.parse(
	                    (String) map.get("time"),
	                    DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.ENGLISH)
	                );
	                
	                return new Reservation(
	                    time,
	                    (String) map.get("name"),
	                    ((Number) map.get("guests")).intValue(),
	                    (String) map.get("status")
	                );
	            } catch (Exception e) {
	                System.err.println("Error parsing reservation: " + map);
	                return null;
	            }
	        })
	        .filter(Objects::nonNull)
	        .collect(Collectors.toList());
	        
	    ObservableList<Reservation> observableReservations = 
	        FXCollections.observableArrayList(reservations);
	    reservationsTable.setItems(observableReservations);
	}
	
	private void updateReservationsTable(List<Reservation> reservations) {
	    ObservableList<Reservation> observableReservations = FXCollections.observableArrayList(reservations);
	    reservationsTable.setItems(observableReservations);
	}
	
	private void setupNetworkUpdates() {
        networkUpdateTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
        	loadReservations();
//            fetchOrderUpdates();
//            fetchTableStatuses();
        }));
        networkUpdateTimer.setCycleCount(Timeline.INDEFINITE);
        networkUpdateTimer.play();
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
				"Served");
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
		ObservableList<String> observableOrders = FXCollections.observableArrayList(orders);
		kitchenOrdersList.setItems(observableOrders);

		refreshTablesStatus(); // Refresh table colors
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

	@FXML
	private void handleUpdateKitchenOrder() {
		// Get the selected order and status
		String selectedOrder = kitchenOrdersList.getSelectionModel().getSelectedItem();
		String selectedStatus = orderStatusCombo.getValue();

		// Debugging: Verify inputs
		System.out.println("Selected Order: " + selectedOrder);
		System.out.println("Selected Status: " + selectedStatus);

		// Check if both inputs are valid
		if (selectedOrder == null || selectedStatus == null) {
			showAlert("Error", "Please select both an order and a status.");
			return;
		}

		// Extract order ID and update status
		int orderId = extractOrderId(selectedOrder);
		updateOrderStatus(orderId, selectedStatus);

		// Reload the Kitchen Orders
		reloadOrdersAndKitchen();
		showAlert("Success", "Order status updated successfully!");
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
				"This will remove all assignments and guests");

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
					new Label("Status: " + status));

			if (clickedTable.isOccupied()) {
				content.getChildren().addAll(
						new Label("Server: " + clickedTable.getAssignedServer()),
						new Label("Guests: " + clickedTable.getNumGuests()));
			}

			dialog.getDialogPane().setContent(content);
			dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

			dialog.showAndWait();
		}
	}

	private void loadReservations() {
	    System.out.println("Loading reservations...");
	    client.sendMessage(MessageType.GET_ALL_RESERVATIONS, null)
	    .thenAccept(response -> {
	        System.out.println("Received response: " + response.getType());
	        if (response.getType() == MessageType.SUCCESS) {
	            @SuppressWarnings("unchecked")
	            List<Map<String, Object>> reservationMaps = (List<Map<String, Object>>) response.getPayload();
	            
	            List<Reservation> reservations = reservationMaps.stream()
	                .map(map -> {
	                    try {
	                        // Parse the datetime string with 12-hour format
	                        String timeStr = (String) map.get("time");
	                        LocalDateTime time = LocalDateTime.parse(
	                            timeStr,
	                            DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.ENGLISH)
	                        );
	                        
	                        Reservation reservation = new Reservation(
	                            time,
	                            (String) map.get("name"),
	                            ((Number) map.get("guests")).intValue(),
	                            (String) map.get("status")
	                        );
	                        
	                        System.out.println("Successfully parsed reservation: " + reservation);
	                        return reservation;
	                    } catch (Exception e) {
	                        System.err.println("Error parsing reservation data: " + map);
	                        e.printStackTrace();
	                        return null;
	                    }
	                })
	                .filter(Objects::nonNull)
	                .collect(Collectors.toList());
	            
	            Platform.runLater(() -> {
	                ObservableList<Reservation> observableReservations = 
	                    FXCollections.observableArrayList(reservations);
	                reservationsTable.setItems(observableReservations);
	                System.out.println("Table updated with " + observableReservations.size() + " items");
	            });
	        } else {
	            System.out.println("Error response: " + response.getPayload());
	        }
	    }).exceptionally(e -> {
	        System.err.println("Error loading reservations: " + e.getMessage());
	        e.printStackTrace();
	        return null;
	    });
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
	private void handleLogout() {
		Optional<ButtonType> result = showConfirmationDialog(
				"Logout",
				"Are you sure you want to logout?",
				"Any unsaved changes will be lost.");

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