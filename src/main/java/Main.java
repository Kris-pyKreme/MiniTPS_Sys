import database.DatabaseSetup;
import gui.*;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Manila"));
        Theme.install();
        try {
            DatabaseSetup.upgrade();
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(null,"Cannot prepare the clinic database.\nStart MySQL in XAMPP and import database/schema.sql and database/staff_users.sql.\n"
                    + "Check db.properties if your connection differs from localhost / root.\n\n"+ex.getMessage(),"KlinikAccess",JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(()->new Login().setVisible(true));
    }
}
