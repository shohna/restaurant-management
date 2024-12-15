package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.time.LocalDate;
import javafx.collections.FXCollections;

public class CustomerController {

	@FXML
	public void showMenu() {
	    Dialog<String> dialog = new Dialog<>();
	    dialog.setTitle("Our Menu");
	    
	    // Create main content VBox
	    VBox content = new VBox(10);
	    content.getStyleClass().add("menu-content");
	    
	    // Add categories
	    content.getChildren().addAll(
	        createMenuCategory("Appetizers", new String[][]{
	            {"Classic Bruschetta", "Fresh tomatoes, garlic, basil on toasted bread", "$12"},
	            {"Crispy Calamari", "Served with marinara sauce and lemon", "$16"},
	            {"Garden Salad", "Mixed greens, cherry tomatoes, cucumber", "$10"}
	        }),
	        new Separator(),
	        createMenuCategory("Main Course", new String[][]{
	            {"Grilled Salmon", "Fresh Atlantic salmon with seasonal vegetables", "$28"},
	            {"Beef Tenderloin", "8oz with red wine reduction sauce", "$34"},
	            {"Mushroom Risotto", "Arborio rice, wild mushrooms, parmesan", "$24"}
	        }),
	        new Separator(),
	        createMenuCategory("Desserts", new String[][]{
	            {"Tiramisu", "Classic Italian coffee-flavored dessert", "$10"},
	            {"Chocolate Lava Cake", "Warm chocolate cake with vanilla ice cream", "$12"},
	            {"Crème Brûlée", "Classic French vanilla custard", "$9"}
	        })
	    );

	    ScrollPane scrollPane = new ScrollPane(content);
	    scrollPane.setFitToWidth(true);
	    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
	    scrollPane.getStyleClass().add("menu-scroll-pane");

	    DialogPane dialogPane = dialog.getDialogPane();
	    dialogPane.setContent(scrollPane);
	    dialogPane.getStyleClass().add("menu-dialog");
	    dialogPane.getButtonTypes().add(ButtonType.CLOSE);

	    dialog.showAndWait();
	}

	private VBox createMenuCategory(String categoryName, String[][] items) {
	    VBox category = new VBox(5);
	    
	    // Category header
	    Label header = new Label(categoryName);
	    header.getStyleClass().add("category-header");
	    category.getChildren().add(header);
	    
	    // Menu items
	    for (String[] item : items) {
	        VBox itemBox = new VBox(2);
	        itemBox.getStyleClass().add("menu-item");
	        
	        HBox namePrice = new HBox();
	        namePrice.setAlignment(Pos.CENTER_LEFT);
	        
	        Label nameLabel = new Label(item[0]);
	        nameLabel.getStyleClass().add("item-name");
	        Region spacer = new Region();
	        HBox.setHgrow(spacer, Priority.ALWAYS);
	        Label priceLabel = new Label(item[2]);
	        priceLabel.getStyleClass().add("item-price");
	        
	        namePrice.getChildren().addAll(nameLabel, spacer, priceLabel);
	        
	        Label descLabel = new Label(item[1]);
	        descLabel.getStyleClass().add("item-description");
	        
	        itemBox.getChildren().addAll(namePrice, descLabel);
	        category.getChildren().add(itemBox);
	    }
	    
	    return category;
	}
    
    
    @FXML
    public void showReservationDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Make a Reservation");
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("custom-dialog");
        
        // Create main content
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.getStyleClass().add("reservation-content");
        
        // Header
        Label header = new Label("Reserve Your Table");
        header.getStyleClass().add("dialog-header");
        
        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.getStyleClass().add("reservation-grid");
        
        // Add form controls with labels
        addFormField(grid, "Name", createStyledTextField("Enter your name"), 0);
        addFormField(grid, "Email", createStyledTextField("Enter your email"), 1);
        addFormField(grid, "Phone", createStyledTextField("Enter your phone number"), 2);
        
        // Date picker with custom styling
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("custom-date-picker");
        addFormField(grid, "Date", datePicker, 3);
        
        // Time combo box
        ComboBox<String> timeComboBox = new ComboBox<>(FXCollections.observableArrayList(
            "5:00 PM", "5:30 PM", "6:00 PM", "6:30 PM", "7:00 PM", "7:30 PM", "8:00 PM", "8:30 PM", "9:00 PM"
        ));
        timeComboBox.setPromptText("Select time");
        timeComboBox.getStyleClass().add("custom-combo-box");
        addFormField(grid, "Time", timeComboBox, 4);
        
        // Guest count spinner
        Spinner<Integer> guestSpinner = new Spinner<>(1, 10, 2);
        guestSpinner.getStyleClass().add("custom-spinner");
        addFormField(grid, "Guests", guestSpinner, 5);
        
        // Special requests
        TextArea specialRequests = new TextArea();
        specialRequests.setPromptText("Any special requests or dietary requirements?");
        specialRequests.setPrefRowCount(3);
        specialRequests.getStyleClass().add("custom-textarea");
        addFormField(grid, "Special Requests", specialRequests, 6);
        
        content.getChildren().addAll(header, grid);
        dialogPane.setContent(content);
        
        // Add buttons
        ButtonType reserveButtonType = new ButtonType("Reserve", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(reserveButtonType, ButtonType.CANCEL);
        
        // Style buttons
        Button reserveButton = (Button) dialogPane.lookupButton(reserveButtonType);
        reserveButton.getStyleClass().add("dialog-button-primary");
        
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-secondary");
        
        dialog.showAndWait();
    }

    private void addFormField(GridPane grid, String labelText, Control field, int row) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        GridPane.setFillWidth(field, true);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("custom-text-field");
        return field;
    }

//    // Helper class for menu items
//    private static class MenuItem {
//        private final String name;
//        private final String description;
//        private final String price;
//
//        public MenuItem(String name, String description, String price) {
//            this.name = name;
//            this.description = description;
//            this.price = price;
//        }
//
////        public String getName() { return name; }
////        public String getDescription() { return description; }
////        public String getPrice() { return price; }
//    }
}