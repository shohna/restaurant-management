package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:restaurant.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    public static void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Create users table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    name TEXT NOT NULL,
                    contact TEXT
                )
            """);

            // Create reservations table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER,
                    date_time TEXT NOT NULL,
                    table_id INTEGER,
                    status TEXT NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES users(id)
                )
            """);

            // Add more table creation statements as needed
        }
    }
}