package application.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        calculateTotal();
    }
    
    private void calculateTotal() {
        this.totalAmount = items.stream()
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
    
    
}