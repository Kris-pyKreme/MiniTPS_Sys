package gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

/** Small shared helpers so the panels stay short. */
public class GuiUtil {

    public static final String FORMAT_MSG =
            "Please check your input:\n- Date format: yyyy-MM-dd (e.g. 2026-10-20)\n"
          + "- Time format: HH:mm (e.g. 14:30)\n- Amounts must be numbers (e.g. 500.00)";

    /** Combo box entry: an id plus a display label (and an optional extra value). */
    public static class Item {
        public final int id;
        public final String label;
        public final Object extra;
        public Item(int id, String label, Object extra) { this.id = id; this.label = label; this.extra = extra; }
        @Override public String toString() { return label; }
    }

    /** Implemented by each tab so MainFrame can reload data when the tab is opened. */
    public interface Refreshable { void refresh(); }

    public static DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static void fill(DefaultTableModel m, List<Object[]> rows) {
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    public static void fillCombo(JComboBox<Item> cb, List<Object[]> rows) {
        cb.removeAllItems();
        for (Object[] r : rows) {
            cb.addItem(new Item((Integer) r[0], String.valueOf(r[1]), r.length > 2 ? r[2] : null));
        }
    }

    public static void select(JComboBox<Item> cb, String label) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).label.equals(label)) { cb.setSelectedIndex(i); return; }
        }
        cb.setSelectedIndex(-1);
    }

    /** Adds "label : field" to a GridBagLayout panel; col 0 or 1 gives a two-column form. */
    public static void addRow(JPanel p, int row, int col, String label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = col * 2;
        g.gridy = row;
        p.add(new JLabel(label), g);
        g.gridx = col * 2 + 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        p.add(field, g);
    }

    public static JPanel buttonBar(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        for (JButton b : buttons) p.add(b);
        return p;
    }

    public static JPanel searchBar(JTextField txt, JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.add(new JLabel("Search:"));
        p.add(txt);
        for (JButton b : buttons) p.add(b);
        return p;
    }

    public static void onChange(JTextComponent f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    public static java.sql.Date date(String s) { return java.sql.Date.valueOf(s.trim()); }

    public static java.sql.Time time(String s) {
        s = s.trim();
        if (s.length() == 5) s += ":00";
        return java.sql.Time.valueOf(s);
    }

    public static String str(Object o) { return o == null ? "" : o.toString(); }

    public static void info(Component c, String m)  { JOptionPane.showMessageDialog(c, m, "Clinic TPS", JOptionPane.INFORMATION_MESSAGE); }
    public static void error(Component c, String m) { JOptionPane.showMessageDialog(c, m, "Clinic TPS", JOptionPane.ERROR_MESSAGE); }
    public static boolean confirm(Component c, String m) {
        return JOptionPane.showConfirmDialog(c, m, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
