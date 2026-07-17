package com.example.parkbiz;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParkingRegistry {
    private static ParkingRegistry instance;
    private ParkingRegistry() {}

    public static ParkingRegistry getInstance() {
        if (instance == null) instance = new ParkingRegistry();
        return instance;
    }

    /**
     * Fetches all slots from the Database.
     * Uses the 'slot_label' column to populate the tactical coordinate (A1, B2, etc.)
     */
    public Map<Integer, AdminController.ParkingSlot> getLiveSlots() throws SQLException {
        Map<Integer, AdminController.ParkingSlot> map = new LinkedHashMap<>();
        String sql = "SELECT * FROM parking_slots ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // We pass the slot_label (A1, B2...) into the ParkingSlot object
                AdminController.ParkingSlot slot = new AdminController.ParkingSlot(
                        rs.getInt("id"),
                        rs.getString("slot_label"),
                        rs.getString("status").equals("OCCUPIED"),
                        rs.getString("vehicle_id"),
                        rs.getLong("session_ttl")
                );
                // Sync sensor health
                slot.sensorOnline = rs.getString("sensor_health").equals("ONLINE");
                map.put(slot.id, slot);
            }
        }
        return map;
    }

    /**
     * Adds a new hardware node (Slot) with an automatic grid label.
     * Logic: 3 slots per row. (1-3 = A, 4-6 = B, etc.)
     */
    public void addSlotToDatabase(int id) throws SQLException {
        // Calculate Grid Coordinate
        int rowIndex = (id - 1) / 3;
        int colIndex = (id - 1) % 3 + 1;
        char rowLetter = (char) ('A' + rowIndex);
        String label = rowLetter + String.valueOf(colIndex); // Results in "A1", "B3", etc.

        String sql = "INSERT INTO parking_slots (id, slot_label, status, sensor_health, vehicle_id, session_ttl) VALUES (?, ?, 'VACANT', 'ONLINE', 'N/A', 0)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, label);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a hardware node from the SQL registry.
     */
    public void deleteSlotFromDatabase(int id) throws SQLException {
        String sql = "DELETE FROM parking_slots WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Transactional Method: Reserves a slot and logs it in the reservations table.
     */
    public void reserveSlot(int userId, int slotId, int hours, double fee, String plate) throws SQLException {
        String updateSlotSql = "UPDATE parking_slots SET status='OCCUPIED', vehicle_id=?, session_ttl=? WHERE id=?";
        String insertReservationSql = "INSERT INTO reservations (user_id, slot_id, duration_hours, total_fee) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Update the slot status
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateSlotSql)) {
                pstmt1.setString(1, plate);
                pstmt1.setInt(2, hours * 3600);
                pstmt1.setInt(3, slotId);
                pstmt1.executeUpdate();
            }

            // 2. Log the transaction in the history table
            try (PreparedStatement pstmt2 = conn.prepareStatement(insertReservationSql)) {
                pstmt2.setInt(1, userId);
                pstmt2.setInt(2, slotId);
                pstmt2.setInt(3, hours);
                pstmt2.setDouble(4, fee);
                pstmt2.executeUpdate();
            }

            conn.commit(); // Save changes
        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // Undo on error
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    /**
     * Resets a slot to VACANT status in the database.
     */
    public void vacateSlotInDatabase(int id) throws SQLException {
        String sql = "UPDATE parking_slots SET status='VACANT', vehicle_id='N/A', session_ttl=0 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}