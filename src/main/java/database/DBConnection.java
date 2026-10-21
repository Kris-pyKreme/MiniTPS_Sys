package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBConnection {

    // >>> Change these to match your MySQL setup <<<
    private static final String URL  = "jdbc:mysql://localhost:3306/clinic_tps"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    /** Opens a new connection. Always use inside try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /** Converts a ResultSet into a list of row arrays (ready for a JTable model). */
    public static List<Object[]> rows(ResultSet rs) throws SQLException {
        int n = rs.getMetaData().getColumnCount();
        List<Object[]> list = new ArrayList<>();
        while (rs.next()) {
            Object[] row = new Object[n];
            for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
            list.add(row);
        }
        return list;
    }
}
