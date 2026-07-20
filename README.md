ParkBiz - Cyber Parking Management System
A real-time parking management desktop application built with JavaFX and MySQL with a cyber/terminal aesthetic.

Overview
ParkBiz is a real-time parking management system featuring persistent user sessions, dynamic timer calculations, and role-based access control for ADMIN and DRIVER users. The system employs Java Serialization for session persistence and follows SOLID design principles for maintainable, scalable architecture.

Features
Core Features
Role-based Authentication - Admin and Driver login with secure validation
User Registration - Password strength enforcement (8+ chars with alphanumeric)
Real-time Parking - Live slot monitoring with occupancy status
Dynamic Timer - Countdown with auto-vacation on expiry
Fee Calculator - $50/hour with instant total display
System Monitoring - CPU, RAM, and database connection status (Admin only)
Slot Management - Add/Delete slots with automatic labeling (Admin only)
Live Map View - Interactive slot inspection (Admin only)
Report Generation - Revenue and occupancy summaries (Admin only)

Java Serialization Implementation
The system uses Java Serialization to maintain persistent user sessions across application restarts.

How It Works
Creation: Upon successful login, SessionManager serializes user data (userId, username, role, lastView, timestamp) to ~/.parkbiz_session.dat
Validation: On startup, the system checks for existing session file and auto-redirects users
Expiry: Sessions automatically expire after 24 hours
Deletion: Session file is securely deleted on explicit logout

Key Classes
SessionManager - Handles all serialization operations
SessionData - Serializable container for session payload
LoginApp - Entry point that checks for existing sessions

SOLID Design Principles Applied
1. Single Responsibility Principle (SRP)
Each class handles one specific responsibility:

Class	Responsibility
DBConnection	Database connectivity only
ParkingRegistry	Parking data operations (CRUD, reservations)
SessionManager	Session serialization and persistence
UserSession	In-memory current user state
AdminController	Admin UI event handling
DashboardController	Driver UI event handling
Example: ParkingRegistry handles ALL database operations, keeping controllers free from SQL logic.

2. Dependency Inversion Principle (DIP)
Controllers depend on abstractions rather than concrete implementations:

java
public class AdminController implements Initializable {
    private ParkingRegistry registry = ParkingRegistry.getInstance();
    // Depends on abstract service, not concrete DB implementation
}

Benefits:
Loose coupling between UI and business logic
Easy to mock for testing
Flexible for future database changes

Quick Start
Prerequisites
Java 21+

XAMPP (MySQL/MariaDB)

IntelliJ IDEA (or any Java IDE)

Database Setup
Download parkbiz_db.sql from the repository

Open XAMPP → Start MySQL → Open phpMyAdmin

Create database named parkbiz_db

Import the SQL file

Run Application
Open project in IntelliJ IDEA

Navigate to LoginApp.java

Click Run

Default Credentials
Role	Username	Password
Admin	admin	admin123
Driver	driver	1234

How to Use
Driver Mode
Login with driver / 1234
Select an available slot
Enter hours (1-24)
Click CONFIRM & PARK
Watch countdown timer
Cancel or let expire to release slot

Admin Mode
Login with admin / admin123
Monitor system metrics
Add/Delete slots
Reset sensors
View live map
Generate reports

