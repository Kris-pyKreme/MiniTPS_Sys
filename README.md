# Clinic Appointment and Billing TPS (Java Swing + MySQL)

## 1. Set up the database
1. Start MySQL (XAMPP / MySQL Server).
2. Open `database/schema.sql` in MySQL Workbench or phpMyAdmin and run it. This creates `clinic_tps`, all tables, and 3 sample doctors.

## 2. Open the project in NetBeans (Maven, recommended)
1. **File > Open Project** and select the `ClinicTPS` folder (NetBeans detects the `pom.xml`).
2. NetBeans downloads the MySQL JDBC driver automatically on first build (needs internet once).
3. Open `src/main/java/database/DBConnection.java` and set `USER` / `PASS` to your MySQL login (XAMPP default: `root` and empty password).
4. Right-click the project > **Run** (or press F6).

### Alternative: plain NetBeans Ant project
1. **File > New Project > Java with Ant > Java Application**, untick "Create Main Class".
2. Copy the `database`, `dao`, `gui` folders and `Main.java` from `src/main/java` into the project's `src` folder.
3. Download `mysql-connector-j` from dev.mysql.com, then right-click **Libraries > Add JAR/Folder** and pick the jar.

## 3. How to use (follows the real workflow)
1. **Patient Registration:** register, search, update, delete patients.
2. **Appointments:** pick a patient and doctor, enter date `yyyy-MM-dd` and time `HH:mm`. Double-booking a doctor is blocked.
3. **Consultation:** pick a pending appointment, enter diagnosis/notes/prescription/fee. Saving marks the appointment *Completed* (one atomic JDBC transaction with commit/rollback).
4. **Billing:** pick an unbilled consultation, fee is auto-filled, add other charges, total is computed, set payment status/method.

Click any table row to load it into the form for Update/Delete.

## Project structure
```
database/schema.sql              MySQL tables + seed doctors
src/main/java/Main.java          entry point (checks DB connection)
src/main/java/database/          DBConnection
src/main/java/dao/               PatientDAO, AppointmentDAO, ConsultationDAO, BillingDAO
src/main/java/gui/               MainFrame, PatientPanel, AppointmentPanel, ConsultationPanel, BillingPanel, GuiUtil
```
