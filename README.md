ParkBiz - Cyber Parking Management System
A real-time parking management desktop application built with JavaFX and MySQL.

Overview
ParkBiz is a real-time parking management system with a cyber/terminal aesthetic. It features persistent sessions, dynamic timer calculations, and role-based access control for ADMIN and DRIVER users.

Features
Authentication & Sessions
Role-based login (ADMIN/DRIVER)

User registration with password validation

Persistent sessions - stay logged in after closing

Session recovery - resume where you left off

Driver Dashboard
Real-time slot availability

Quick parking with hour selection

Live fee calculator ($50/hour)

Countdown timer for active sessions

Cancel session with confirmation dialog

Admin Mainframe
Live slot monitoring with occupancy status

System metrics (CPU, RAM, DB connection)

Terminal-style logging output

Add/Delete slots dynamically

Reset sensors with diagnostics

Interactive live map view

Generate revenue reports

Database Features
Dynamic timestamp calculations

Auto-vacation of expired slots

Orphaned reservation cleanup

Transaction management with rollback

Quick Start
Prerequisites
Java 21+

MySQL/MariaDB 8.0+

Step 1: Import Database
bash
mysql -u root -p < database/parkbiz_db.sql
Step 2: Configure Connection
Edit DBConnection.java:

java
private static final String URL = "jdbc:mysql://localhost:3306/parkbiz_db";
private static final String USER = "root";
private static final String PASS = "your_password";
Step 3: Run the App
bash
mvn clean javafx:run
Database Schema
sql
-- Users Table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role ENUM('ADMIN', 'DRIVER') NOT NULL
);

-- Parking Slots Table
CREATE TABLE parking_slots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    slot_label VARCHAR(10) UNIQUE NOT NULL,
    status ENUM('VACANT', 'OCCUPIED') DEFAULT 'VACANT',
    sensor_health ENUM('ONLINE', 'OFFLINE') DEFAULT 'ONLINE',
    vehicle_id VARCHAR(20) DEFAULT 'N/A',
    session_ttl INT DEFAULT 0
);

-- Reservations Table
CREATE TABLE reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    slot_id INT,
    duration_hours INT,
    total_fee DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id)
);

-- Default Users
INSERT INTO users (username, password, role) VALUES
('admin', 'admin123', 'ADMIN'),
('driver', '1234', 'DRIVER');

-- Default Slots
INSERT INTO parking_slots (slot_label, status, sensor_health) VALUES
('A1', 'VACANT', 'ONLINE'),
('A2', 'VACANT', 'ONLINE'),
('A3', 'VACANT', 'ONLINE'),
('B1', 'VACANT', 'ONLINE'),
('B2', 'VACANT', 'ONLINE');
How to Use
Driver
Login with driver / 1234

Select an available slot

Enter hours (1-24)

Click CONFIRM & PARK

Watch countdown timer

Cancel or let expire to release slot

Admin
Login with admin / admin123

Monitor system metrics

Add/Delete slots

Reset sensors

View live map

Generate reports

Tech Stack
Component	Technology
Frontend	JavaFX, FXML, CSS
Backend	Java
Database	MySQL/MariaDB
Architecture	MVC, Singleton Pattern
ParkBiz - Where Parking Meets Cyber!
