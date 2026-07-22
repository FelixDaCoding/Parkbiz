package com.example.parkbiz;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminController implements Initializable {

    // These MUST match fx:id in FXML exactly
    @FXML private Label lblClock, lblCpu, lblRam, lblDbStatus, lblOccupancy, lblSlotDisplay, lblMainframeStatus;
    @FXML private TextArea txtTerminal;
    @FXML private Button btnLogout, btnReport, btnAdd, btnDelete, btnReset, btnCheck;

    // Stage reference for session management
    private Stage currentStage;

    // --- DATA MODEL ---
    public static class ParkingSlot {
        int id;
        String label;
        boolean occupied;
        boolean sensorOnline;
        String plate;
        long secondsRemaining;

        public ParkingSlot(int id, String label, boolean occupied, String plate, long seconds) {
            this.id = id;
            this.label = label;
            this.occupied = occupied;
            this.plate = plate;
            this.secondsRemaining = seconds;
            this.sensorOnline = true;
        }
    }

    private Map<Integer, ParkingSlot> slotMap;
    private Map<Integer, Button> liveMapButtons = new HashMap<>();
    private int selectedSlot = 1;
    private final com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ParkingRegistry registry = ParkingRegistry.getInstance();
    private ParkingSlot currentlyInspectedSlot;
    private Label popSlotNum, popPlate, popTimer;

    // CSS Themes
    private final String AMBER_BOX = "-fx-background-color: transparent; -fx-border-color: #ff8c00; -fx-border-width: 1; -fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced';";
    private final String AMBER_SOLID = "-fx-background-color: #ff8c00; -fx-border-color: #ff8c00; -fx-border-width: 1; -fx-text-fill: black; -fx-font-family: 'Monospaced';";
    private final String RED_BOX = "-fx-background-color: transparent; -fx-border-color: #ff4500; -fx-border-width: 1; -fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';";
    private final String RED_SOLID = "-fx-background-color: #ff4500; -fx-border-color: #ff4500; -fx-border-width: 1; -fx-text-fill: white; -fx-font-family: 'Monospaced';";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check if session is valid - redirect to login if not
        if (!SessionManager.getInstance().isLoggedIn()) {
            try {
                redirectToLogin();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Use Platform.runLater to get the stage after scene is ready
        Platform.runLater(() -> {
            try {
                currentStage = (Stage) lblClock.getScene().getWindow();
                // Save session when window is closed (not logged out)
                currentStage.setOnCloseRequest(e -> {
                    saveCurrentSession();
                });
                writeToLog("> SESSION_MANAGER_INITIALIZED . . . [OK]");
            } catch (Exception e) {
                System.err.println("Could not initialize session manager: " + e.getMessage());
            }
        });

        // Clean orphaned reservations on startup
        try {
            registry.clearOrphanedReservations();
            writeToLog("> ORPHANED_RESERVATIONS_CLEANED . . . [OK]");
        } catch (SQLException e) {
            writeToLog("> ERR: ORPHANED_CLEAN_FAILED");
            e.printStackTrace();
        }

        syncWithDatabase();
        startSystemTimelines();
        updateSlotLabel();
        writeToLog("> BOOT_SEQUENCE_COMPLETE . . . [OK]");

        // Check if session was restored
        if (SessionManager.getInstance().isLoggedIn()) {
            writeToLog("> SESSION_RESTORED . . . [OK]");
        }
    }

    /**
     * Redirects user to login screen if session is invalid
     */
    private void redirectToLogin() throws IOException {
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) lblClock.getScene().getWindow();
                Scene scene = new Scene(FXMLLoader.load(getClass().getResource("login-view.fxml")));
                stage.setScene(scene);
                stage.setTitle("ParkBiz - Business Login");
                stage.centerOnScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Saves the current session so user can resume after reopening the app
     */
    private void saveCurrentSession() {
        try {
            // Get from SessionManager directly, not UserSession
            if (SessionManager.getInstance().isLoggedIn()) {
                SessionManager.getInstance().saveSession(
                        SessionManager.getInstance().getUserId(),
                        SessionManager.getInstance().getUsername(),
                        "ADMIN",
                        "admin-view.fxml"
                );
                System.out.println("Admin session saved on close");
            }
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    private void syncWithDatabase() {
        try {
            this.slotMap = registry.getLiveSlots();
            updateOccupancyDisplay();
            if (currentlyInspectedSlot != null) {
                currentlyInspectedSlot = slotMap.get(currentlyInspectedSlot.id);
            }
            // Update DB status label
            lblDbStatus.setText("DB CONN: ONLINE [OK]");
            lblDbStatus.setStyle("-fx-text-fill: #00ff00; -fx-font-family: 'Monospaced';");
        } catch (SQLException e) {
            writeToLog("> ERR: DATABASE_SYNC_FAILED");
            // Show DB offline status
            lblDbStatus.setText("DB CONN: OFFLINE [FAIL]");
            lblDbStatus.setStyle("-fx-text-fill: #ff0000; -fx-font-family: 'Monospaced';");
        }
    }

    private void updateMapButtonDisplay(Button b, ParkingSlot slot) {
        if (!slot.sensorOnline) {
            b.setText("SLOT " + slot.label + "\nOFFLINE");
            b.setStyle("-fx-background-color: #440000; -fx-text-fill: #ff4500; -fx-border-color: #ff4500; -fx-font-family: 'Monospaced';");
        } else if (slot.occupied) {
            long s = slot.secondsRemaining;
            String timer = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
            b.setText("SLOT " + slot.label + "\n" + timer);
            b.setStyle(AMBER_SOLID + "-fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            b.setText("SLOT " + slot.label + "\nVACANT");
            b.setStyle(AMBER_BOX + "-fx-border-style: dashed;");
        }
    }

    private void startSystemTimelines() {
        Timeline global = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblClock.setText("SYSTEM TIME: " + LocalDateTime.now().format(timeFormat) + " [STABLE]");
            syncWithDatabase();
            if (!liveMapButtons.isEmpty()) {
                for (Map.Entry<Integer, Button> entry : liveMapButtons.entrySet()) {
                    ParkingSlot s = slotMap.get(entry.getKey());
                    if (s != null) updateMapButtonDisplay(entry.getValue(), s);
                }
            }
            if (currentlyInspectedSlot != null) updatePopupDetails();
        }));
        global.setCycleCount(Animation.INDEFINITE);
        global.play();

        Timeline hw = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            double cpu = osBean.getSystemCpuLoad();
            lblCpu.setText(String.format("CPU LOAD: %d%% [%s]", (int) (cpu * 100), getBar(cpu, 10)));
            long t = osBean.getTotalPhysicalMemorySize();
            long f = osBean.getFreePhysicalMemorySize();
            lblRam.setText(String.format("RAM UTIL: %.1f/%.1f GB [%s]", (t - f) / 1e9, t / 1e9, getBar((double) (t - f) / t, 10)));
        }));
        hw.setCycleCount(Animation.INDEFINITE);
        hw.play();
    }

    @FXML
    private void handleReport() {
        animateLogWithBuffer("PULLING LOT TELEMETRY", "[SUCCESS]", 0);
        Timeline reportDataTimeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            long occupiedCount = slotMap.values().stream().filter(s -> s.occupied).count();
            writeToLog("------------------------------------------");
            writeToLog("DATA_STREAM: PARKING_SUMMARY_v1.0");
            writeToLog("TOTAL_NODES      : " + slotMap.size());
            writeToLog("ACTIVE_SESSIONS  : " + occupiedCount);
            writeToLog("EST_REVENUE_24H  : $" + String.format("%.2f", occupiedCount * ParkingRegistry.HOURLY_RATE));
            writeToLog("------------------------------------------");
        }));
        reportDataTimeline.play();
    }

    @FXML
    private void handleAddSlot() {
        try {
            int nid = slotMap.keySet().stream().max(Integer::compare).orElse(0) + 1;
            registry.addSlotToDatabase(nid);
            syncWithDatabase();
            writeToLog("INITIALIZING NODE_" + slotMap.get(nid).label + " . . . [OK]");
        } catch (SQLException e) {
            writeToLog("> ERR: SQL_INSERT_REJECTED");
        }
    }

    @FXML
    private void handleDeleteSlot() {
        try {
            if (slotMap.size() > 1) {
                int lid = slotMap.keySet().stream().max(Integer::compare).get();
                String label = slotMap.get(lid).label;
                registry.deleteSlotFromDatabase(lid);
                syncWithDatabase();
                if (selectedSlot > slotMap.size()) {
                    selectedSlot = slotMap.keySet().stream().max(Integer::compare).get();
                    updateSlotLabel();
                }
                writeToLog("DECOMMISSIONING NODE_" + label + " . . . [OFFLINE]");
            }
        } catch (SQLException e) {
            writeToLog("> ERR: SQL_DELETE_FAILED");
        }
    }

    @FXML
    private void handleReset() {
        btnReset.setDisable(true);
        txtTerminal.clear();
        writeToLog("> INITIATING SYSTEM-WIDE DIAGNOSTICS...");

        double currentDelay = 500;
        for (ParkingSlot slot : slotMap.values()) {
            slot.sensorOnline = true;
            // Update database sensor health
            try {
                registry.updateSensorHealth(slot.id, true);
            } catch (SQLException ex) {
                writeToLog("> ERR: SENSOR_UPDATE_FAILED for " + slot.label);
            }
            animateLogWithBuffer("PINGING SENSOR_" + slot.label, "[ONLINE]", currentDelay);
            currentDelay += 900;
        }

        Timeline finish = new Timeline(new KeyFrame(Duration.millis(currentDelay + 200), e -> {
            writeToLog("> ALL NODES RESPONDING. SYSTEM NOMINAL.");
            btnReset.setDisable(false);
        }));
        finish.play();
    }

    @FXML
    private void handleLookup() {
        ParkingSlot s = slotMap.get(selectedSlot);
        if (s == null) return;
        animateLogWithBuffer("QUERYING SLOT_" + s.label + " HARDWARE", s.sensorOnline ? "[OK]" : "[FAIL]", 0);
        Timeline res = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            if (!s.sensorOnline) writeToLog("> STATUS: [SIGNAL_LOST] - NODE_" + s.label + " INTERRUPT.");
            else if (!s.occupied) writeToLog("> STATUS: [VACANT] - GRID_COORD_" + s.label + " CLEAR.");
            else writeToLog("> STATUS: [OCCUPIED] - VEHICLE: " + s.plate + " | TTL: " + s.secondsRemaining + "s");
        }));
        res.play();
    }

    @FXML
    private void handleViewLiveMap() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        HBox root = new HBox(20);
        root.setStyle("-fx-background-color: #000000; -fx-border-color: #ff8c00; -fx-border-width: 3; -fx-padding: 20;");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        liveMapButtons.clear();

        for (ParkingSlot slot : slotMap.values()) {
            Button b = new Button();
            b.setPrefSize(110, 75);
            updateMapButtonDisplay(b, slot);
            liveMapButtons.put(slot.id, b);
            b.setOnAction(e -> {
                currentlyInspectedSlot = slot;
                selectedSlot = slot.id;
                updateSlotLabel();
                updatePopupDetails();
            });
            grid.add(b, (slot.id - 1) % 4, (slot.id - 1) / 4);
        }

        VBox details = new VBox(15);
        details.setPrefWidth(260);
        Label head = new Label("[ SLOT_DIAGNOSTICS ]");
        head.setStyle("-fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
        popSlotNum = new Label("SLOT COORD: --");
        popPlate = new Label("OCCUPANT ID: [--]");
        popTimer = new Label("TIME REMAINING: 00:00:00");
        String ds = "-fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced'; -fx-font-size: 12;";
        popSlotNum.setStyle(ds);
        popPlate.setStyle(ds);
        popTimer.setStyle(ds);
        Button close = new Button("[ CLOSE_TERMINAL ]");
        close.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");
        close.setOnAction(e -> {
            currentlyInspectedSlot = null;
            liveMapButtons.clear();
            popup.close();
        });
        details.getChildren().addAll(head, popSlotNum, popPlate, popTimer, new Separator(), close);

        ScrollPane sp = new ScrollPane(grid);
        sp.setStyle("-fx-background:black; -fx-background-color:black; -fx-border-color:transparent;");
        sp.setPrefHeight(350);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getChildren().addAll(sp, details);
        popup.setScene(new Scene(root));
        popup.show();
    }

    // --- HELPER LOGIC ---
    private void animateLogWithBuffer(String baseMsg, String finalResult, double initialDelayMs) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(initialDelayMs), e -> txtTerminal.appendText("[" + LocalDateTime.now().format(timeFormat) + "] >> " + baseMsg)),
                new KeyFrame(Duration.millis(initialDelayMs + 200), e -> txtTerminal.appendText(" .")),
                new KeyFrame(Duration.millis(initialDelayMs + 400), e -> txtTerminal.appendText(" .")),
                new KeyFrame(Duration.millis(initialDelayMs + 600), e -> txtTerminal.appendText(" . ")),
                new KeyFrame(Duration.millis(initialDelayMs + 800), e -> {
                    txtTerminal.appendText(finalResult + "\n");
                    txtTerminal.setScrollTop(Double.MAX_VALUE);
                })
        );
        timeline.play();
    }

    private void updatePopupDetails() {
        if (currentlyInspectedSlot == null) return;
        // Safety check: popup might not be open
        if (popSlotNum == null || popPlate == null || popTimer == null) return;

        popSlotNum.setText("SLOT COORD: " + currentlyInspectedSlot.label);
        popPlate.setText("OCCUPANT ID: [" + (currentlyInspectedSlot.sensorOnline ? currentlyInspectedSlot.plate : "ERR_NO_DATA") + "]");
        long s = currentlyInspectedSlot.secondsRemaining;
        popTimer.setText(currentlyInspectedSlot.sensorOnline ? String.format("TIME REMAINING: %02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) : "SIGNAL_LOST");
    }

    private void updateOccupancyDisplay() {
        if (slotMap == null || slotMap.isEmpty()) return;
        long occ = slotMap.values().stream().filter(s -> s.occupied).count();
        double p = (double) occ / slotMap.size();
        StringBuilder b = new StringBuilder();
        int f = (int) (p * 16);
        for (int i = 0; i < 16; i++) b.append(i < f ? "█" : "░");
        lblOccupancy.setText(String.format("SLOTS OCCUPIED: %d / %d [%s] %d%%", occ, slotMap.size(), b.toString(), (int) (p * 100)));
    }

    private void updateSlotLabel() {
        ParkingSlot s = slotMap.get(selectedSlot);
        if (s != null) lblSlotDisplay.setText("ZONE_" + s.label);
    }

    private String getBar(double p, int s) {
        int f = (int) (p * s);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s; i++) b.append(i < f ? "|" : ".");
        return b.toString();
    }

    /**
     * WRITE TO LOG - PUBLIC METHOD accessible from anywhere in this class
     */
    public void writeToLog(String m) {
        if (txtTerminal != null) {
            txtTerminal.appendText("[" + LocalDateTime.now().format(timeFormat) + "] > " + m + "\n");
            txtTerminal.setScrollTop(Double.MAX_VALUE);
        }
    }

    @FXML
    private void handleIncrementSlot() {
        List<Integer> keys = new ArrayList<>(slotMap.keySet());
        Collections.sort(keys);
        int currentIndex = keys.indexOf(selectedSlot);
        if (currentIndex < keys.size() - 1) {
            selectedSlot = keys.get(currentIndex + 1);
            updateSlotLabel();
        }
    }

    @FXML
    private void handleDecrementSlot() {
        List<Integer> keys = new ArrayList<>(slotMap.keySet());
        Collections.sort(keys);
        int currentIndex = keys.indexOf(selectedSlot);
        if (currentIndex > 0) {
            selectedSlot = keys.get(currentIndex - 1);
            updateSlotLabel();
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        // Clear session on explicit logout
        SessionManager.getInstance().clearSession();
        Stage s = (Stage) btnLogout.getScene().getWindow();
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("login-view.fxml")));
        s.setScene(scene);
        s.setTitle("ParkBiz - Business Login");
        s.centerOnScreen();
    }

    @FXML
    private void onBtnH(javafx.scene.input.MouseEvent e) {
        Button b = (Button) e.getSource();
        if (b.getText().contains("LOGOUT")) b.setStyle(RED_SOLID);
        else b.setStyle(AMBER_SOLID);
    }

    @FXML
    private void onBtnEx(javafx.scene.input.MouseEvent e) {
        Button b = (Button) e.getSource();
        if (b.getText().contains("LOGOUT")) b.setStyle(RED_BOX);
        else b.setStyle(AMBER_BOX);
    }
}