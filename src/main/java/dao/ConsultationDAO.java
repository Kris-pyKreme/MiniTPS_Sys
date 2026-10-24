package dao;

import database.DBConnection;
import security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class ConsultationDAO {

    /**
     * ATOMIC TRANSACTION: inserts the consultation AND marks the appointment Completed.
     * Either both succeed (commit) or neither does (rollback).
     */
    public void save(int appointmentId, String diagnosis, String notes,
                     String prescription, BigDecimal fee) throws SQLException {
        Session.require(Session.current().role.consults());
        Session.appointment(appointmentId);
        validate(fee);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO consultations (appointment_id,diagnosis,treatment_notes,prescription,consultation_fee) "
                  + "VALUES (?,?,?,?,?)");
                 PreparedStatement upd = c.prepareStatement(
                    "UPDATE appointments SET status='Completed' WHERE appointment_id=?")) {
                try (PreparedStatement check=c.prepareStatement("SELECT status FROM appointments WHERE appointment_id=? FOR UPDATE")) {
                    check.setInt(1,appointmentId);
                    try(ResultSet r=check.executeQuery()) {
                        if(!r.next() || !"Scheduled".equals(r.getString(1))) throw new SQLException("Only a scheduled appointment can be consulted. Refresh the appointment list.");
                    }
                }
                ins.setInt(1, appointmentId);
                ins.setString(2, diagnosis);
                ins.setString(3, notes);
                ins.setString(4, prescription);
                ins.setBigDecimal(5, fee);
                ins.executeUpdate();

                upd.setInt(1, appointmentId);
                upd.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void update(int consultationId, String diagnosis, String notes,
                       String prescription, BigDecimal fee) throws SQLException {
        Session.require(Session.current().role.consults());
        Session.consultation(consultationId);
        validate(fee);
        String sql = "UPDATE consultations SET diagnosis=?, treatment_notes=?, prescription=?, "
                   + "consultation_fee=? WHERE consultation_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, diagnosis);
            ps.setString(2, notes);
            ps.setString(3, prescription);
            ps.setBigDecimal(4, fee);
            ps.setInt(5, consultationId);
            c.setAutoCommit(false);
            try {
                ps.executeUpdate();
                try(PreparedStatement bill=c.prepareStatement("UPDATE billing SET consultation_fee=?,total_amount=?+other_charges WHERE consultation_id=?")) {
                    bill.setBigDecimal(1,fee); bill.setBigDecimal(2,fee); bill.setInt(3,consultationId); bill.executeUpdate();
                }
                c.commit();
            } catch(SQLException e) { c.rollback(); throw e; }
        }
    }

    /** ATOMIC TRANSACTION: reopens the appointment and deletes the consultation (and its bill via cascade). */
    public void delete(int consultationId) throws SQLException {
        Session.admin();
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement reopen = c.prepareStatement(
                    "UPDATE appointments SET status='Scheduled' WHERE appointment_id="
                  + "(SELECT appointment_id FROM consultations WHERE consultation_id=?)");
                 PreparedStatement del = c.prepareStatement(
                    "DELETE FROM consultations WHERE consultation_id=?")) {
                reopen.setInt(1, consultationId);
                reopen.executeUpdate();
                del.setInt(1, consultationId);
                del.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Object[]> getAll(String kw) throws SQLException {
        Session.require(Session.current().role.consults());
        String sql = "SELECT c.consultation_id, c.appointment_id, CONCAT(p.patient_id,' - ',p.first_name,' ',p.last_name), "
                   + "d.full_name, c.diagnosis, c.treatment_notes, c.prescription, c.consultation_fee, c.date_recorded "
                   + "FROM consultations c JOIN appointments a ON c.appointment_id=a.appointment_id "
                   + "JOIN patients p ON a.patient_id=p.patient_id JOIN doctors d ON a.doctor_id=d.doctor_id "
                   + "WHERE (CONCAT(p.first_name,' ',p.last_name) LIKE ? OR c.diagnosis LIKE ?) "
                   + Session.doctorFilter("a.doctor_id")
                   + "ORDER BY c.consultation_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + kw + "%");
            ps.setString(2, "%" + kw + "%");
            try (ResultSet rs = ps.executeQuery()) { return DBConnection.rows(rs); }
        }
    }
    private void validate(BigDecimal fee) {
        if(fee == null || fee.signum()<0 || fee.scale()>2) throw new IllegalArgumentException("Fee must be nonnegative with at most two decimals.");
    }
}
