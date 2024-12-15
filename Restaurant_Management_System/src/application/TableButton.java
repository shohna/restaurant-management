package application;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class TableButton extends Button {
    private boolean isOccupied;
    private String assignedServer;
    private int numGuests;
    private boolean isSelected;

    public TableButton(String text) {
        super(text);
        this.isOccupied = false;
        this.assignedServer = null;
        this.numGuests = 0;
        this.isSelected = false;
        
        // Set initial style
        updateStyle();
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateStyle();
    }

    public boolean isSelected() {
        return isSelected;
    }
    
    private void updateStyle() {
        getStyleClass().removeAll("table-button-occupied", "table-button-available", "table-button-selected");
        
        if (isSelected) {
            getStyleClass().add("table-button-selected");
        }
        if (isOccupied) {
            getStyleClass().add("table-button-occupied");
        } else {
            getStyleClass().add("table-button-available");
        }
    }

    public void assignTable(String server, int guests) {
        this.isOccupied = true;
        this.assignedServer = server;
        this.numGuests = guests;
        updateStyle();
        updateTooltip();
    }

    public void clearTable() {
        this.isOccupied = false;
        this.assignedServer = null;
        this.numGuests = 0;
        updateStyle();
        updateTooltip();
    }

    private void updateTooltip() {
        String tooltipText = isOccupied ? 
            String.format("Server: %s\nGuests: %d", assignedServer, numGuests) :
            "Available";
        setTooltip(new Tooltip(tooltipText));
    }

    // Getters for table status
    public boolean isOccupied() {
        return isOccupied;
    }

    public String getAssignedServer() {
        return assignedServer;
    }

    public int getNumGuests() {
        return numGuests;
    }
}