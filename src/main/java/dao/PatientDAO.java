package dao;

import database.DBConnection;
import security.*;
import validation.PatientInputs;
import java.sql.*;
import java.util.List;

public class PatientDAO {

    public void insert(String first, String last, Date birth, String sex,
                       String contact, String address, String email) throws SQLException {
        Session.require(Session.current().role.registersPatients());
        String phone = PatientInputs.contact(contact);
        String sql = "INSERT INTO patients (first_name,last_name,birth_date,sex,contact_number,address,email) "
                   + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, first, last, birth, sex, phone, address, email);
            ps.executeUpdate();
        }
    }

    public void update(int id, String first, String last, Date birth, String sex,
                       String contact, String address, String email) throws SQLException {
        Session.require(Session.current().role.registersPatients());
        String phone = PatientInputs.contact(contact);
        String sql = "UPDATE patients SET first_name=?, last_name=?, birth_date=?, sex=?, "
                   + "contact_number=?, address=?, email=? WHERE patient_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, first, last, birth, sex, phone, address, email);
            ps.setInt(8, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        Session.admin();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM patients WHERE patient_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Object[]> getAll(String kw) throws SQLException {
        Session.verified();
        String sql = "SELECT patient_id, first_name, last_name, birth_date, sex, contact_number, address, email, date_registered "
                   + "FROM patients WHERE (CONCAT(first_name,' ',last_name) LIKE ? OR contact_number LIKE ? "
                   + "OR CAST(patient_id AS CHAR) = ?) "
                   + patientScope() + " ORDER BY patient_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + kw + "%");
            ps.setString(2, "%" + kw + "%");
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) { return DBConnection.rows(rs); }
        }
    }

    /** {id, label} pairs for combo boxes. */
    public List<Object[]> forCombo() throws SQLException {
        Session.verified();
        String sql = "SELECT patient_id, CONCAT(patient_id,' - ',first_name,' ',last_name) "
                   + "FROM patients WHERE 1=1 " + patientScope() + " ORDER BY last_name, first_name";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return DBConnection.rows(rs);
        }
    }

    private String patientScope() {
        if (Session.current().role != Role.DOCTOR) return "";
        return " AND patient_id IN (SELECT a.patient_id FROM appointments a WHERE 1=1 "
                + Session.doctorFilter("a.doctor_id") + ")";
    }

    private void bind(PreparedStatement ps, String first, String last, Date birth, String sex,
                      String contact, String address, String email) throws SQLException {
        ps.setString(1, first);
        ps.setString(2, last);
        ps.setDate(3, birth);
        ps.setString(4, sex);
        ps.setString(5, contact);
        ps.setString(6, address);
        ps.setString(7, email);
    }
}
