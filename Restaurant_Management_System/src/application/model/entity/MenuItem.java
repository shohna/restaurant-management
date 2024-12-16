package application.model.entity;

import java.sql.*;
import java.util.*;

import application.DatabaseConnection;
import javafx.beans.property.*;

public class MenuItem {
    private final int id; // Add this field
    private final SimpleStringProperty name;
    private final SimpleDoubleProperty price;
    private final SimpleStringProperty description;
    private final SimpleStringProperty category;
    private int quantity; // Quantity selected for an order

    public MenuItem(int id, String name, double price, String description, String category) {
        this.id = id;
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.description = new SimpleStringProperty(description);
        this.category = new SimpleStringProperty(category);
        this.quantity = 0; // Default quantity
    }

    // Getter for id
    public int getId() {
        return id;
    }

    // Other getters and setters
    public String getName() {
        return name.get();
    }

    public double getPrice() {
        return price.get();
    }

    public String getDescription() {
        return description.get();
    }

    public String getCategory() {
        return category.get();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public static Map<String, List<MenuItem>> fetchMenuItems() {
        Map<String, List<MenuItem>> categorizedMenu = new HashMap<>();
        String query = "SELECT id, name, price, description, category FROM menu_items WHERE is_available > 0";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String description = rs.getString("description");
                String category = rs.getString("category");

                MenuItem item = new MenuItem(id, name, price, description, category);
                categorizedMenu.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categorizedMenu;
    }

}
