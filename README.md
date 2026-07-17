**ParkBiz** - Cyber Parking Management System
A real-time parking management desktop application built with JavaFX and MySQL.

********Overview********
ParkBiz is a real-time parking management system with a cyber/terminal aesthetic. It features persistent sessions, dynamic timer calculations, and role-based access control for ADMIN and DRIVER users.

********Features********
**Authentication & Sessions**
Role-based login (ADMIN/DRIVER)
User registration with password validation
Persistent sessions - stay logged in after closing
Session recovery - resume where you left off

**Driver Dashboard**
Real-time slot availability

Quick parking with hour selection

Live fee calculator ($50/hour)

Countdown timer for active sessions

Cancel session with confirmation dialog

**Admin Mainframe**
Live slot monitoring with occupancy status

System metrics (CPU, RAM, DB connection)

Terminal-style logging output
Add/Delete slots dynamically
Reset sensors with diagnostics
Interactive live map view
Generate revenue reports

**Database Features**
Dynamic timestamp calculations
Auto-vacation of expired slots
Orphaned reservation cleanup
Transaction management with rollback

********Quick Start********
**Prerequisites**
Java 21+
XAMPP (MySQL/MariaDB)
IntelliJ IDEA (or any Java IDE)

**Step 1: Download Database**
Download parkbiz_db.sql from the repository.

**Step 2: Import Database**
Open XAMPP Control Panel
Start MySQL
Open phpMyAdmin (http://localhost/phpmyadmin)
Click New on the left sidebar
Create database named parkbiz_db
Click Import tab
Choose the parkbiz_db.sql file
Click Go

**Step 3: Run the App**
Open the project in IntelliJ IDEA
Navigate to Launcher.java
Click the Run button (green triangle)
Login with credentials below

**Default Credentials**
Role	Username	Password
Admin	admin	    admin123
Driver	driver	    1234

********How to Use********
**Driver**
Login with [**driver / 1234**]
Select an available slot
Enter hours (1-24)
Click CONFIRM & PARK
Watch countdown timer
Cancel or let expire to release slot

**Admin**
Login with [**admin / admin123**]
Monitor system metrics
Add/Delete slots
Reset sensors
View live map
Generate reports

**Tech Stack**
Component	  Technology
Frontend	  JavaFX, FXML, CSS
Backend	      Java
Database	  MySQL/MariaDB
Architecture  MVC, Singleton Pattern
