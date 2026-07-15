package com.example.parkbiz;

public class ParkingSession {
    private final double HOURLY_RATE = 50.00; // Fixed rate per hour

    public double calculateFee(int hours) {
        return hours * HOURLY_RATE;
    }
}