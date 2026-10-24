package database;

import java.sql.*;

/** Additive upgrade for the original clinic database. Never drops records or reseeds accounts. */
public final class DatabaseSetup {
    private DatabaseSetup() { }
    public static void upgrade() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS staff_users (user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, salt VARCHAR(32) NOT NULL, password_hash CHAR(64) NOT NULL, full_name VARCHAR(100) NOT NULL, role ENUM('Admin','Receptionist','Doctor','Nurse','Cashier') DEFAULT 'Receptionist')");
            add(c, s, "doctor_id", "INT NULL");
            add(c, s, "password_scheme", "VARCHAR(24) NOT NULL DEFAULT 'SHA256'");
            add(c, s, "credential_version", "INT NOT NULL DEFAULT 1");
            // Retain legacy Cashier records; they can be reassigned by an administrator.
            s.executeUpdate("ALTER TABLE staff_users MODIFY role ENUM('Admin','Receptionist','Doctor','Nurse','Cashier') NOT NULL DEFAULT 'Receptionist'");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS password_requests (request_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL UNIQUE, requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, status ENUM('Pending','Resolved') NOT NULL DEFAULT 'Pending', resolved_at TIMESTAMP NULL, resolved_by INT NULL, FOREIGN KEY (user_id) REFERENCES staff_users(user_id) ON DELETE CASCADE)");
        }
    }
    private static void add(Connection c, Statement s, String column, String definition) throws SQLException {
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "staff_users", column)) {
            if (!r.next()) s.executeUpdate("ALTER TABLE staff_users ADD COLUMN " + column + " " + definition);
        }
    }
}
