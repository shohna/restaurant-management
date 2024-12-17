package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:restaurant.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            enableWALMode(connection); // Enable WAL mode for concurrent access
        }
        return connection;
    }

    private static void enableWALMode(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
        } catch (SQLException e) {
            System.err.println("Failed to enable WAL mode: " + e.getMessage());
        }
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

            //conn.createStatement().execute("DROP TABLE IF EXISTS reservations");
            // Create reservations table
            conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        time TEXT NOT NULL,
                        name TEXT NOT NULL,
                        guests INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """);

            // Create menu_items table
            conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS menu_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            description TEXT,
                            price REAL NOT NULL,
                            category TEXT,
                            is_available INTEGER DEFAULT 1
                        )
                    """);

            // Create orders table
            conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS orders (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            table_id INTEGER,
                            customer_id INTEGER,
                            total_amount REAL,
                            order_time TEXT,
                            status TEXT DEFAULT 'NEW',
                            FOREIGN KEY (table_id) REFERENCES tables(id),
                            FOREIGN KEY (customer_id) REFERENCES users(id)
                        )
                    """);

            // Create order_items table
            conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS order_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            order_id INTEGER,
                            menu_item_id INTEGER,
                            quantity INTEGER NOT NULL,
                            FOREIGN KEY (order_id) REFERENCES orders(id),
                            FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
                        )
                    """);

            // Create tables table
            conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS tables (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            capacity INTEGER NOT NULL,
                            status TEXT DEFAULT 'AVAILABLE'
                        )
                    """);

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM tables");
                if (rs.next() && rs.getInt("count") == 0) {
                    stmt.executeUpdate("""
                                INSERT INTO tables (capacity, status) VALUES
                                (4, 'AVAILABLE'),
                                (4, 'AVAILABLE'),
                                (6, 'AVAILABLE'),
                                (6, 'AVAILABLE'),
                                (2, 'AVAILABLE'),
                                (2, 'AVAILABLE'),
                                (8, 'AVAILABLE'),
                                (8, 'AVAILABLE'),
                                (4, 'AVAILABLE'),
                                (4, 'AVAILABLE')
                            """);
                    System.out.println("Tables initialized successfully.");
                }
            }

            // Insert default menu items if not already present
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM menu_items");
                if (rs.next() && rs.getInt("count") == 0) {
                    stmt.executeUpdate(
                            """
                                        INSERT INTO menu_items (name, description, price, category, is_available) VALUES
                                        ('Classic Bruschetta', 'Fresh tomatoes, garlic, basil on toasted bread', 12.00, 'Appetizers', 5),
                                        ('Grilled Salmon', 'Fresh Atlantic salmon with seasonal vegetables', 28.00, 'Main Course', 10),
                                        ('Tiramisu', 'Classic Italian coffee-flavored dessert', 10.00, 'Desserts', 2),
                                        ('Crispy Calamari', 'Served with marinara sauce and lemon', 16.00, 'Appetizers', 8),
                                        ('Beef Tenderloin', '8oz with red wine reduction sauce', 34.00, 'Main Course', 5),
                                        ('Mushroom Risotto', 'Arborio rice, wild mushrooms, parmesan', 24.00, 'Main Course', 3),
                                        ('Chocolate Lava Cake', 'Warm chocolate cake with vanilla ice cream', 12.00, 'Desserts', 5),
                                        ('Creme Brulee', 'Classic French vanilla custard', 9.00, 'Desserts', 10),
                                        ('Garden Salad', 'Mixed greens, cherry tomatoes, cucumber', 10.00, 'Appetizers', 10)
                                    """);
                }
            }
        }
    }
}
