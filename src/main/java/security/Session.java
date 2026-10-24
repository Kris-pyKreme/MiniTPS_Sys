package security;

import database.DBConnection;
import java.sql.*;

/** One authenticated desktop session. Authorization is checked in DAOs as well as navigation. */
public final class Session {
    private static volatile User user;
    private Session() { }
    public static void login(User authenticated) { user = authenticated; }
    public static void logout() { user = null; }
    public static User current() {
        User u = user;
        if (u == null) throw new SecurityException("Please sign in first.");
        return u;
    }
    public static User verified() throws SQLException {
        User u = current();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT credential_version,role,doctor_id FROM staff_users WHERE user_id=?")) {
            p.setInt(1, u.id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || r.getInt(1) != u.credentialVersion || !r.getString(2).equals(u.role.toString())
                        || !java.util.Objects.equals((Integer) r.getObject(3), u.doctorId)) {
                    throw new SecurityException("Your account was updated. Please sign in again.");
                }
            }
        }
        return u;
    }
    public static void require(boolean allowed) throws SQLException {
        verified();
        if (!allowed) throw new SecurityException("Your role does not allow this action.");
    }
    public static void admin() throws SQLException { require(current().role == Role.ADMIN); }
    public static String doctorFilter(String column) {
        User u = current();
        return u.role == Role.DOCTOR ? " AND " + column + "=" + (u.doctorId == null ? -1 : u.doctorId) + " " : " ";
    }
    public static void appointment(int id) throws SQLException {
        User u = verified();
        if (u.role != Role.DOCTOR) return;
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT 1 FROM appointments WHERE appointment_id=? AND doctor_id=?")) {
            p.setInt(1, id); p.setInt(2, u.doctorId == null ? -1 : u.doctorId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SecurityException("This appointment is not assigned to you.");
            }
        }
    }
    public static void consultation(int id) throws SQLException {
        verified();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT appointment_id FROM consultations WHERE consultation_id=?")) {
            p.setInt(1,id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Consultation no longer exists.");
                appointment(r.getInt(1));
            }
        }
    }
}
