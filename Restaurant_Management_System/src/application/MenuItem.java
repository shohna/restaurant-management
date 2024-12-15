package application;

import javafx.beans.property.*;

public class MenuItem {
    private final SimpleStringProperty name;
    private final SimpleStringProperty price;
    private final SimpleStringProperty description;

    public MenuItem(String name, String price, String description) {
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleStringProperty(price);
        this.description = new SimpleStringProperty(description);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getPrice() {
        return price.get();
    }

    public void setPrice(String price) {
        this.price.set(price);
    }

    public StringProperty priceProperty() {
        return price;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s - $%s\n%s", getName(), getPrice(), getDescription());
    }
}