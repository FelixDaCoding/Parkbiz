CREATE DATABASE IF NOT EXISTS parkbiz_db;
USE parkbiz_db;

-- 1. Authentication Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role ENUM('ADMIN', 'DRIVER') NOT NULL
);

-- 2. Hardware Registry Table
CREATE TABLE parking_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slot_label VARCHAR(10) UNIQUE NOT NULL,
    status ENUM('VACANT', 'OCCUPIED') DEFAULT 'VACANT',
    sensor_health ENUM('ONLINE', 'OFFLINE') DEFAULT 'ONLINE',
    vehicle_id VARCHAR(20) DEFAULT 'N/A',
    session_ttl INT DEFAULT 0
);

-- 3. Transaction History Table
CREATE TABLE reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    slot_id INT,
    duration_hours INT,
    total_fee DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id)
);

-- SEED DATA
INSERT INTO users (username, password, role) VALUES 
('admin', 'admin123', 'ADMIN'),
('driver', '1234', 'DRIVER');

INSERT INTO parking_slots (slot_label) VALUES 
('A1'), ('A2'), ('A3'), ('B1'), ('B2'), ('B3');