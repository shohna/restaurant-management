package application.model.entity;

import java.util.UUID;

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
    
}
