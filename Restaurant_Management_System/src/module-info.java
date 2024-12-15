module Restaurant_Management_System {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    opens application to javafx.fxml;
    opens resources to javafx.fxml;
    exports application;
}