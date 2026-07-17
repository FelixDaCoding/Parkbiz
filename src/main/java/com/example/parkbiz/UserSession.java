package com.example.parkbiz;

public class UserSession {
    private static UserSession instance;
    private int userId;
    private String username;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setUser(int id, String name) {
        this.userId = id;
        this.username = name;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
}