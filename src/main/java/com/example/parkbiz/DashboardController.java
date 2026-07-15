package com.example.parkbiz;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private FlowPane parkingGrid;
    @FXML private Label lblActiveSlot, lblLiveFee, lblStatusMessage, lblCountdown;
    @FXML private TextField txtHours;
    @FXML private StackPane miniFan;
    @FXML private Button btnAction, btnCancel; // Added btnCancel

    private String selectedSlotId = null;
    private final ParkingSession sessionLogic = new ParkingSession();
    private Timeline countdownTimeline;
    private int totalSecondsRemaining;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startFanAnimation();
        loadDummySlots();
        txtHours.textProperty().addListener((obs, old, newValue) -> calculateFeePreview(newValue));
    }

    private void calculateFeePreview(String hoursText) {
        try {
            if (hoursText == null || hoursText.isEmpty()) {
                lblLiveFee.setText("TOTAL: $0.00");
                return;
            }
            int hours = Integer.parseInt(hoursText);
            lblLiveFee.setText(String.format("TOTAL: $%.2f", sessionLogic.calculateFee(hours)));
        } catch (NumberFormatException e) {
            lblLiveFee.setText("ERR: INVALID");
        }
    }

    private void loadDummySlots() {
        parkingGrid.getChildren().clear();
        for (int i = 1; i <= 12; i++) {
            String slotId = "SLOT-" + String.format("%02d", i);
            Button slotBtn = new Button(slotId + "\n[FREE]");
            slotBtn.setPrefSize(120, 80);
            slotBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff8c00; -fx-border-color: #ff8c00; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");
            slotBtn.setOnAction(e -> selectSlot(slotId, slotBtn));
            parkingGrid.getChildren().add(slotBtn);
        }
    }

    private void selectSlot(String id, Button btn) {
        if (countdownTimeline != null && countdownTimeline.getStatus() == Animation.Status.RUNNING) return;
        selectedSlotId = id;
        lblActiveSlot.setText("SLOT: " + id);
        lblStatusMessage.setText("> " + id + " SELECTED.");
        loadDummySlots();
        btn.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-font-family: 'Monospaced';");
    }

    @FXML
    private void handlePrimaryAction() {
        if (selectedSlotId == null) return;
        try {
            int hours = Integer.parseInt(txtHours.getText());
            lblStatusMessage.setText("> PARKING SUCCESSFUL. SENSORS ACTIVE.");
            startCountdown(hours);
            btnAction.setDisable(true);
            btnCancel.setDisable(false); // Enable Cancel when parked
            txtHours.setDisable(true);
            parkingGrid.setDisable(true);
        } catch (Exception e) {
            lblStatusMessage.setText("> ERROR: INVALID INPUT.");
        }
    }

    @FXML
    private void handleCancelSession() {
        // 1. Create the alert as a basic Confirmation type
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        // 2. Remove the "?" Icon and the Header Box
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED); // Removes the window title bar
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // 3. Set the Warning Text
        alert.setTitle("CRITICAL_WARNING");
        alert.setContentText(">> WARNING: PARKING SPACE RESERVATION <<\n\n" +
                "TERMINATING THIS SESSION WILL FORFEIT ALL REMAINING TIME.\n" +
                "CREDITS ARE NON-REFUNDABLE.\n\n" +
                "PROCEED WITH TERMINATION?");

        // 4. Access the DialogPane to style it
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #000000; -fx-border-color: #ff4500; -fx-border-width: 3;");

        // Style the content text
        dialogPane.lookupAll(".label").forEach(node -> {
            node.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
        });

        // 5. Style the OK and CANCEL buttons
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        String btnStyle = "-fx-background-color: #000000; " +
                "-fx-text-fill: #ff8c00; " +
                "-fx-border-color: #ff8c00; " +
                "-fx-border-radius: 0; " +
                "-fx-font-family: 'Monospaced'; " +
                "-fx-cursor: hand;";

        okButton.setStyle(btnStyle);
        okButton.setText("[ YES TERMINATE ]");

        cancelButton.setStyle(btnStyle);
        cancelButton.setText("[ NO ABORT ]");

        // 6. Logic handling
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            lblStatusMessage.setText("> SESSION TERMINATED. SLOT " + selectedSlotId + " RELEASED.");
            if (countdownTimeline != null) countdownTimeline.stop();
            resetUI();
        } else {
            lblStatusMessage.setText("> TERMINATION ABORTED. SESSION CONTINUES.");
        }
    }

    private void startCountdown(int hours) {
        totalSecondsRemaining = hours * 3600;
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            totalSecondsRemaining--;
            if (totalSecondsRemaining <= 0) {
                countdownTimeline.stop();
                lblStatusMessage.setText("> SESSION EXPIRED.");
                resetUI();
            } else {
                updateTimerLabel();
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateTimerLabel() {
        int h = totalSecondsRemaining / 3600;
        int m = (totalSecondsRemaining % 3600) / 60;
        int s = totalSecondsRemaining % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void resetUI() {
        selectedSlotId = null;
        txtHours.clear();
        btnAction.setDisable(false);
        btnCancel.setDisable(true); // Disable again
        txtHours.setDisable(false);
        parkingGrid.setDisable(false);
        lblActiveSlot.setText("SLOT: NONE");
        lblLiveFee.setText("TOTAL: $0.00");
        lblCountdown.setText("00:00:00");
        loadDummySlots();
    }

    private void startFanAnimation() {
        RotateTransition rt = new RotateTransition(Duration.millis(1500), miniFan);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt.play();
    }

    @FXML
    private void handleLogout() throws IOException {
        if (countdownTimeline != null) countdownTimeline.stop();
        Stage stage = (Stage) btnAction.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(root));
    }
}