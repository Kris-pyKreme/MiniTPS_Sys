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
            "Please check your input:\n- Date format: YYYY-MM-DD (e.g. 2026-10-20)\n"
          + "- Time format: HH-MM (e.g. 14-30, using 24-hour time)\n- Amounts must be numbers (e.g. 500.00)";

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
        g.insets = new Insets(8, 0, 8, 16);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = col * 2;
        g.gridy = row;
        JLabel caption=Theme.label(label.replaceAll(":$", ""),13,Theme.MUTED,false);
        caption.setLabelFor(field);caption.setMinimumSize(caption.getPreferredSize());
        p.add(caption, g);
        g.gridx = col * 2 + 1;
        g.insets = new Insets(8, 0, 8, col == 0 ? 32 : 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        field.setFont(Theme.font(14,false));
        if(field instanceof JTextField || field instanceof JComboBox){
            int height=Theme.controlHeight(field,38);
            field.setPreferredSize(new Dimension(field instanceof JTextField?160:180,height));
            field.setMinimumSize(new Dimension(80,height));
        }
        if(field instanceof JScrollPane && ((JScrollPane)field).getViewport().getView() instanceof JTextArea){
            field.setPreferredSize(new Dimension(180,72));field.setMinimumSize(new Dimension(90,72));
        }
        p.add(field, g);
    }

    public static JPanel buttonBar(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);p.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        for (int i=0;i<buttons.length;i++) {
            JButton b=buttons[i];b.setFont(Theme.font(14,false));b.setBackground(Theme.WHITE);b.setForeground(Theme.INK);
            if(i==0){b.setBackground(Theme.TEAL);b.setForeground(Color.WHITE);b.setFont(Theme.font(14,true));}
            if("Delete".equals(b.getText()))b.setForeground(new Color(0xB34747));
            p.add(b);
        }
        return p;
    }

    public static JPanel searchBar(JTextField txt, JButton... buttons) {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));p.setOpaque(false);
        txt.putClientProperty("JTextField.placeholderText","Search records");
        int height=Theme.controlHeight(txt,38);txt.setPreferredSize(new Dimension(210,height));txt.setMinimumSize(new Dimension(80,height));
        txt.setColumns(16);if(buttons.length>0)txt.addActionListener(e->buttons[0].doClick());p.add(txt);
        for(JButton button:buttons){button.setFont(Theme.font(14,false));p.add(button);}return p;
    }

    public static void onChange(JTextComponent f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    public static java.sql.Date date(String s) { try { return java.sql.Date.valueOf(java.time.LocalDate.parse(s.trim())); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("Invalid date", e); } }

    public static java.sql.Time time(String s) {
        try {
            // HH-MM is the visible guide; Java's minute token is lowercase mm.
            // Retain colon input for records and workflows created before the guide changed.
            java.time.LocalTime t = java.time.LocalTime.parse(s.trim().replace('-', ':'), java.time.format.DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT));
            return java.sql.Time.valueOf(t);
        } catch(java.time.DateTimeException e) { throw new IllegalArgumentException("Invalid time",e); }
    }

    public static String str(Object o) { return o == null ? "" : o.toString(); }

    public static void info(Component c, String m)  { JOptionPane.showMessageDialog(c, m, "KlinikAccess", JOptionPane.INFORMATION_MESSAGE); }
    public static void error(Component c, String m) { JOptionPane.showMessageDialog(c, m, "KlinikAccess", JOptionPane.ERROR_MESSAGE); }
    public static boolean confirm(Component c, String m) {
        return JOptionPane.showConfirmDialog(c, m, "KlinikAccess", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
