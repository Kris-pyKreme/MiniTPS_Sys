package gui;

import dao.BillingDAO;
import gui.GuiUtil.Item;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BillingPanel extends JPanel implements GuiUtil.Refreshable {

    private final BillingDAO dao = new BillingDAO();

    private final JTextField txtId = new JTextField(15), txtFee = new JTextField(15), txtOther = new JTextField(15),
            txtTotal = new JTextField(15), txtSearch = new JTextField(15);
    private final JComboBox<Item> cmbConsultation = new JComboBox<>();
    private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Unpaid", "Paid", "Partial"});
    private final JComboBox<String> cmbMethod = new JComboBox<>(new String[]{"", "Cash", "Card", "GCash", "Insurance"});
    private final DefaultTableModel model = GuiUtil.tableModel(
            "Bill ID", "Consult ID", "Patient", "Fee", "Other Charges", "Total", "Status", "Method", "Billed On");
    private final JTable table = new JTable(model);

    public BillingPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtId.setEditable(false);
        txtFee.setEditable(false);
        txtTotal.setEditable(false);
        txtOther.setText("0");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Billing Details"));
        GuiUtil.addRow(form, 0, 0, "Bill ID:", txtId);
        GuiUtil.addRow(form, 0, 1, "Consultation (unbilled):", cmbConsultation);
        GuiUtil.addRow(form, 1, 0, "Consultation Fee (PHP):", txtFee);
        GuiUtil.addRow(form, 1, 1, "Other Charges (PHP):", txtOther);
        GuiUtil.addRow(form, 2, 0, "Total (PHP):", txtTotal);
        GuiUtil.addRow(form, 2, 1, "Payment Status:", cmbStatus);
        GuiUtil.addRow(form, 3, 1, "Payment Method:", cmbMethod);

        JButton btnAdd = new JButton("Create Bill"), btnUpdate = new JButton("Update"),
                btnDelete = new JButton("Delete"), btnClear = new JButton("Clear"),
                btnSearch = new JButton("Search"), btnAll = new JButton("Show All");

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnDelete, btnClear), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Auto-fill fee when a consultation is picked; keep total live as charges change
        cmbConsultation.addActionListener(e -> {
            Item i = (Item) cmbConsultation.getSelectedItem();
            if (i != null && i.extra != null) txtFee.setText(i.extra.toString());
        });
        GuiUtil.onChange(txtFee, this::recalc);
        GuiUtil.onChange(txtOther, this::recalc);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadSelected(); });
        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clear());
        btnSearch.addActionListener(e -> loadTable());
        btnAll.addActionListener(e -> { txtSearch.setText(""); loadTable(); });
    }

    @Override
    public void refresh() {
        try {
            GuiUtil.fillCombo(cmbConsultation, dao.unbilledForCombo());
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
        loadTable();
    }

    private void loadTable() {
        try {
            GuiUtil.fill(model, dao.getAll(txtSearch.getText().trim()));
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void recalc() {
        try {
            BigDecimal fee = new BigDecimal(txtFee.getText().trim());
            String o = txtOther.getText().trim();
            txtTotal.setText(fee.add(o.isEmpty() ? BigDecimal.ZERO : new BigDecimal(o)).toString());
        } catch (NumberFormatException ex) {
            txtTotal.setText("");
        }
    }

    private boolean paymentValid() {
        String status = (String) cmbStatus.getSelectedItem();
        String method = (String) cmbMethod.getSelectedItem();
        if (!"Unpaid".equals(status) && (method == null || method.isEmpty())) {
            GuiUtil.error(this, "Choose a payment method for Paid or Partial bills.");
            return false;
        }
        return true;
    }

    private void add() {
        Item c = (Item) cmbConsultation.getSelectedItem();
        if (c == null) { GuiUtil.error(this, "No unbilled consultation available."); return; }
        if (!paymentValid()) return;
        try {
            BigDecimal other = new BigDecimal(txtOther.getText().trim());
            dao.insert(c.id, other, (String) cmbStatus.getSelectedItem(), (String) cmbMethod.getSelectedItem());
            GuiUtil.info(this, "Bill created.");
            clear();
            refresh();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void update() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a bill from the table first."); return; }
        if (!paymentValid()) return;
        try {
            dao.update(Integer.parseInt(txtId.getText()), new BigDecimal(txtOther.getText().trim()),
                    (String) cmbStatus.getSelectedItem(), (String) cmbMethod.getSelectedItem());
            GuiUtil.info(this, "Bill updated.");
            clear();
            loadTable();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void delete() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a bill from the table first."); return; }
        if (!GuiUtil.confirm(this, "Delete this bill?")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            clear();
            refresh();
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void clear() {
        txtId.setText("");
        txtOther.setText("0");
        cmbStatus.setSelectedIndex(0);
        cmbMethod.setSelectedIndex(0);
        table.clearSelection();
        Item i = (Item) cmbConsultation.getSelectedItem();
        txtFee.setText(i != null && i.extra != null ? i.extra.toString() : "");
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtId.setText(GuiUtil.str(model.getValueAt(r, 0)));
        txtFee.setText(GuiUtil.str(model.getValueAt(r, 3)));
        txtOther.setText(GuiUtil.str(model.getValueAt(r, 4)));
        cmbStatus.setSelectedItem(GuiUtil.str(model.getValueAt(r, 6)));
        cmbMethod.setSelectedItem(GuiUtil.str(model.getValueAt(r, 7)));
    }
}
