package gui;

import dao.BillingDAO;
import gui.GuiUtil.Item;
import java.awt.*;
import security.*;
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
    private final JPanel recordsBody = new JPanel(new CardLayout());
    private final JLabel recordsCount = Theme.label("0 records", 13, Theme.MUTED, false);
    private final JLabel emptyTitle = Theme.label("No bills yet", 18, Theme.INK, true);
    private final JLabel emptyHint = Theme.label("Bills will appear here after a consultation is billed.", 13, Theme.MUTED, false);

    public BillingPanel() {
        setLayout(new BorderLayout(0, 18));
        setOpaque(false);
        txtId.setEditable(false);
        txtId.putClientProperty("JTextField.placeholderText", "Assigned automatically");
        txtFee.setEditable(false);
        txtTotal.setEditable(false);
        txtFee.putClientProperty("JTextField.placeholderText", "Filled from consultation");
        txtTotal.putClientProperty("JTextField.placeholderText", "Calculated automatically");
        txtOther.setText("0");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GuiUtil.addRow(form, 0, 0, "Bill ID", txtId);
        GuiUtil.addRow(form, 0, 1, "Unbilled consultation", cmbConsultation);
        GuiUtil.addRow(form, 1, 0, "Consultation fee (PHP)", txtFee);
        GuiUtil.addRow(form, 1, 1, "Other charges (PHP)", txtOther);
        GuiUtil.addRow(form, 2, 0, "Total (PHP)", txtTotal);
        GuiUtil.addRow(form, 2, 1, "Payment status", cmbStatus);
        GuiUtil.addRow(form, 3, 1, "Payment method", cmbMethod);

        JButton btnAdd = Theme.primary("Create bill"), btnUpdate = Theme.quiet("Save changes"),
                btnDelete = Theme.quiet("Delete"), btnClear = Theme.quiet("Clear"),
                btnSearch = Theme.quiet("Search"), btnAll = Theme.quiet("Reset");
        btnDelete.setForeground(new Color(0xAD3D3D));

        boolean editable = Session.current().role.bills();
        btnAdd.setVisible(editable); btnUpdate.setVisible(editable); btnClear.setVisible(editable);
        btnDelete.setVisible(Session.current().role == Role.ADMIN);
        JPanel formCard = Theme.card(new BorderLayout(0, 12));
        formCard.add(Theme.label("Bill details", 18, Theme.INK, true), BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);
        formCard.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnClear, btnDelete), BorderLayout.SOUTH);
        formCard.setVisible(editable);

        JPanel recordsCard = Theme.card(new BorderLayout(0, 14));
        JPanel recordsHeader = new JPanel(new BorderLayout(0, 12));recordsHeader.setOpaque(false);
        JPanel recordTitle = new JPanel(new BorderLayout());recordTitle.setOpaque(false);
        recordTitle.add(Theme.label("Billing records", 20, Theme.INK, true), BorderLayout.WEST);
        recordTitle.add(recordsCount, BorderLayout.EAST);
        recordsHeader.add(recordTitle, BorderLayout.NORTH);
        recordsHeader.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.SOUTH);
        recordsCard.add(recordsHeader, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.WHITE);
        table.setName("billRecords");
        recordsBody.setOpaque(false);recordsBody.add(scroll, "table");
        JPanel empty = new JPanel(new GridBagLayout());empty.setBackground(Theme.WHITE);
        JPanel emptyText = Theme.column();emptyTitle.setAlignmentX(CENTER_ALIGNMENT);emptyHint.setAlignmentX(CENTER_ALIGNMENT);
        emptyText.add(emptyTitle);emptyText.add(Box.createVerticalStrut(8));emptyText.add(emptyHint);empty.add(emptyText);
        recordsBody.add(empty, "empty");recordsCard.add(recordsBody, BorderLayout.CENTER);
        model.addTableModelListener(e -> updateRecordsState());
        add(formCard, BorderLayout.NORTH);add(recordsCard, BorderLayout.CENTER);
        updateRecordsState();Theme.polish(this);

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

    private void updateRecordsState() {
        int count = model.getRowCount();
        recordsCount.setText(count + (count == 1 ? " record" : " records"));
        boolean searching = !txtSearch.getText().trim().isEmpty();
        emptyTitle.setText(searching ? "No matching bills" : "No bills yet");
        emptyHint.setText(searching ? "Try a different search or reset the filter." : "Bills will appear here after a consultation is billed.");
        ((CardLayout) recordsBody.getLayout()).show(recordsBody, count == 0 ? "empty" : "table");
    }

    @Override
    public void refresh() {
        try {
            GuiUtil.fillCombo(cmbConsultation, dao.unbilledForCombo());
        } catch (SQLException | SecurityException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
        loadTable();
    }

    private void loadTable() {
        try {
            GuiUtil.fill(model, dao.getAll(txtSearch.getText().trim()));
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
