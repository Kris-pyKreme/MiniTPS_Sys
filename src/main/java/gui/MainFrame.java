package gui;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Clinic Appointment and Billing Transaction System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Clinic Appointment & Billing TPS");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 20f));
        header.setOpaque(true);
        header.setBackground(new Color(0x1F6F8B));
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        GuiUtil.Refreshable[] panels = {
            new PatientPanel(), new AppointmentPanel(), new ConsultationPanel(), new BillingPanel()
        };
        String[] names = {"1. Patient Registration", "2. Appointments", "3. Consultation", "4. Billing"};

        JTabbedPane tabs = new JTabbedPane();
        for (int i = 0; i < panels.length; i++) tabs.addTab(names[i], (JComponent) panels[i]);

        // Reload data every time a tab is opened so dropdowns always show current records
        tabs.addChangeListener(e -> panels[tabs.getSelectedIndex()].refresh());
        panels[0].refresh();

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
