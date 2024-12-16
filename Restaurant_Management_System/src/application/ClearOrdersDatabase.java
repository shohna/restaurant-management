package application;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class ClearOrdersDatabase {

    public static void main(String[] args) {
        clearDatabase();
    }

    public static void clearDatabase() {
        String[] queries = {
            "DELETE FROM order_items;",
            "DELETE FROM orders;"
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute each query
            for (String query : queries) {
                stmt.executeUpdate(query);
            }

            System.out.println("Orders and order_items tables cleared successfully!");

        } catch (SQLException e) {
            System.err.println("Error while clearing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

