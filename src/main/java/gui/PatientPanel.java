package gui;

import dao.PatientDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PatientPanel extends JPanel implements GuiUtil.Refreshable {

    private final PatientDAO dao = new PatientDAO();

    private final JTextField txtId = new JTextField(15), txtFirst = new JTextField(15),
            txtLast = new JTextField(15), txtBirth = new JTextField(15), txtContact = new JTextField(15),
            txtAddress = new JTextField(15), txtEmail = new JTextField(15), txtSearch = new JTextField(15);
    private final JComboBox<String> cmbSex = new JComboBox<>(new String[]{"Male", "Female"});
    private final DefaultTableModel model = GuiUtil.tableModel(
            "ID", "First Name", "Last Name", "Birth Date", "Sex", "Contact", "Address", "Email", "Registered");
    private final JTable table = new JTable(model);

    public PatientPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Patient Details"));
        GuiUtil.addRow(form, 0, 0, "Patient ID:", txtId);
        GuiUtil.addRow(form, 0, 1, "Birth Date (yyyy-MM-dd):", txtBirth);
        GuiUtil.addRow(form, 1, 0, "First Name:", txtFirst);
        GuiUtil.addRow(form, 1, 1, "Last Name:", txtLast);
        GuiUtil.addRow(form, 2, 0, "Sex:", cmbSex);
        GuiUtil.addRow(form, 2, 1, "Contact No.:", txtContact);
        GuiUtil.addRow(form, 3, 0, "Address:", txtAddress);
        GuiUtil.addRow(form, 3, 1, "Email:", txtEmail);

        JButton btnAdd = new JButton("Register"), btnUpdate = new JButton("Update"),
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

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadSelected(); });
        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clear());
        btnSearch.addActionListener(e -> refresh());
        btnAll.addActionListener(e -> { txtSearch.setText(""); refresh(); });
    }

    @Override
    public void refresh() {
        try {
            GuiUtil.fill(model, dao.getAll(txtSearch.getText().trim()));
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private boolean valid() {
        if (txtFirst.getText().trim().isEmpty() || txtLast.getText().trim().isEmpty()
                || txtBirth.getText().trim().isEmpty()) {
            GuiUtil.error(this, "First name, last name and birth date are required.");
            return false;
        }
        return true;
    }

    private void add() {
        if (!valid()) return;
        try {
            dao.insert(txtFirst.getText().trim(), txtLast.getText().trim(), GuiUtil.date(txtBirth.getText()),
                    (String) cmbSex.getSelectedItem(), txtContact.getText().trim(),
                    txtAddress.getText().trim(), txtEmail.getText().trim());
            GuiUtil.info(this, "Patient registered.");
            clear();
            refresh();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void update() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a patient from the table first."); return; }
        if (!valid()) return;
        try {
            dao.update(Integer.parseInt(txtId.getText()), txtFirst.getText().trim(), txtLast.getText().trim(),
                    GuiUtil.date(txtBirth.getText()), (String) cmbSex.getSelectedItem(),
                    txtContact.getText().trim(), txtAddress.getText().trim(), txtEmail.getText().trim());
            GuiUtil.info(this, "Patient updated.");
            clear();
            refresh();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void delete() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a patient from the table first."); return; }
        if (!GuiUtil.confirm(this, "Delete this patient? Their appointments, consultations and bills will also be deleted.")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            clear();
            refresh();
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void clear() {
        for (JTextField f : new JTextField[]{txtId, txtFirst, txtLast, txtBirth, txtContact, txtAddress, txtEmail})
            f.setText("");
        cmbSex.setSelectedIndex(0);
        table.clearSelection();
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtId.setText(GuiUtil.str(model.getValueAt(r, 0)));
        txtFirst.setText(GuiUtil.str(model.getValueAt(r, 1)));
        txtLast.setText(GuiUtil.str(model.getValueAt(r, 2)));
        txtBirth.setText(GuiUtil.str(model.getValueAt(r, 3)));
        cmbSex.setSelectedItem(GuiUtil.str(model.getValueAt(r, 4)));
        txtContact.setText(GuiUtil.str(model.getValueAt(r, 5)));
        txtAddress.setText(GuiUtil.str(model.getValueAt(r, 6)));
        txtEmail.setText(GuiUtil.str(model.getValueAt(r, 7)));
    }
}
