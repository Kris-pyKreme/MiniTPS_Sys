package dao;

import database.DBConnection;
import java.sql.*;
import java.util.List;

public class AppointmentDAO {

    public void insert(int patientId, int doctorId, Date date, Time time,
                       String status, String reason) throws SQLException {
        String sql = "INSERT INTO appointments (patient_id,doctor_id,appointment_date,appointment_time,status,reason) "
                   + "VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, patientId, doctorId, date, time, status, reason);
            ps.executeUpdate();
        }
    }

    public void update(int id, int patientId, int doctorId, Date date, Time time,
                       String status, String reason) throws SQLException {
        String sql = "UPDATE appointments SET patient_id=?, doctor_id=?, appointment_date=?, "
                   + "appointment_time=?, status=?, reason=? WHERE appointment_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, patientId, doctorId, date, time, status, reason);
            ps.setInt(7, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM appointments WHERE appointment_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Object[]> getAll(String kw) throws SQLException {
        String sql = "SELECT a.appointment_id, CONCAT(p.patient_id,' - ',p.first_name,' ',p.last_name), "
                   + "CONCAT(d.doctor_id,' - ',d.full_name), a.appointment_date, a.appointment_time, a.status, a.reason "
                   + "FROM appointments a JOIN patients p ON a.patient_id=p.patient_id "
                   + "JOIN doctors d ON a.doctor_id=d.doctor_id "
                   + "WHERE CONCAT(p.first_name,' ',p.last_name) LIKE ? OR d.full_name LIKE ? OR a.status LIKE ? "
                   + "ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, "%" + kw + "%");
            try (ResultSet rs = ps.executeQuery()) { return DBConnection.rows(rs); }
        }
    }

    /** True if the doctor already has a Scheduled appointment at that date/time. */
    public boolean slotTaken(int doctorId, Date date, Time time, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id=? AND appointment_date=? "
                   + "AND appointment_time=? AND status='Scheduled' AND appointment_id<>?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, date);
            ps.setTime(3, time);
            ps.setInt(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    public List<Object[]> doctorsForCombo() throws SQLException {
        String sql = "SELECT doctor_id, CONCAT(doctor_id,' - ',full_name) FROM doctors ORDER BY full_name";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return DBConnection.rows(rs);
        }
    }

    /** Scheduled appointments that have no consultation yet. */
    public List<Object[]> pendingForCombo() throws SQLException {
        String sql = "SELECT a.appointment_id, CONCAT('Appt #',a.appointment_id,' - ',p.first_name,' ',p.last_name,"
                   + "' (',a.appointment_date,' ',a.appointment_time,')') "
                   + "FROM appointments a JOIN patients p ON a.patient_id=p.patient_id "
                   + "LEFT JOIN consultations c ON c.appointment_id=a.appointment_id "
                   + "WHERE a.status='Scheduled' AND c.consultation_id IS NULL "
                   + "ORDER BY a.appointment_date, a.appointment_time";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return DBConnection.rows(rs);
        }
    }

    private void bind(PreparedStatement ps, int patientId, int doctorId, Date date, Time time,
                      String status, String reason) throws SQLException {
        ps.setInt(1, patientId);
        ps.setInt(2, doctorId);
        ps.setDate(3, date);
        ps.setTime(4, time);
        ps.setString(5, status);
        ps.setString(6, reason);
    }
}
