import database.DBConnection;
import gui.Login;
import java.sql.Connection;
import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }

        // Fail early with a clear message if MySQL isn't reachable
        try (Connection c = DBConnection.getConnection()) {
            // connection OK
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the database.\n\nCheck that MySQL is running, schema.sql has been run,\n"
                  + "and the URL/user/password in DBConnection.java are correct.\n\n" + ex.getMessage(),
                    "Clinic TPS", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
