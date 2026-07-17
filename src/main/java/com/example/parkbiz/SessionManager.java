package com.example.parkbiz;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class SessionManager {
    private static final String SESSION_FILE = System.getProperty("user.home") + File.separator + ".parkbiz_session.dat";
    private static SessionManager instance;

    private int userId;
    private String username;
    private String role;
    private String lastView;
    private long sessionTimestamp;
    private boolean isLoggedIn;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
            instance.loadSession();
        }
        return instance;
    }

    public void saveSession(int userId, String username, String role, String lastView) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.lastView = lastView;
        this.sessionTimestamp = System.currentTimeMillis();
        this.isLoggedIn = true;

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SESSION_FILE))) {
            oos.writeObject(new SessionData(userId, username, role, lastView, sessionTimestamp, isLoggedIn));
        } catch (IOException e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    public void loadSession() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(SESSION_FILE))) {
            SessionData data = (SessionData) ois.readObject();

            // Check if session is still valid (e.g., 24 hour expiry)
            long hoursSince = (System.currentTimeMillis() - data.sessionTimestamp) / (1000 * 60 * 60);
            if (hoursSince < 24) {
                this.userId = data.userId;
                this.username = data.username;
                this.role = data.role;
                this.lastView = data.lastView;
                this.isLoggedIn = data.isLoggedIn;

                // Restore UserSession singleton
                UserSession.getInstance().setUser(data.userId, data.username);
            } else {
                clearSession();
            }
        } catch (IOException | ClassNotFoundException e) {
            // No session exists or corrupted - that's fine
            clearSession();
        }
    }

    public void clearSession() {
        this.isLoggedIn = false;
        this.userId = -1;
        this.username = null;
        this.role = null;
        this.lastView = null;
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (IOException e) {
            // Ignore
        }
    }

    public boolean isLoggedIn() { return isLoggedIn; }
    public String getRole() { return role; }
    public String getLastView() { return lastView; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }

    // Serializable data container
    private static class SessionData implements Serializable {
        private static final long serialVersionUID = 1L;
        int userId;
        String username;
        String role;
        String lastView;
        long sessionTimestamp;
        boolean isLoggedIn;

        SessionData(int userId, String username, String role, String lastView, long sessionTimestamp, boolean isLoggedIn) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.lastView = lastView;
            this.sessionTimestamp = sessionTimestamp;
            this.isLoggedIn = isLoggedIn;
        }
    }
}