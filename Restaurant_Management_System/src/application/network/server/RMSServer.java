package application.network.server;

import java.io.IOException;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import application.network.message.*;
import application.DatabaseConnection;
import application.model.entity.MenuItem;

public class RMSServer {
    private static final int PORT = 5000;
    private final ServerSocket serverSocket;
    private final ExecutorService clientPool;
    private final ConcurrentHashMap<String, ClientHandler> connectedClients;
    private final Connection dbConnection;

    public RMSServer() throws IOException, SQLException {
        this.serverSocket = new ServerSocket(PORT);
        this.clientPool = Executors.newFixedThreadPool(20);
        this.connectedClients = new ConcurrentHashMap<>();
        this.dbConnection = DatabaseConnection.getConnection();
    }

    public void start() {
        System.out.println("Server starting on port " + PORT);
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from: " + 
                    clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientPool.execute(handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Handler for database operations
    public NetworkMessage handleDatabaseOperation(NetworkMessage message) {
        try {
            switch (message.getType()) {
                case MAKE_RESERVATION:
                    return handleReservation(message);
                case GET_ALL_RESERVATIONS:  // Add this case
                    return handleGetAllReservations();
//                case GET_UPDATES:
//                    return handleGetAllReservations(); 
//                case GET_AVAILABLE_TABLES:
//                    return handleGetTables(message);
//                case PLACE_ORDER:
//                    return handleOrder(message);
                // Add other cases
                default:
                    return new NetworkMessage(MessageType.ERROR, "Unsupported operation");
            }
        } catch (Exception e) {
            return new NetworkMessage(MessageType.ERROR, e.getMessage());
        }
    }
    
    private NetworkMessage handleReservation(NetworkMessage message) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            @SuppressWarnings("unchecked")
            Map<String, Object> reservationData = (Map<String, Object>) message.getPayload();
            
            String sql = "INSERT INTO reservations (time, name, guests, status) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Combine date and time
                String date = (String) reservationData.get("date");
                String time = (String) reservationData.get("time");
                pstmt.setString(1, date + " " + time);
                pstmt.setString(2, (String) reservationData.get("name"));
                pstmt.setInt(3, (Integer) reservationData.get("guests"));
                pstmt.setString(4, "PLACED"); // Initial status
                
                System.out.println("Executing reservation insert with data: " + reservationData);
                
                int result = pstmt.executeUpdate();
                
                if (result > 0) {
                    return new NetworkMessage(MessageType.SUCCESS, "Reservation created successfully");
                } else {
                    return new NetworkMessage(MessageType.ERROR, "Failed to create reservation");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new NetworkMessage(MessageType.ERROR, "Database error: " + e.getMessage());
        }
    }
    
    private NetworkMessage handleGetAllReservations() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM reservations ORDER BY time";
            
            List<Map<String, Object>> reservations = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Map<String, Object> reservation = new HashMap<>();
                    reservation.put("time", rs.getString("time"));
                    reservation.put("name", rs.getString("name"));
                    reservation.put("guests", rs.getInt("guests"));
                    reservation.put("status", rs.getString("status"));
                    reservations.add(reservation);
                }
                
                return new NetworkMessage(MessageType.SUCCESS, reservations);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new NetworkMessage(MessageType.ERROR, "Database error: " + e.getMessage());
        }
    }
    
    private NetworkMessage handleGetTables(NetworkMessage message) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM tables WHERE status = 'AVAILABLE'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                List<Map<String, Object>> availableTables = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> table = new HashMap<>();
                    table.put("id", rs.getInt("id"));
                    table.put("capacity", rs.getInt("capacity"));
                    table.put("status", rs.getString("status"));
                    availableTables.add(table);
                }
                
                return new NetworkMessage(MessageType.SUCCESS, availableTables);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new NetworkMessage(MessageType.ERROR, "Database error: " + e.getMessage());
        }
    }

    private NetworkMessage handleOrder(NetworkMessage message) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) message.getPayload();
            
            // First, find an available table
            int tableId = findAvailableTable(conn);
            if (tableId == -1) {
                conn.rollback();
                return new NetworkMessage(MessageType.ERROR, "No tables available");
            }
            
            // Insert order
            String orderSql = "INSERT INTO orders (table_id, total_amount, order_time, status) VALUES (?, ?, ?, ?)";
            int orderId;
            
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, tableId);
                pstmt.setDouble(2, (Double) orderData.get("totalAmount"));
                pstmt.setString(3, (String) orderData.get("orderTime"));
                pstmt.setString(4, "NEW");
                
                pstmt.executeUpdate();
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        return new NetworkMessage(MessageType.ERROR, "Failed to create order");
                    }
                }
            }
            
            // Insert order items
            String itemSql = "INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(itemSql)) {
                @SuppressWarnings("unchecked")
                List<MenuItem> items = (List<MenuItem>) orderData.get("items");
                
                for (MenuItem item : items) {
                    pstmt.setInt(1, orderId);
                    pstmt.setInt(2, item.getId());
                    pstmt.setInt(3, item.getQuantity());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
            }
            
            // Update table status
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE tables SET status = 'OCCUPIED' WHERE id = ?")) {
                pstmt.setInt(1, tableId);
                pstmt.executeUpdate();
            }
            
            conn.commit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("tableId", tableId);
            
            return new NetworkMessage(MessageType.SUCCESS, response);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return new NetworkMessage(MessageType.ERROR, "Database error: " + e.getMessage());
        }
    }

    private int findAvailableTable(Connection conn) throws SQLException {
        String sql = "SELECT id FROM tables WHERE status = 'AVAILABLE' LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        }
    }
}