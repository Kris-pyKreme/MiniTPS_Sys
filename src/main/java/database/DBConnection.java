package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBConnection {

    private static final java.util.Properties CONFIG = loadConfig();
    private static java.util.Properties loadConfig() {
        java.util.Properties p = new java.util.Properties();
        java.nio.file.Path path = java.nio.file.Paths.get(System.getProperty("clinic.config", "db.properties"));
        if (java.nio.file.Files.exists(path)) {
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) { p.load(in); }
            catch (java.io.IOException e) { throw new IllegalStateException("Cannot read database configuration", e); }
        }
        return p;
    }
    private static String setting(String key, String env, String fallback) {
        String value = System.getProperty(key);
        if (value == null) value = System.getenv(env);
        return value == null ? CONFIG.getProperty(key, fallback) : value;
    }

    /** Opens a new connection. Always use inside try-with-resources. */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
                setting("clinic.db.url", "CLINIC_DB_URL", "jdbc:mysql://localhost:3306/clinic_tps?serverTimezone=Asia/Manila"),
                setting("clinic.db.user", "CLINIC_DB_USER", "root"),
                setting("clinic.db.password", "CLINIC_DB_PASSWORD", ""));
        // Align MySQL TIMESTAMP values with the calendar's Philippine time zone.
        // A numeric offset also works in XAMPP without MySQL time-zone tables.
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+08:00'");
        } catch (SQLException ex) { connection.close(); throw ex; }
        return connection;
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
