package com.example.parkbiz;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParkingRegistry {
    // Singleton Instance
    private static ParkingRegistry instance;

    // Shared Data
    public Map<Integer, AdminController.ParkingSlot> slotMap = new LinkedHashMap<>();

    private ParkingRegistry() {
        // Initialize 16 default slots once
        for (int i = 1; i <= 16; i++) {
            slotMap.put(i, new AdminController.ParkingSlot(i, false, "N/A", 0));
        }
    }

    public static ParkingRegistry getInstance() {
        if (instance == null) instance = new ParkingRegistry();
        return instance;
    }
}