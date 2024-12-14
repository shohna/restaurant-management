package application.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reservation {
    private UUID id;
    private UUID customerId;
    private LocalDateTime dateTime;
    private int tableId;
    private ReservationStatus status;
    private int partySize;
    
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
    
    public Reservation(UUID customerId, LocalDateTime dateTime, int partySize) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.dateTime = dateTime;
        this.partySize = partySize;
        this.status = ReservationStatus.PENDING;
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

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public int getPartySize() {
		return partySize;
	}

	public void setPartySize(int partySize) {
		this.partySize = partySize;
	}
    
}
