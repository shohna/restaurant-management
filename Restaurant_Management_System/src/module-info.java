module Restaurant_Management_System {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;  

    opens application to javafx.fxml;
<<<<<<< HEAD
    opens resources to javafx.fxml;
=======
    opens resources to javafx.fxml; // Add this line
    
>>>>>>> parent of 8ed796e (Added class files)
    exports application;
}