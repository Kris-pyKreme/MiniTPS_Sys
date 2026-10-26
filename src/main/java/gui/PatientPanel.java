package gui;

import dao.PatientDAO;
import java.awt.*;
import security.*;
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
    private final JPanel recordsBody = new JPanel(new CardLayout());
    private final JLabel recordsCount = Theme.label("0 records", 13, Theme.MUTED, false);
    private final JLabel emptyTitle = Theme.label("No patients yet", 18, Theme.INK, true);
    private final JLabel emptyHint = Theme.label("Registered patients will appear here.", 13, Theme.MUTED, false);

    public PatientPanel() {
        setLayout(new BorderLayout(0, 18));
        setOpaque(false);
        txtId.setEditable(false);
        txtId.putClientProperty("JTextField.placeholderText", "Assigned automatically");
        txtBirth.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        txtContact.putClientProperty("JTextField.placeholderText", "Contact number");
        txtEmail.putClientProperty("JTextField.placeholderText", "name@example.com");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GuiUtil.addRow(form, 0, 0, "Patient ID", txtId);
        GuiUtil.addRow(form, 0, 1, "Birth date", txtBirth);
        GuiUtil.addRow(form, 1, 0, "First name", txtFirst);
        GuiUtil.addRow(form, 1, 1, "Last name", txtLast);
        GuiUtil.addRow(form, 2, 0, "Sex", cmbSex);
        GuiUtil.addRow(form, 2, 1, "Contact number", txtContact);
        GuiUtil.addRow(form, 3, 0, "Address", txtAddress);
        GuiUtil.addRow(form, 3, 1, "Email", txtEmail);

        JButton btnAdd = Theme.primary("Register patient"), btnUpdate = Theme.quiet("Save changes"),
                btnDelete = Theme.quiet("Delete"), btnClear = Theme.quiet("Clear"),
                btnSearch = Theme.quiet("Search"), btnAll = Theme.quiet("Reset");
        btnDelete.setForeground(new Color(0xAD3D3D));

        boolean editable = Session.current().role.registersPatients();
        btnAdd.setVisible(editable); btnUpdate.setVisible(editable); btnClear.setVisible(editable);
        btnDelete.setVisible(Session.current().role == Role.ADMIN);
        JPanel formCard = Theme.card(new BorderLayout(0, 12));
        formCard.add(Theme.label("Patient details", 18, Theme.INK, true), BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);
        formCard.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnClear, btnDelete), BorderLayout.SOUTH);
        formCard.setVisible(editable);

        JPanel recordsCard = Theme.card(new BorderLayout(0, 14));
        JPanel recordsHeader = new JPanel(new BorderLayout(0, 12));recordsHeader.setOpaque(false);
        JPanel recordTitle = new JPanel(new BorderLayout());recordTitle.setOpaque(false);
        recordTitle.add(Theme.label("Patient records", 18, Theme.INK, true), BorderLayout.WEST);
        recordTitle.add(recordsCount, BorderLayout.EAST);
        recordsHeader.add(recordTitle, BorderLayout.NORTH);
        recordsHeader.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.SOUTH);
        recordsCard.add(recordsHeader, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.WHITE);
        table.setName("patientRecords");
        recordsBody.setOpaque(false);recordsBody.add(scroll, "table");
        JPanel empty = new JPanel(new GridBagLayout());empty.setBackground(Theme.WHITE);
        JPanel emptyText = Theme.column();emptyTitle.setAlignmentX(CENTER_ALIGNMENT);emptyHint.setAlignmentX(CENTER_ALIGNMENT);
        emptyText.add(emptyTitle);emptyText.add(Box.createVerticalStrut(8));emptyText.add(emptyHint);empty.add(emptyText);
        recordsBody.add(empty, "empty");recordsCard.add(recordsBody, BorderLayout.CENTER);
        model.addTableModelListener(e -> updateRecordsState());
        add(formCard, BorderLayout.NORTH);add(recordsCard, BorderLayout.CENTER);
        updateRecordsState();Theme.polish(this);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadSelected(); });
        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clear());
        btnSearch.addActionListener(e -> refresh());
        btnAll.addActionListener(e -> { txtSearch.setText(""); refresh(); });
    }

    private void updateRecordsState() {
        int count = model.getRowCount();
        recordsCount.setText(count + (count == 1 ? " record" : " records"));
        boolean searching = !txtSearch.getText().trim().isEmpty();
        emptyTitle.setText(searching ? "No matching patients" : "No patients yet");
        emptyHint.setText(searching ? "Try a different search or reset the filter." : Session.current().role == Role.DOCTOR
                ? "Patients assigned to your appointments will appear here." : "Registered patients will appear here.");
        ((CardLayout) recordsBody.getLayout()).show(recordsBody, count == 0 ? "empty" : "table");
    }

    @Override
    public void refresh() {
        try {
            GuiUtil.fill(model, dao.getAll(txtSearch.getText().trim()));
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
