# ParkBiz - Cyber Parking Management System

A real-time parking management desktop application built with JavaFX and MySQL.

## Overview

ParkBiz is a real-time parking management system with a cyber/terminal aesthetic. It features persistent sessions, dynamic timer calculations, and role-based access control for ADMIN and DRIVER users.

## Features

### Authentication & Sessions
- Role-based login (ADMIN/DRIVER)
- User registration with password validation
- Persistent sessions - stay logged in after closing
- Session recovery - resume where you left off

### Driver Dashboard
- Real-time slot availability
- Quick parking with hour selection
- Live fee calculator ($50/hour)
- Countdown timer for active sessions
- Cancel session with confirmation dialog

### Admin Mainframe
- Live slot monitoring with occupancy status
- System metrics (CPU, RAM, DB connection)
- Terminal-style logging output
- Add/Delete slots dynamically
- Reset sensors with diagnostics
- Interactive live map view
- Generate revenue reports

### Database Features
- Dynamic timestamp calculations
- Auto-vacation of expired slots
- Orphaned reservation cleanup
- Transaction management with rollback

## SOLID Design Principles Applied

### Single Responsibility Principle (SRP)
Each class handles one specific responsibility:

| Class | Responsibility |
|-------|---------------|
| DBConnection | Database connectivity only |
| ParkingRegistry | Parking data operations (CRUD, reservations) |
| SessionManager | Session serialization and persistence |
| UserSession | In-memory current user state |
| AdminController | Admin UI event handling |
| DashboardController | Driver UI event handling |

**Example**: ParkingRegistry handles ALL database operations, keeping controllers free from SQL logic.

### Dependency Inversion Principle (DIP)
Controllers depend on abstractions rather than concrete implementations:

```java
public class AdminController implements Initializable {
    private ParkingRegistry registry = ParkingRegistry.getInstance();
}
```

**Benefits**: Loose coupling, easy testing, flexible for future changes.

## Behavioral Design Pattern Applied

### Observer Pattern
The system uses JavaFX Timeline and KeyFrame to implement the Observer pattern for real-time UI updates:

```java
Timeline global = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
    lblClock.setText("SYSTEM TIME: " + LocalDateTime.now().format(timeFormat));
    syncWithDatabase(); // Observes time changes and updates UI
    // Update all slot buttons
    for (Map.Entry<Integer, Button> entry : liveMapButtons.entrySet()) {
        ParkingSlot s = slotMap.get(entry.getKey());
        if (s != null) updateMapButtonDisplay(entry.getValue(), s);
    }
}));
global.setCycleCount(Animation.INDEFINITE);
global.play();
```

**How it works**: The Timeline acts as the Subject that observes time changes every second. All registered KeyFrame handlers are Observers that get notified and update the UI components (clock, database sync, slot displays) automatically.

## UML Diagrams

Click the links below to view full-size UML diagrams:

| Diagram | Link |
|---------|------|
| **Class Diagram** | [View Class Diagram](https://github.com/user-attachments/assets/c60a0835-e105-45cc-b4a1-3509da97884e) |
| **Activity Diagram** | [View Activity Diagram](https://github.com/user-attachments/assets/4a2de67f-a780-4d99-b4b2-80fa119db80c) |
| **Sequence Diagram** | [View Sequence Diagram](https://github.com/user-attachments/assets/8242dcc9-4e1c-46b6-8e1e-27b96c114889) |
| **Use-Case Diagram** | [View Use-Case Diagram](https://github.com/user-attachments/assets/30bce598-76aa-496f-b6e3-dcc12e0f3fcf) |

### Complete Diagram Collection

> **[Click here to view the full UML diagram collection](https://github.com/user-attachments/assets/2b2d2039-a099-4161-b214-948a78ebee72)** - Open in new tab for zoom and pan


## Quick Start

### Prerequisites
- Java 21+
- XAMPP (MySQL/MariaDB)
- IntelliJ IDEA (or any Java IDE)

### Step 1: Download Database
Download parkbiz_db.sql from the repository.

### Step 2: Import Database
1. Open XAMPP Control Panel
2. Start MySQL
3. Open phpMyAdmin (http://localhost/phpmyadmin)
4. Click New on the left sidebar
5. Create database named parkbiz_db
6. Click Import tab
7. Choose the parkbiz_db.sql file
8. Click Go

### Step 3: Run the App
1. Open the project in IntelliJ IDEA
2. Navigate to LoginApp.java
3. Click the Run button (green triangle)
4. Login with credentials below

### Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Driver | driver | 1234 |

## How to Use

### Driver
1. Login with [driver / 1234]
2. Select an available slot
3. Enter hours (1-24)
4. Click CONFIRM & PARK
5. Watch countdown timer
6. Cancel or let expire to release slot

### Admin
1. Login with [admin / admin123]
2. Monitor system metrics
3. Add/Delete slots
4. Reset sensors
5. View live map
6. Generate reports

## Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | JavaFX, FXML, CSS |
| Backend | Java |
| Database | MySQL/MariaDB |
| Architecture | MVC, Singleton Pattern |
