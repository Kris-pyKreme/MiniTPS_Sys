package gui;

import dao.AppointmentDAO;
import dao.ConsultationDAO;
import gui.GuiUtil.Item;
import java.awt.*;
import security.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ConsultationPanel extends JPanel implements GuiUtil.Refreshable {

    private final ConsultationDAO dao = new ConsultationDAO();
    private final AppointmentDAO appointmentDao = new AppointmentDAO();

    private final JTextField txtId = new JTextField(15), txtFee = new JTextField(15), txtSearch = new JTextField(15);
    private final JComboBox<Item> cmbAppointment = new JComboBox<>();
    private final JTextArea txtDiagnosis = new JTextArea(3, 20), txtNotes = new JTextArea(3, 20),
            txtPrescription = new JTextArea(3, 20);
    private final DefaultTableModel model = GuiUtil.tableModel(
            "ID", "Appt ID", "Patient", "Doctor", "Diagnosis", "Treatment Notes", "Prescription", "Fee", "Recorded");
    private final JTable table = new JTable(model);

    public ConsultationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtId.setEditable(false);
        for (JTextArea a : new JTextArea[]{txtDiagnosis, txtNotes, txtPrescription}) {
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Consultation Details"));
        GuiUtil.addRow(form, 0, 0, "Consultation ID:", txtId);
        GuiUtil.addRow(form, 0, 1, "Appointment (pending):", cmbAppointment);
        GuiUtil.addRow(form, 1, 0, "Consultation Fee (PHP):", txtFee);
        GuiUtil.addRow(form, 2, 0, "Diagnosis:", new JScrollPane(txtDiagnosis));
        GuiUtil.addRow(form, 2, 1, "Treatment Notes:", new JScrollPane(txtNotes));
        GuiUtil.addRow(form, 3, 0, "Prescription:", new JScrollPane(txtPrescription));

        JButton btnAdd = new JButton("Save Consultation"), btnUpdate = new JButton("Update"),
                btnDelete = new JButton("Delete"), btnClear = new JButton("Clear"),
                btnSearch = new JButton("Search"), btnAll = new JButton("Show All");

        boolean editable = Session.current().role.consults();
        btnAdd.setVisible(editable); btnUpdate.setVisible(editable); btnClear.setVisible(editable);
        btnDelete.setVisible(Session.current().role == Role.ADMIN);
        if (!editable) form.setVisible(false);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnDelete, btnClear), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        Theme.polish(this);

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
            GuiUtil.fillCombo(cmbAppointment, appointmentDao.pendingForCombo());
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

    private void add() {
        Item appt = (Item) cmbAppointment.getSelectedItem();
        if (appt == null) {
            GuiUtil.error(this, "No pending appointment selected. Book a Scheduled appointment first.");
            return;
        }
        try {
            BigDecimal fee = new BigDecimal(txtFee.getText().trim());
            dao.save(appt.id, txtDiagnosis.getText().trim(), txtNotes.getText().trim(),
                    txtPrescription.getText().trim(), fee);
            GuiUtil.info(this, "Consultation saved. The appointment is now marked Completed.");
            clear();
            refresh();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException | SecurityException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void update() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a consultation from the table first."); return; }
        try {
            dao.update(Integer.parseInt(txtId.getText()), txtDiagnosis.getText().trim(), txtNotes.getText().trim(),
                    txtPrescription.getText().trim(), new BigDecimal(txtFee.getText().trim()));
            GuiUtil.info(this, "Consultation updated.");
            clear();
            loadTable();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException | SecurityException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void delete() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select a consultation from the table first."); return; }
        if (!GuiUtil.confirm(this, "Delete this consultation? Its bill will be deleted and the appointment set back to Scheduled.")) return;
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
        txtFee.setText("");
        txtDiagnosis.setText("");
        txtNotes.setText("");
        txtPrescription.setText("");
        table.clearSelection();
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtId.setText(GuiUtil.str(model.getValueAt(r, 0)));
        txtDiagnosis.setText(GuiUtil.str(model.getValueAt(r, 4)));
        txtNotes.setText(GuiUtil.str(model.getValueAt(r, 5)));
        txtPrescription.setText(GuiUtil.str(model.getValueAt(r, 6)));
        txtFee.setText(GuiUtil.str(model.getValueAt(r, 7)));
    }
}
