package com.example.parkbiz;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private FlowPane parkingGrid;
    @FXML private Label lblSystemLog;
    @FXML private StackPane miniFan;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startFanAnimation();
        loadDummySlots();
    }

    private void startFanAnimation() {
        RotateTransition rt = new RotateTransition(Duration.millis(1500), miniFan);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt.play();
    }

    private void loadDummySlots() {
        // Create 12 simulated parking slots
        for (int i = 1; i <= 12; i++) {
            String slotId = "SLOT-" + String.format("%02d", i);
            boolean isOccupied = Math.random() > 0.7; // Randomly occupy some for effect

            Button slotBtn = new Button(slotId + "\n" + (isOccupied ? "[OCCUPIED]" : "[FREE]"));
            slotBtn.setPrefSize(120, 80);

            // Retro Styling
            updateSlotStyle(slotBtn, isOccupied);

            slotBtn.setOnAction(e -> handleSlotClick(slotId, isOccupied, slotBtn));
            parkingGrid.getChildren().add(slotBtn);
        }
    }

    private void handleSlotClick(String id, boolean occupied, Button btn) {
        if (occupied) {
            lblSystemLog.setText("> ALERT: " + id + " IS CURRENTLY UNAVAILABLE.");
        } else {
            lblSystemLog.setText("> SUCCESS: " + id + " RESERVED. PROCEED TO GATE.");
            updateSlotStyle(btn, true); // Simulate reservation
        }
    }

    private void updateSlotStyle(Button btn, boolean occupied) {
        if (occupied) {
            btn.setStyle("-fx-background-color: #221100; -fx-text-fill: #663300; -fx-border-color: #663300; -fx-font-family: 'Monospaced';");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff8c00; -fx-border-color: #ff8c00; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        // Back to Login Screen
        Stage stage = (Stage) parkingGrid.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(root));
    }
}
