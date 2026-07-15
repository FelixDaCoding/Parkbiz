module com.example.parkbiz {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.parkbiz to javafx.fxml; // This is the crucial line
    exports com.example.parkbiz;



}