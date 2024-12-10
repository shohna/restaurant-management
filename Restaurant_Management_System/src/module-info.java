module Restaurant_Management_System {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens application to javafx.fxml;  // Allow JavaFX to access the application's package

    exports application;
}
