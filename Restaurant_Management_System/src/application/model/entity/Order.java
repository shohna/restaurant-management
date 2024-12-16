package application.model.entity;

import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;
import application.DatabaseConnection;

public class Order {
    private UUID id;
    private UUID customerId;
    private List<MenuItem> items;
    private OrderStatus status;
    private double totalAmount;
    private LocalDateTime orderTime;

    public enum OrderStatus {
        NEW, PREPARING, READY, DELIVERED, CANCELLED
    }

    public Order(UUID customerId, List<MenuItem> items) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.items = items;
        this.status = OrderStatus.NEW;
        this.orderTime = LocalDateTime.now();
        calculateTotal(); // Calculate total using the instance's items field
    }

    // Calculate total based on the instance's items field
    private void calculateTotal() {
        this.totalAmount = items.stream()
                                .mapToDouble(MenuItem::getPrice)
                                .sum();
    }

    // Overloaded method to calculate total for any list of MenuItem
    public static double calculateTotal(List<MenuItem> items) {
        return items.stream()
                    .mapToDouble(MenuItem::getPrice)
                    .sum();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
        calculateTotal(); // Recalculate total whenever items are updated
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }
    
    public static List<String> fetchOrders() {
        List<String> orders = new ArrayList<>();
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

                String orderDetails = String.format("Order #%d (Table %d): %s x%d", 
                                                    orderId, tableId, itemName, quantity);
                orders.add(orderDetails);
            }

            // Debugging: Print fetched orders
            System.out.println("Orders fetched from database: " + orders);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }



    public static void placeOrder(int tableId, double totalAmount, List<MenuItem> items) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Insert into orders table
            String insertOrder = "INSERT INTO orders (table_id, total_amount, order_time, status) VALUES (?, ?, ?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, tableId);
            orderStmt.setDouble(2, totalAmount);
            orderStmt.setString(3, LocalDateTime.now().toString());
            orderStmt.setString(4, "PREPARING"); // Automatically set to "PREPARING"
            orderStmt.executeUpdate();

            // Retrieve order ID
            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            int orderId = -1;
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            }

            // Insert into order_items table
            String insertOrderItem = "INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
            PreparedStatement itemStmt = conn.prepareStatement(insertOrderItem);

            for (MenuItem item : items) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, item.getId());
                itemStmt.setInt(3, item.getQuantity());
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
