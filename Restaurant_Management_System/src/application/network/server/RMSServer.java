package application.network.server;

import java.io.IOException;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import application.network.message.*;
import application.DatabaseConnection;

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
                case GET_UPDATES :
                    return handleGetUpdates(); 
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
    
    private NetworkMessage handleGetUpdates() {
        return new NetworkMessage(MessageType.SUCCESS, new HashMap<>());
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
    
}