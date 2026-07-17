package com.example.parkbiz;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // FXML Bindings
    @FXML private FlowPane parkingGrid;
    @FXML private Label lblActiveSlot, lblLiveFee, lblStatusMessage, lblCountdown;
    @FXML private TextField txtHours;
    @FXML private Button btnAction, btnCancel;
    @FXML private StackPane miniFan;

    // Shared Data and Logic
    private ParkingRegistry registry = ParkingRegistry.getInstance();
    private int selectedSlotId = -1;
    private Timeline countdownTimeline;
    private long totalSecondsRemaining;
    private Stage currentStage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Get reference to the stage for session management
        javafx.application.Platform.runLater(() -> {
            try {
                currentStage = (Stage) miniFan.getScene().getWindow();
                // Save session when window is closed (not logged out)
                currentStage.setOnCloseRequest(e -> {
                    saveCurrentSession();
                });
            } catch (Exception e) {
                System.err.println("Could not initialize session manager: " + e.getMessage());
            }
        });

        startFanAnimation();
        loadSlotsFromRegistry();

        // RESTORE SESSION ON INITIALIZATION IF REGISTERED
        restoreActiveSessionIfAny();

        // Live Fee Calculator: $50.00 per hour
        txtHours.textProperty().addListener((obs, old, val) -> {
            try {
                if (val == null || val.isEmpty()) {
                    lblLiveFee.setText("TOTAL: $0.00");
                    return;
                }
                int h = Integer.parseInt(val);
                lblLiveFee.setText(String.format("TOTAL: $%.2f", h * 50.00));
            } catch (Exception e) {
                lblLiveFee.setText("ERR: INVALID");
            }
        });
    }

    /**
     * Saves the current session so user can resume after reopening the app
     */
    private void saveCurrentSession() {
        try {
            int userId = UserSession.getInstance().getUserId();
            String username = UserSession.getInstance().getUsername();
            if (userId > 0 && username != null) {
                SessionManager.getInstance().saveSession(userId, username, "DRIVER", "dashboard-view.fxml");
            }
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    /**
     * Checks if the currently logged-in user has an active parking slot,
     * and restores their countdown and UI locks if a session is running.
     */
    private void restoreActiveSessionIfAny() {
        try {
            int currentUserId = UserSession.getInstance().getUserId();
            ParkingRegistry.ActiveSession session = registry.getActiveSessionForUser(currentUserId);

            if (session != null) {
                selectedSlotId = session.slotId;
                lblActiveSlot.setText("SLOT: " + session.slotLabel);
                lblLiveFee.setText(String.format("TOTAL: $%.2f", session.totalFee));
                txtHours.setText(String.valueOf(session.durationHours));
                lblStatusMessage.setText("> RESTORED ACTIVE PARKING SESSION.");

                // Re-establish component locks
                btnAction.setDisable(true);
                btnCancel.setDisable(false);
                txtHours.setDisable(true);
                parkingGrid.setDisable(true);

                // Start UI countdown tracking
                startCountdownWithSeconds(session.secondsRemaining);
            }
        } catch (SQLException e) {
            lblStatusMessage.setText("> ERR: SESSION_RECOVERY_FAILED");
            e.printStackTrace();
        }
    }

    private void loadSlotsFromRegistry() {
        parkingGrid.getChildren().clear();
        try {
            Map<Integer, AdminController.ParkingSlot> currentSlots = registry.getLiveSlots();

            currentSlots.forEach((id, slot) -> {
                Button b = new Button("SLOT " + slot.label + "\n" +
                        (slot.occupied ? "[BUSY]" : "[FREE]"));
                b.setPrefSize(110, 70);

                if (slot.occupied) {
                    b.setStyle("-fx-background-color: #221100; -fx-text-fill: #663300; -fx-border-color: #663300; -fx-font-family: 'Monospaced';");
                    b.setDisable(true);
                } else if (id == selectedSlotId) {
                    b.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-border-color: #ff8c00; -fx-font-family: 'Monospaced';");
                } else {
                    b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff8c00; -fx-border-color: #ff8c00; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");
                }

                b.setOnAction(e -> {
                    selectedSlotId = id;
                    lblActiveSlot.setText("SLOT: " + slot.label);
                    lblStatusMessage.setText("> SLOT_" + slot.label + " SELECTED.");
                    loadSlotsFromRegistry();
                });
                parkingGrid.getChildren().add(b);
            });
        } catch (SQLException e) {
            lblStatusMessage.setText("> ERR: DB_FETCH_FAILED");
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrimaryAction() {
        if (selectedSlotId == -1) return;
        try {
            int hours = Integer.parseInt(txtHours.getText());

            // Input validation
            if (hours < 1 || hours > 24) {
                lblStatusMessage.setText("> ERR: HOURS_MUST_BE_BETWEEN_1_AND_24");
                return;
            }

            double fee = hours * 50.00;
            String plate = "DRV-" + (int)(Math.random()*9000+1000);

            // PERSIST TO DATABASE via Registry
            registry.reserveSlot(
                    UserSession.getInstance().getUserId(),
                    selectedSlotId,
                    hours,
                    fee,
                    plate
            );

            lblStatusMessage.setText("> SYNCING WITH DATABASE . . . [OK]");

            // Mechanical delay effect before starting timer
            new Timeline(new KeyFrame(Duration.millis(1000), e -> {
                lblStatusMessage.setText("> PARKING SUCCESSFUL. SLOT LOCKED.");
                startCountdown(hours);
                loadSlotsFromRegistry();
                btnAction.setDisable(true);
                btnCancel.setDisable(false);
                txtHours.setDisable(true);
                parkingGrid.setDisable(true);
            })).play();

        } catch (Exception e) {
            lblStatusMessage.setText("> ERR: TRANSACTION_REJECTED");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelSession() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(">> WARNING: PRIVATE_SPACE_RESERVATION <<\n\n" +
                "TERMINATING THIS SESSION WILL FORFEIT ALL REMAINING TIME.\n" +
                "CREDITS ARE NON-REFUNDABLE.\n\n" +
                "PROCEED WITH TERMINATION?");

        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: #000000; -fx-border-color: #ff4500; -fx-border-width: 3;");
        dp.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;"));

        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) dp.lookupButton(ButtonType.CANCEL);

        String style = "-fx-background-color: black; -fx-text-fill: #ff8c00; -fx-border-color: #ff8c00; -fx-font-family: 'Monospaced';";
        okBtn.setStyle(style); okBtn.setText("[ YES_TERMINATE ]");
        cancelBtn.setStyle(style); cancelBtn.setText("[ NO_ABORT ]");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (countdownTimeline != null) countdownTimeline.stop();

            // VACATE THE SLOT AND DELETE RESERVATION
            if (selectedSlotId != -1) {
                try {
                    // First delete the reservation for this slot and user
                    registry.deleteReservationForSlot(selectedSlotId, UserSession.getInstance().getUserId());
                    // Then vacate the slot
                    registry.vacateSlotInDatabase(selectedSlotId);
                    lblStatusMessage.setText("> SESSION TERMINATED. SPACE RELEASED.");
                } catch (SQLException e) {
                    lblStatusMessage.setText("> ERR: DATABASE_VACATION_FAILED");
                    e.printStackTrace();
                }
            }

            resetDriverUI();
        }
    }

    private void startCountdown(int hours) {
        startCountdownWithSeconds((long) hours * 3600);
    }

    /**
     * Initializes and starts the real-time UI countdown timeline using seconds.
     */
    private void startCountdownWithSeconds(long seconds) {
        totalSecondsRemaining = seconds;
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            totalSecondsRemaining--;
            if (totalSecondsRemaining <= 0) {
                countdownTimeline.stop();
                lblStatusMessage.setText("> SESSION EXPIRED. SPACE RELEASED.");

                // Vacate the slot when timer expires
                try {
                    if (selectedSlotId != -1) {
                        registry.vacateSlotInDatabase(selectedSlotId);
                    }
                } catch (SQLException ex) {
                    lblStatusMessage.setText("> ERR: AUTO_VACATION_FAILED");
                    ex.printStackTrace();
                }

                resetDriverUI();
            } else {
                updateTimerLabel();
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateTimerLabel() {
        long h = totalSecondsRemaining / 3600;
        long m = (totalSecondsRemaining % 3600) / 60;
        long s = totalSecondsRemaining % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void resetDriverUI() {
        selectedSlotId = -1;
        txtHours.clear();
        btnAction.setDisable(false);
        btnCancel.setDisable(true);
        txtHours.setDisable(false);
        parkingGrid.setDisable(false);
        lblActiveSlot.setText("SLOT: NONE");
        lblLiveFee.setText("TOTAL: $0.00");
        lblCountdown.setText("00:00:00");
        loadSlotsFromRegistry();
    }

    private void startFanAnimation() {
        if (miniFan != null) {
            RotateTransition rt = new RotateTransition(Duration.millis(1500), miniFan);
            rt.setByAngle(360);
            rt.setCycleCount(Animation.INDEFINITE);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        // Stop the countdown if running
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // Clear the session
        SessionManager.getInstance().clearSession();

        // Go to login screen
        Stage s = (Stage) btnAction.getScene().getWindow();
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("login-view.fxml")));
        s.setScene(scene);
        s.setTitle("ParkBiz - Business Login");
        s.centerOnScreen();
        s.show();
    }
}