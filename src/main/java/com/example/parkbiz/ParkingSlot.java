package com.example.parkbiz;

public class ParkingSlot {
    private String id;
    private boolean isOccupied;
    private boolean isReserved;

    public ParkingSlot(String id) {
        this.id = id;
        this.isOccupied = false;
        this.isReserved = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
    public boolean isReserved() { return isReserved; }
    public void setReserved(boolean reserved) { isReserved = reserved; }
}