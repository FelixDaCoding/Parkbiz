package com.example.parkbiz;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParkingRegistry {
    private static ParkingRegistry instance;
    public static final double HOURLY_RATE = 50.00;

    private ParkingRegistry() {}

    public static ParkingRegistry getInstance() {
        if (instance == null) instance = new ParkingRegistry();
        return instance;
    }

    /**
     * Active session container helper to track persistent user sessions across logouts.
     */
    public static class ActiveSession {
        public int slotId;
        public String slotLabel;
        public long secondsRemaining;
        public int durationHours;
        public double totalFee;
        public int reservationId;

        public ActiveSession(int slotId, String slotLabel, long secondsRemaining, int durationHours, double totalFee, int reservationId) {
            this.slotId = slotId;
            this.slotLabel = slotLabel;
            this.secondsRemaining = secondsRemaining;
            this.durationHours = durationHours;
            this.totalFee = totalFee;
            this.reservationId = reservationId;
        }
    }

    /**
     * Fetches slots and calculates remaining TTL dynamically based on reservation timestamps.
     * Auto-vacates slots in the database when their allocated time has expired.
     */
    public Map<Integer, AdminController.ParkingSlot> getLiveSlots() throws SQLException {
        Map<Integer, AdminController.ParkingSlot> map = new LinkedHashMap<>();

        String sql = "SELECT s.*, r.created_at, r.duration_hours " +
                "FROM parking_slots s " +
                "LEFT JOIN (" +
                "    SELECT slot_id, created_at, duration_hours " +
                "    FROM reservations " +
                "    WHERE id IN (SELECT MAX(id) FROM reservations GROUP BY slot_id)" +
                ") r ON s.id = r.slot_id " +
                "ORDER BY s.id ASC";

        List<Integer> expiredSlots = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String label = rs.getString("slot_label");
                boolean occupied = rs.getString("status").equals("OCCUPIED");
                String vehicleId = rs.getString("vehicle_id");
                long staticTtl = rs.getLong("session_ttl");

                Timestamp createdAt = rs.getTimestamp("created_at");
                int durationHours = rs.getInt("duration_hours");

                long secondsRemaining = 0;
                if (occupied) {
                    if (createdAt != null) {
                        long elapsed = Duration.between(createdAt.toLocalDateTime(), LocalDateTime.now()).getSeconds();
                        long totalSecs = (long) durationHours * 3600;
                        long remaining = totalSecs - elapsed;

                        if (remaining <= 0) {
                            occupied = false;
                            vehicleId = "N/A";
                            expiredSlots.add(id);
                        } else {
                            secondsRemaining = remaining;
                        }
                    } else {
                        secondsRemaining = staticTtl;
                    }
                }

                AdminController.ParkingSlot slot = new AdminController.ParkingSlot(
                        id,
                        label,
                        occupied,
                        vehicleId,
                        secondsRemaining
                );
                slot.sensorOnline = rs.getString("sensor_health").equals("ONLINE");
                map.put(slot.id, slot);
            }
        }

        for (int expiredId : expiredSlots) {
            try {
                vacateSlotInDatabase(expiredId);
            } catch (SQLException e) {
                System.err.println("Auto-vacation update failed for slot " + expiredId + ": " + e.getMessage());
            }
        }

        return map;
    }

    /**
     * Retrieves an active session for a specific user to restore their UI upon login.
     */
    public ActiveSession getActiveSessionForUser(int userId) throws SQLException {
        String sql = "SELECT r.id, r.*, s.slot_label, s.status " +
                "FROM reservations r " +
                "JOIN parking_slots s ON r.slot_id = s.id " +
                "WHERE r.user_id = ? AND s.status = 'OCCUPIED' " +
                "ORDER BY r.id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int reservationId = rs.getInt("id");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    int durationHours = rs.getInt("duration_hours");
                    int slotId = rs.getInt("slot_id");
                    String label = rs.getString("slot_label");
                    double fee = rs.getDouble("total_fee");

                    long elapsed = Duration.between(createdAt.toLocalDateTime(), LocalDateTime.now()).getSeconds();
                    long totalSecs = (long) durationHours * 3600;
                    long remaining = totalSecs - elapsed;

                    if (remaining > 0) {
                        return new ActiveSession(slotId, label, remaining, durationHours, fee, reservationId);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds a new hardware node (Slot) with an automatic grid label.
     */
    public void addSlotToDatabase(int id) throws SQLException {
        int rowIndex = (id - 1) / 3;
        int colIndex = (id - 1) % 3 + 1;
        char rowLetter = (char) ('A' + rowIndex);
        String label = rowLetter + String.valueOf(colIndex);

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
     * Also deletes all associated reservations first.
     */
    public void deleteSlotFromDatabase(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String deleteReservationsSql = "DELETE FROM reservations WHERE slot_id = ?";
            try (PreparedStatement pstmt1 = conn.prepareStatement(deleteReservationsSql)) {
                pstmt1.setInt(1, id);
                pstmt1.executeUpdate();
            }

            String deleteSlotSql = "DELETE FROM parking_slots WHERE id = ?";
            try (PreparedStatement pstmt2 = conn.prepareStatement(deleteSlotSql)) {
                pstmt2.setInt(1, id);
                pstmt2.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    /**
     * Transactional Method: Reserves a slot and logs it in the reservations table.
     * Returns the generated reservation ID.
     */
    public int reserveSlot(int userId, int slotId, int hours, double fee, String plate) throws SQLException {
        String updateSlotSql = "UPDATE parking_slots SET status='OCCUPIED', vehicle_id=?, session_ttl=? WHERE id=?";
        String insertReservationSql = "INSERT INTO reservations (user_id, slot_id, duration_hours, total_fee) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        int reservationId = -1;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(updateSlotSql)) {
                pstmt1.setString(1, plate);
                pstmt1.setInt(2, hours * 3600);
                pstmt1.setInt(3, slotId);
                pstmt1.executeUpdate();
            }

            try (PreparedStatement pstmt2 = conn.prepareStatement(insertReservationSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt2.setInt(1, userId);
                pstmt2.setInt(2, slotId);
                pstmt2.setInt(3, hours);
                pstmt2.setDouble(4, fee);
                pstmt2.executeUpdate();

                try (ResultSet generatedKeys = pstmt2.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reservationId = generatedKeys.getInt(1);
                    }
                }
            }

            conn.commit();
            return reservationId;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    /**
     * Deletes the most recent reservation for a specific slot and user.
     */
    public void deleteReservationForSlot(int slotId, int userId) throws SQLException {
        String sql = "DELETE FROM reservations WHERE slot_id = ? AND user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, slotId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Clears all orphaned reservations that are not connected to valid driver accounts.
     * Also vacates any slots that are marked as OCCUPIED but have no valid reservation.
     */
    public void clearOrphanedReservations() throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Delete reservations where user_id doesn't exist in users table or user is not a DRIVER
            String deleteOrphanedReservations =
                    "DELETE r FROM reservations r " +
                            "LEFT JOIN users u ON r.user_id = u.id " +
                            "WHERE u.id IS NULL OR u.role != 'DRIVER'";

            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrphanedReservations)) {
                int deletedCount = pstmt.executeUpdate();
                System.out.println("Deleted " + deletedCount + " orphaned reservation(s)");
            }

            // 2. Find all slots that are OCCUPIED but have no active reservation
            String findOrphanedSlots =
                    "SELECT s.id FROM parking_slots s " +
                            "LEFT JOIN reservations r ON s.id = r.slot_id " +
                            "WHERE s.status = 'OCCUPIED' " +
                            "GROUP BY s.id " +
                            "HAVING COUNT(r.id) = 0";

            List<Integer> orphanedSlots = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(findOrphanedSlots)) {
                while (rs.next()) {
                    orphanedSlots.add(rs.getInt("id"));
                }
            }

            // 3. Vacate any orphaned slots
            for (int slotId : orphanedSlots) {
                String vacateSql = "UPDATE parking_slots SET status='VACANT', vehicle_id='N/A', session_ttl=0 WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(vacateSql)) {
                    pstmt.setInt(1, slotId);
                    pstmt.executeUpdate();
                    System.out.println("Vacated orphaned slot: " + slotId);
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* ignore */ }
            }
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

    /**
     * Updates sensor health status in the database.
     */
    public void updateSensorHealth(int slotId, boolean online) throws SQLException {
        String status = online ? "ONLINE" : "OFFLINE";
        String sql = "UPDATE parking_slots SET sensor_health = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, slotId);
            pstmt.executeUpdate();
        }
    }
}