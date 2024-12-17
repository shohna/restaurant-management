package application;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class TableButton extends Button {
    private int id;  // Table ID as int
    private boolean isOccupied;
    private String assignedServer;
    private int numGuests;
    private boolean isSelected;

    public TableButton(int id, String text) {
        super(text);  // Sets the button text
        this.id = id; // Assigns the table ID
        this.isOccupied = false;
        this.isSelected = false;

        updateStyle();
        updateTooltip();
    }

    /** Renamed method to avoid conflict with Node.getId() */
    public int getTableId() { // Return id as int
        return id;
    }

    /** Toggles the selection state of the table */
    private void toggleSelection() {
        this.isSelected = !this.isSelected;
        updateStyle();
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateStyle();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void assignTable(String server, int guests) {
        this.isOccupied = true;
        this.assignedServer = server;
        this.numGuests = guests;
        this.isSelected = false;
        updateStyle();
        updateTooltip();
    }

    public void clearTable() {
        this.isOccupied = false;
        this.assignedServer = null;
        this.numGuests = 0;
        this.isSelected = false;
        updateStyle();
        updateTooltip();
    }

    private void updateStyle() {
        getStyleClass().removeAll("table-button-occupied", "table-button-available", "table-button-selected");

        if (isSelected) {
            getStyleClass().add("table-button-selected");
        } else if (isOccupied) {
            getStyleClass().add("table-button-occupied");
        } else {
            getStyleClass().add("table-button-available"); // Same style for all available tables
        }
    }


    private void updateTooltip() {
        String tooltipText = isOccupied
                ? String.format("Server: %s\nGuests: %d", assignedServer, numGuests)
                : "Available";
        setTooltip(new Tooltip(tooltipText));
    }

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
