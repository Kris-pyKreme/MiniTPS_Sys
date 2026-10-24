package service;

import database.DBConnection;
import security.*;
import java.sql.*;
import java.util.List;

public class AccountService {
    public User authenticate(String username, char[] password, Role role) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM staff_users WHERE username=? AND role=?")) {
            p.setString(1, username.trim()); p.setString(2, role.toString());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !Passwords.matches(password, r.getString("salt"), r.getString("password_hash"), r.getString("password_scheme"))) return null;
                Integer doctor = (Integer) r.getObject("doctor_id");
                if (role == Role.DOCTOR && doctor == null) throw new SQLException("This doctor account needs a linked doctor. Contact your administrator.");
                int id = r.getInt("user_id");
                if (!Passwords.SCHEME.equals(r.getString("password_scheme"))) {
                    String salt = Passwords.salt();
                    try (PreparedStatement up = c.prepareStatement("UPDATE staff_users SET salt=?,password_hash=?,password_scheme=? WHERE user_id=? AND password_hash=?")) {
                        up.setString(1,salt); up.setString(2,Passwords.hash(password,salt)); up.setString(3,Passwords.SCHEME);
                        up.setInt(4,id); up.setString(5,r.getString("password_hash")); up.executeUpdate();
                    }
                }
                return new User(id, r.getString("username"), r.getString("full_name"), role, doctor, r.getInt("credential_version"));
            }
        }
    }
    /** Generic public response prevents account enumeration. One outstanding request per account. */
    public void requestReset(String username, Role role) throws SQLException {
        if (username.trim().isEmpty()) throw new IllegalArgumentException("Enter your username first.");
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO password_requests(user_id) SELECT user_id FROM staff_users WHERE username=? AND role=? "
                + "ON DUPLICATE KEY UPDATE requested_at=IF(status='Resolved',CURRENT_TIMESTAMP,requested_at),resolved_at=NULL,resolved_by=NULL,status='Pending'")) {
            p.setString(1,username.trim()); p.setString(2,role.toString()); p.executeUpdate();
        }
    }
    public String requestStatus() throws SQLException {
        User u = Session.verified();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("SELECT status,requested_at FROM password_requests WHERE user_id=?")) {
            p.setInt(1,u.id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) + " · Requested " + r.getTimestamp(2) : "No password change requested"; }
        }
    }
    public List<Object[]> staff() throws SQLException {
        Session.admin();
        return query("SELECT u.user_id,u.username,u.full_name,u.role,COALESCE(d.full_name,'—') FROM staff_users u LEFT JOIN doctors d ON d.doctor_id=u.doctor_id ORDER BY u.full_name");
    }
    public List<Object[]> requests() throws SQLException {
        Session.admin();
        return query("SELECT r.request_id,u.user_id,u.full_name,u.username,u.role,r.requested_at FROM password_requests r JOIN staff_users u ON u.user_id=r.user_id WHERE r.status='Pending' ORDER BY r.requested_at");
    }
    public void create(String username, String name, Role role, Integer doctorId, char[] password) throws SQLException {
        Session.admin(); Passwords.validate(password);
        if (!username.matches("[A-Za-z0-9_.-]{3,50}") || name.trim().isEmpty() || name.length()>100) throw new IllegalArgumentException("Use a 3–50 character username (letters, numbers, _, . or -) and a full name up to 100 characters.");
        if (role == Role.DOCTOR && doctorId == null) throw new IllegalArgumentException("Link this account to a doctor.");
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("INSERT INTO staff_users(username,full_name,role,doctor_id,salt,password_hash,password_scheme) VALUES(?,?,?,?,?,?,?)")) {
            if (role == Role.DOCTOR) {
                try (PreparedStatement valid = c.prepareStatement("SELECT 1 FROM doctors WHERE doctor_id=?")) {
                    valid.setInt(1,doctorId);
                    try(ResultSet r=valid.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("Select an existing doctor."); }
                }
            }
            String salt = Passwords.salt();
            p.setString(1,username); p.setString(2,name.trim()); p.setString(3,role.toString());
            if (role == Role.DOCTOR) p.setInt(4,doctorId); else p.setNull(4,Types.INTEGER);
            p.setString(5,salt); p.setString(6,Passwords.hash(password,salt)); p.setString(7,Passwords.SCHEME); p.executeUpdate();
        }
    }
    public void updateStaff(int id,String name,Role role,Integer doctorId) throws SQLException {
        Session.admin();
        if(name.trim().isEmpty() || name.length()>100) throw new IllegalArgumentException("Enter a full name up to 100 characters.");
        if(id==Session.current().id && role!=Role.ADMIN) throw new IllegalArgumentException("You cannot remove your own administrator access.");
        if(role==Role.DOCTOR && doctorId==null) throw new IllegalArgumentException("Link the account to a doctor.");
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE staff_users SET full_name=?,role=?,doctor_id=?,credential_version=credential_version+1 WHERE user_id=?")) {
            if(role==Role.DOCTOR) {
                try(PreparedStatement q=c.prepareStatement("SELECT 1 FROM doctors WHERE doctor_id=?")) {
                    q.setInt(1,doctorId);try(ResultSet r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("Select an existing doctor.");}
                }
            }
            p.setString(1,name.trim());p.setString(2,role.toString());
            if(role==Role.DOCTOR)p.setInt(3,doctorId);else p.setNull(3,Types.INTEGER);
            p.setInt(4,id);if(p.executeUpdate()!=1)throw new SQLException("Staff account no longer exists.");
        }
    }
    public void resetPassword(int userId, char[] password) throws SQLException {
        Session.admin(); Passwords.validate(password); User admin = Session.current();
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("UPDATE staff_users SET salt=?,password_hash=?,password_scheme=?,credential_version=credential_version+1 WHERE user_id=?");
                 PreparedStatement q = c.prepareStatement("UPDATE password_requests SET status='Resolved',resolved_at=CURRENT_TIMESTAMP,resolved_by=? WHERE user_id=? AND status='Pending'")) {
                String salt = Passwords.salt(); p.setString(1,salt); p.setString(2,Passwords.hash(password,salt)); p.setString(3,Passwords.SCHEME); p.setInt(4,userId);
                if (p.executeUpdate()!=1) throw new SQLException("Staff account no longer exists.");
                q.setInt(1,admin.id); q.setInt(2,userId); q.executeUpdate(); c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
    private List<Object[]> query(String sql) throws SQLException {
        try(Connection c=DBConnection.getConnection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) { return DBConnection.rows(r); }
    }
}
