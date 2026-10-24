package dao;

import database.DBConnection;
import security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class BillingDAO {

    /** Creates a bill from a consultation. Fee, patient and total are computed in SQL. */
    public void insert(int consultationId, BigDecimal other, String status, String method) throws SQLException {
        Session.require(Session.current().role.bills());
        if(other == null || other.signum()<0 || other.scale()>2) throw new IllegalArgumentException("Charges must be nonnegative with at most two decimals.");
        if(!"Unpaid".equals(status) && (method==null || method.isEmpty())) throw new IllegalArgumentException("Choose a payment method.");
        String sql = "INSERT INTO billing (patient_id,consultation_id,consultation_fee,other_charges,total_amount,"
                   + "payment_status,payment_method) "
                   + "SELECT a.patient_id, c.consultation_id, c.consultation_fee, ?, c.consultation_fee + ?, ?, ? "
                   + "FROM consultations c JOIN appointments a ON c.appointment_id=a.appointment_id "
                   + "WHERE c.consultation_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, other);
            ps.setBigDecimal(2, other);
            ps.setString(3, status);
            setMethod(ps, 4, method);
            ps.setInt(5, consultationId);
            ps.executeUpdate();
        }
    }

    public void update(int billId, BigDecimal other, String status, String method) throws SQLException {
        Session.require(Session.current().role.bills());
        if(other == null || other.signum()<0 || other.scale()>2) throw new IllegalArgumentException("Charges must be nonnegative with at most two decimals.");
        if(!"Unpaid".equals(status) && (method==null || method.isEmpty())) throw new IllegalArgumentException("Choose a payment method.");
        String sql = "UPDATE billing SET other_charges=?, total_amount=consultation_fee+?, "
                   + "payment_status=?, payment_method=? WHERE bill_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, other);
            ps.setBigDecimal(2, other);
            ps.setString(3, status);
            setMethod(ps, 4, method);
            ps.setInt(5, billId);
            ps.executeUpdate();
        }
    }

    public void delete(int billId) throws SQLException {
        Session.admin();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM billing WHERE bill_id=?")) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        }
    }

    public List<Object[]> getAll(String kw) throws SQLException {
        Session.require(Session.current().role.bills());
        String sql = "SELECT b.bill_id, b.consultation_id, CONCAT(p.patient_id,' - ',p.first_name,' ',p.last_name), "
                   + "b.consultation_fee, b.other_charges, b.total_amount, b.payment_status, b.payment_method, b.billing_date "
                   + "FROM billing b JOIN patients p ON b.patient_id=p.patient_id "
                   + "WHERE CONCAT(p.first_name,' ',p.last_name) LIKE ? OR b.payment_status LIKE ? "
                   + "ORDER BY b.bill_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + kw + "%");
            ps.setString(2, "%" + kw + "%");
            try (ResultSet rs = ps.executeQuery()) { return DBConnection.rows(rs); }
        }
    }

    /** Consultations that have no bill yet: {id, label, fee}. */
    public List<Object[]> unbilledForCombo() throws SQLException {
        Session.require(Session.current().role.bills());
        String sql = "SELECT c.consultation_id, CONCAT('Consult #',c.consultation_id,' - ',p.first_name,' ',p.last_name,"
                   + "' (PHP ',c.consultation_fee,')'), c.consultation_fee "
                   + "FROM consultations c JOIN appointments a ON c.appointment_id=a.appointment_id "
                   + "JOIN patients p ON a.patient_id=p.patient_id "
                   + "LEFT JOIN billing b ON b.consultation_id=c.consultation_id "
                   + "WHERE b.bill_id IS NULL ORDER BY c.consultation_id DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return DBConnection.rows(rs);
        }
    }

    private void setMethod(PreparedStatement ps, int idx, String method) throws SQLException {
        if (method == null || method.isEmpty()) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, method);
    }
}
