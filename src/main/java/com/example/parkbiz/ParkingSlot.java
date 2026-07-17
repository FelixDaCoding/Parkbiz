package com.example.parkbiz;

public class ParkingSlot {
    private String id;
    private boolean isOccupied;

    public ParkingSlot(String id, boolean isOccupied) {
        this.id = id;
        this.isOccupied = isOccupied;
    }


    public String getId() { return id; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
}