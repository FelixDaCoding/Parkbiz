module com.example.parkbiz {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires jdk.management;
    requires java.sql;
    requires mysql.connector.j;

    opens com.example.parkbiz to javafx.fxml;
    exports com.example.parkbiz;


}