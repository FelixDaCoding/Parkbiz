package com.example.parkbiz;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private Label lblClock, lblCpu, lblRam, lblDbStatus, lblOccupancy, lblSlotDisplay, lblMainframeStatus;
    @FXML private TextArea txtTerminal;
    @FXML private Button btnLogout, btnReport, btnAdd, btnDelete, btnReset, btnCheck;

    // DATA MODEL (Must be static or public so Registry can see it)
    public static class ParkingSlot {
        int id;
        boolean occupied;
        boolean sensorOnline;
        String plate;
        long secondsRemaining;

        public ParkingSlot(int id, boolean occupied, String plate, long seconds) {
            this.id = id; this.occupied = occupied; this.plate = plate;
            this.secondsRemaining = seconds; this.sensorOnline = true;
        }
    }

    private Map<Integer, ParkingSlot> slotMap;
    private int selectedSlot = 1;
    private final com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Random random = new Random();
    private ParkingSlot currentlyInspectedSlot;
    private Label popSlotNum, popPlate, popTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // LINK TO SHARED REGISTRY (This is the fix)
        this.slotMap = ParkingRegistry.getInstance().slotMap;

        startSystemTimelines();
        updateOccupancyDisplay();
        updateSlotLabel();
        writeToLog("> BOOT_SEQUENCE_COMPLETE. SHARED_REGISTRY_LINKED.");
    }

    @FXML
    private void handleReport() {
        animateLogWithBuffer("PULLING LOT TELEMETRY", "[SUCCESS]", 0);

        Timeline reportDataTimeline = new Timeline(new KeyFrame(Duration.millis(1100), e -> {
            long occupiedCount = slotMap.values().stream().filter(s -> s.occupied).count();
            double estimatedRevenue = occupiedCount * 50.00;

            writeToLog("------------------------------------------");
            writeToLog("DATA_STREAM: PARKING_SUMMARY_v1.0");
            writeToLog("ACTIVE_SESSIONS  : " + occupiedCount);
            writeToLog("VACANT_SLOTS     : " + (slotMap.size() - occupiedCount));
            writeToLog("EST_REVENUE_24H  : $" + String.format("%.2f", estimatedRevenue));
            writeToLog("SYSTEM_HEALTH    : NOMINAL");
            writeToLog("------------------------------------------");
            writeToLog("> END_OF_TRANSMISSION.");
        }));
        reportDataTimeline.play();
    }

    @FXML
    private void handleReset() {
        if (btnReset == null) return;
        btnReset.setDisable(true);
        txtTerminal.clear();
        writeToLog("> INITIATING SYSTEM-WIDE DIAGNOSTICS...");

        for (ParkingSlot s : slotMap.values()) s.sensorOnline = true;

        double currentDelay = 500;
        for (ParkingSlot slot : slotMap.values()) {
            animateLogWithBuffer("PINGING SENSOR_" + String.format("%02d", slot.id), "[ONLINE]", currentDelay);
            currentDelay += 250;
        }

        Timeline finish = new Timeline(new KeyFrame(Duration.millis(currentDelay + 500), e -> {
            writeToLog("> ALL NODES RESPONDING. SYSTEM NOMINAL.");
            lblMainframeStatus.setText("MAINFRAME_STATUS: NOMINAL");
            lblMainframeStatus.setStyle("-fx-text-fill: #ff8c00; -fx-opacity: 0.4;");
            btnReset.setDisable(false);
        }));
        finish.play();
    }

    private void animateLogWithBuffer(String baseMsg, String finalResult, double initialDelayMs) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(initialDelayMs), e -> {
                    String ts = LocalDateTime.now().format(timeFormat);
                    txtTerminal.appendText("[" + ts + "] >> " + baseMsg);
                }),
                new KeyFrame(Duration.millis(initialDelayMs + 300), e -> txtTerminal.appendText(" .")),
                new KeyFrame(Duration.millis(initialDelayMs + 600), e -> txtTerminal.appendText(" .")),
                new KeyFrame(Duration.millis(initialDelayMs + 900), e -> {
                    txtTerminal.appendText(finalResult + "\n");
                    txtTerminal.setScrollTop(Double.MAX_VALUE);
                })
        );
        timeline.play();
    }

    @FXML
    private void handleViewLiveMap() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        HBox root = new HBox(20);
        root.setStyle("-fx-background-color: #000000; -fx-border-color: #ff8c00; -fx-border-width: 3; -fx-padding: 20;");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        for (ParkingSlot slot : slotMap.values()) {
            Button b = new Button("SLOT " + String.format("%02d", slot.id) + "\n" + (slot.sensorOnline ? (slot.occupied ? "ACTIVE" : "VACANT") : "OFFLINE"));
            b.setPrefSize(100, 60);
            b.setFont(javafx.scene.text.Font.font("Monospaced", 10));
            if (!slot.sensorOnline) b.setStyle("-fx-background-color: #440000; -fx-text-fill: #ff4500; -fx-border-color: #ff4500;");
            else if (slot.occupied) b.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-font-weight: bold;");
            else b.setStyle("-fx-background-color: transparent; -fx-border-color: #ff8c00; -fx-border-style: dashed; -fx-text-fill: #ff8c00;");

            b.setOnAction(e -> { currentlyInspectedSlot = slot; selectedSlot = slot.id; updateSlotLabel(); updatePopupDetails(); });
            grid.add(b, (slot.id-1) % 4, (slot.id-1) / 4);
        }

        VBox details = new VBox(15);
        details.setPrefWidth(260);
        Label head = new Label("[ SLOT_DIAGNOSTICS ]");
        head.setStyle("-fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
        popSlotNum = new Label("SLOT NUMBER: --");
        popPlate = new Label("OCCUPANT ID: [--]");
        popTimer = new Label("TIME REMAINING: 00:00:00");
        String ds = "-fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced'; -fx-font-size: 12;";
        popSlotNum.setStyle(ds); popPlate.setStyle(ds); popTimer.setStyle(ds);
        Button close = new Button("[ CLOSE_TERMINAL ]");
        close.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");
        close.setOnAction(e -> { currentlyInspectedSlot = null; popup.close(); });
        details.getChildren().addAll(head, popSlotNum, popPlate, popTimer, new Separator(), close);

        ScrollPane sp = new ScrollPane(grid);
        sp.setStyle("-fx-background:black; -fx-background-color:black; -fx-border-color:transparent;");
        sp.setPrefHeight(300); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getChildren().addAll(sp, details);
        popup.setScene(new Scene(root));
        popup.show();
    }

    @FXML
    private void handleLookup() {
        ParkingSlot s = slotMap.get(selectedSlot);
        animateLogWithBuffer("QUERYING SLOT_" + String.format("%02d", selectedSlot) + " HARDWARE", s.sensorOnline ? "[OK]" : "[FAIL]", 0);
        Timeline res = new Timeline(new KeyFrame(Duration.millis(1100), e -> {
            if (!s.sensorOnline) writeToLog("> STATUS: [SIGNAL_LOST] - HARDWARE INTERRUPT.");
            else if (!s.occupied) writeToLog("> STATUS: [VACANT] - SENSOR_ID_" + s.id + " CLEAR.");
            else writeToLog("> STATUS: [OCCUPIED] - VEHICLE_ID: " + s.plate + " | TTL: " + s.secondsRemaining + "s");
        }));
        res.play();
    }

    private void updatePopupDetails() {
        if (currentlyInspectedSlot == null) return;
        popSlotNum.setText("SLOT NUMBER: " + String.format("%02d", currentlyInspectedSlot.id));
        popPlate.setText("OCCUPANT ID: [" + (currentlyInspectedSlot.sensorOnline ? currentlyInspectedSlot.plate : "ERR_NO_DATA") + "]");
        long s = currentlyInspectedSlot.secondsRemaining;
        popTimer.setText(currentlyInspectedSlot.sensorOnline ? String.format("TIME REMAINING: %02d:%02d:%02d", s/3600, (s%3600)/60, s%60) : "SIGNAL_LOST");
    }

    private void startSystemTimelines() {
        Timeline global = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblClock.setText("SYSTEM TIME: " + LocalDateTime.now().format(timeFormat) + " [STABLE]");
            for (ParkingSlot s : slotMap.values()) if (s.occupied && s.secondsRemaining > 0 && s.sensorOnline) s.secondsRemaining--;
            if (random.nextDouble() < 0.005) {
                int rid = random.nextInt(slotMap.size()) + 1;
                if (slotMap.get(rid).sensorOnline) {
                    slotMap.get(rid).sensorOnline = false;
                    writeToLog("> CRITICAL: SENSOR_" + String.format("%02d", rid) + " OFFLINE.");
                    lblMainframeStatus.setText("STATUS: ERROR_DETECTED");
                    lblMainframeStatus.setStyle("-fx-text-fill: #ff4500;");
                }
            }
            if (currentlyInspectedSlot != null) updatePopupDetails();
            updateOccupancyDisplay(); // Keep the progress bar live
        }));
        global.setCycleCount(Animation.INDEFINITE); global.play();

        Timeline hw = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            double cpu = osBean.getSystemCpuLoad(); lblCpu.setText(String.format("CPU LOAD: %d%% [%s]", (int)(cpu*100), getBar(cpu, 10)));
            long t = osBean.getTotalPhysicalMemorySize(); long f = osBean.getFreePhysicalMemorySize();
            lblRam.setText(String.format("RAM UTIL: %.1f/%.1f GB [%s]", (t-f)/1e9, t/1e9, getBar((double)(t-f)/t, 10)));
        }));
        hw.setCycleCount(Animation.INDEFINITE); hw.play();
    }

    private String getBar(double p, int s) {
        int f = (int)(p*s); StringBuilder b = new StringBuilder();
        for(int i=0; i<s; i++) b.append(i<f ? "|" : ".");
        return b.toString();
    }

    private void updateOccupancyDisplay() {
        long occ = slotMap.values().stream().filter(s -> s.occupied).count();
        double p = (double)occ / slotMap.size(); StringBuilder b = new StringBuilder();
        int f = (int)(p*16); for(int i=0; i<16; i++) b.append(i<f ? "█" : "░");
        lblOccupancy.setText(String.format("SLOTS OCCUPIED: %d / %d [%s] %d%%", occ, slotMap.size(), b.toString(), (int)(p*100)));
    }

    public void writeToLog(String m) { txtTerminal.appendText("[" + LocalDateTime.now().format(timeFormat) + "] > " + m + "\n"); txtTerminal.setScrollTop(Double.MAX_VALUE); }
    @FXML private void handleIncrementSlot() { if (selectedSlot < slotMap.size()) { selectedSlot++; updateSlotLabel(); } }
    @FXML private void handleDecrementSlot() { if (selectedSlot > 1) { selectedSlot--; updateSlotLabel(); } }
    private void updateSlotLabel() { lblSlotDisplay.setText(String.format("SLOT_%02d", selectedSlot)); }
    @FXML private void handleLogout() throws IOException { Stage s = (Stage) btnLogout.getScene().getWindow(); Parent r = FXMLLoader.load(getClass().getResource("login-view.fxml")); s.setScene(new Scene(r)); }
    @FXML private void onBtnH(javafx.scene.input.MouseEvent e) { ((Button)e.getSource()).setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black;"); }
    @FXML private void onBtnEx(javafx.scene.input.MouseEvent e) { ((Button)e.getSource()).setStyle("-fx-background-color: transparent; -fx-text-fill: #ff8c00;"); }

    @FXML private void handleAddSlot() {
        int nid = slotMap.size()+1;
        slotMap.put(nid, new ParkingSlot(nid, false, "N/A", 0));
        writeToLog("INITIALIZING SLOT-" + nid);
    }
    @FXML private void handleDeleteSlot() {
        if(slotMap.size()>1) {
            int lid = slotMap.size();
            slotMap.remove(lid);
            writeToLog("DELETING SLOT-" + lid);
        }
    }
}