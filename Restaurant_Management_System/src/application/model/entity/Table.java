package application.model.entity;

import java.util.*;

import application.DatabaseConnection;

import java.sql.*;

public class Table {
    private int id;
    private int capacity;
    private TableStatus status;
    private UUID currentReservationId;
    
    public enum TableStatus {
        AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE
    }
    
    public Table(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public TableStatus getStatus() {
		return status;
	}

	public void setStatus(TableStatus status) {
		this.status = status;
	}

	public UUID getCurrentReservationId() {
		return currentReservationId;
	}

	public void setCurrentReservationId(UUID currentReservationId) {
		this.currentReservationId = currentReservationId;
	}
    
	public void updateTableStatus(int tableId, String status) {
	    String query = "UPDATE tables SET status = ? WHERE id = ?";
	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setString(1, status);
	        stmt.setInt(2, tableId);
	        stmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
}
