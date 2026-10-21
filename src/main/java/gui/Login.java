package gui;

import database.DBConnection;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;

/**
 * Staff login with a split layout: brand panel (left) + form (right).
 * Opens MainFrame only after the credentials are verified against staff_users.
 */
public class Login extends JFrame {

    private static final int MAX_ATTEMPTS = 3;
    private static final Color TEAL        = new Color(0x035B63);
    private static final Color TEAL_LIGHT  = new Color(0x1E8A7E);
    private static final Color ORANGE      = new Color(0xF26B2B);
    private static final Color FIELD_LINE  = new Color(0xC8C8C8);

    private final JTextField txtUser = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();
    private final JCheckBox chkShow = new JCheckBox("Show password");
    private final FlatButton btnLogin = new FlatButton("LOGIN", TEAL, new Color(0x047A85));
    private final char defaultEcho = txtPass.getEchoChar();
    private int attempts = 0;

    public Login() {
        super("Clinic TPS - Staff Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setPreferredSize(new Dimension(760, 410));
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
        setContentPane(root);

        getRootPane().setDefaultButton(btnLogin);   // Enter key = Login
        chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho));
        btnLogin.addActionListener(e -> doLogin());

        pack();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- left side
    private JPanel buildBrandPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(TEAL);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        LogoPanel logo = new LogoPanel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel("CLINIC TPS");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 26f));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tag = new JLabel("Appointment & Billing System");
        tag.setFont(tag.getFont().deriveFont(12f));
        tag.setForeground(new Color(0xBFE3E6));
        tag.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(logo);
        center.add(Box.createVerticalStrut(14));
        center.add(name);
        center.add(Box.createVerticalStrut(4));
        center.add(tag);
        center.add(Box.createVerticalGlue());

        JLabel copy = new JLabel("\u00A9 Clinic TPS. All rights reserved.", SwingConstants.CENTER);
        copy.setFont(copy.getFont().deriveFont(11f));
        copy.setForeground(new Color(0x8FBFC4));
        copy.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        p.add(center, BorderLayout.CENTER);
        p.add(copy, BorderLayout.SOUTH);
        return p;
    }

    // --------------------------------------------------------------- right side
    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        int y = 0;

        JLabel title = new JLabel("STAFF LOGIN", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        g.gridy = y++; g.insets = new Insets(0, 0, 26, 0);
        p.add(title, g);

        g.gridy = y++; g.insets = new Insets(0, 0, 4, 0);
        p.add(fieldLabel("Username"), g);
        styleField(txtUser);
        g.gridy = y++; g.insets = new Insets(0, 0, 14, 0);
        p.add(txtUser, g);

        g.gridy = y++; g.insets = new Insets(0, 0, 4, 0);
        p.add(fieldLabel("Password"), g);
        styleField(txtPass);
        g.gridy = y++; g.insets = new Insets(0, 0, 6, 0);
        p.add(txtPass, g);

        chkShow.setOpaque(false);
        chkShow.setFocusPainted(false);
        chkShow.setFont(chkShow.getFont().deriveFont(11f));
        g.gridy = y++; g.insets = new Insets(0, 0, 14, 0);
        p.add(chkShow, g);

        btnLogin.setPreferredSize(new Dimension(0, 40));
        g.gridy = y++; g.insets = new Insets(0, 0, 14, 0);
        p.add(btnLogin, g);

        JLabel hint = new JLabel("Forgot your password? Contact the administrator.");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        g.gridy = y++; g.insets = new Insets(0, 0, 0, 0);
        p.add(hint, g);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(12f));
        return l;
    }

    private void styleField(JTextField f) {
        f.setPreferredSize(new Dimension(0, 36));
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_LINE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    // ------------------------------------------------------------------- logic
    private void doLogin() {
        String user = txtUser.getText().trim();
        char[] pass = txtPass.getPassword();
        try {
            if (user.isEmpty() || pass.length == 0) {
                GuiUtil.error(this, "Enter your username and password.");
                return;
            }
            String[] staff = authenticate(user, new String(pass));
            if (staff != null) {
                dispose();
                MainFrame main = new MainFrame();
                main.setTitle(main.getTitle() + "  |  Logged in: " + staff[0] + " (" + staff[1] + ")");
                main.setVisible(true);
            } else {
                attempts++;
                txtPass.setText("");
                if (attempts >= MAX_ATTEMPTS) {
                    GuiUtil.error(this, "Too many failed attempts. The application will now close.");
                    System.exit(0);
                }
                GuiUtil.error(this, "Invalid username or password. Attempts left: " + (MAX_ATTEMPTS - attempts));
            }
        } catch (SQLException ex) {
            GuiUtil.error(this, "Cannot reach the database.\nCheck MySQL is running and DBConnection.java settings.\n\n"
                    + ex.getMessage());
        } finally {
            java.util.Arrays.fill(pass, '\0');   // wipe password from memory
        }
    }

    /** Returns {full_name, role} if the credentials match, otherwise null. */
    private String[] authenticate(String username, String password) throws SQLException {
        String sql = "SELECT full_name, role FROM staff_users "
                   + "WHERE username=? AND password_hash=SHA2(CONCAT(salt,?),256)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new String[]{rs.getString(1), rs.getString(2)} : null;
            }
        }
    }

    // ------------------------------------------------------------ custom pieces
    /** Simple drawn logo (medical cross) so no image file is needed. Swap for an ImageIcon if you have one. */
    private static class LogoPanel extends JPanel {
        LogoPanel() {
            setOpaque(false);
            Dimension d = new Dimension(120, 120);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g.setColor(TEAL_LIGHT);
            g.fillOval(cx - 54, cy - 54, 108, 108);
            g.setColor(ORANGE);
            g.fillRoundRect(cx - 14, cy - 40, 28, 80, 12, 12);   // vertical bar
            g.fillRoundRect(cx - 40, cy - 14, 80, 28, 12, 12);   // horizontal bar
            g.dispose();
        }
    }

    /** Flat rounded button (Nimbus ignores setBackground on normal buttons, so we paint it ourselves). */
    private static class FlatButton extends JButton {
        private final Color base, hover;
        private boolean over;

        FlatButton(String text, Color base, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { over = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(over ? hover : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g.setColor(getForeground());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(getText(), x, y);
            g.dispose();
        }
    }
}
