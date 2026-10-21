package gui;

import dao.AppointmentDAO;
import dao.PatientDAO;
import gui.GuiUtil.Item;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AppointmentPanel extends JPanel implements GuiUtil.Refreshable {

    private final AppointmentDAO dao = new AppointmentDAO();
    private final PatientDAO patientDao = new PatientDAO();

    private final JTextField txtId = new JTextField(15), txtDate = new JTextField(15),
            txtTime = new JTextField(15), txtReason = new JTextField(15), txtSearch = new JTextField(15);
    private final JComboBox<Item> cmbPatient = new JComboBox<>(), cmbDoctor = new JComboBox<>();
    private final JComboBox<String> cmbStatus =
            new JComboBox<>(new String[]{"Scheduled", "Completed", "Cancelled", "No-show"});
    private final DefaultTableModel model = GuiUtil.tableModel(
            "ID", "Patient", "Doctor", "Date", "Time", "Status", "Reason");
    private final JTable table = new JTable(model);

    public AppointmentPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Appointment Details"));
        GuiUtil.addRow(form, 0, 0, "Appointment ID:", txtId);
        GuiUtil.addRow(form, 0, 1, "Status:", cmbStatus);
        GuiUtil.addRow(form, 1, 0, "Patient:", cmbPatient);
        GuiUtil.addRow(form, 1, 1, "Doctor:", cmbDoctor);
        GuiUtil.addRow(form, 2, 0, "Date (yyyy-MM-dd):", txtDate);
        GuiUtil.addRow(form, 2, 1, "Time (HH:mm):", txtTime);
        GuiUtil.addRow(form, 3, 0, "Reason:", txtReason);

        JButton btnAdd = new JButton("Book Appointment"), btnUpdate = new JButton("Update"),
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
        btnAdd.addActionListener(e -> save(false));
        btnUpdate.addActionListener(e -> save(true));
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clear());
        btnSearch.addActionListener(e -> loadTable());
        btnAll.addActionListener(e -> { txtSearch.setText(""); loadTable(); });
    }

    /** Called when the tab is opened: reloads combos (new patients) and the table. */
    @Override
    public void refresh() {
        try {
            GuiUtil.fillCombo(cmbPatient, patientDao.forCombo());
            GuiUtil.fillCombo(cmbDoctor, dao.doctorsForCombo());
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

    private void save(boolean isUpdate) {
        if (isUpdate && txtId.getText().isEmpty()) {
            GuiUtil.error(this, "Select an appointment from the table first.");
            return;
        }
        Item p = (Item) cmbPatient.getSelectedItem();
        Item d = (Item) cmbDoctor.getSelectedItem();
        if (p == null || d == null) {
            GuiUtil.error(this, "Select a patient and a doctor (register a patient first if the list is empty).");
            return;
        }
        try {
            Date date = GuiUtil.date(txtDate.getText());
            Time time = GuiUtil.time(txtTime.getText());
            String status = (String) cmbStatus.getSelectedItem();
            int id = isUpdate ? Integer.parseInt(txtId.getText()) : 0;

            if ("Scheduled".equals(status) && dao.slotTaken(d.id, date, time, id)) {
                GuiUtil.error(this, "That doctor already has a scheduled appointment at this date and time.");
                return;
            }
            if (isUpdate) dao.update(id, p.id, d.id, date, time, status, txtReason.getText().trim());
            else          dao.insert(p.id, d.id, date, time, status, txtReason.getText().trim());

            GuiUtil.info(this, isUpdate ? "Appointment updated." : "Appointment booked.");
            clear();
            loadTable();
        } catch (IllegalArgumentException ex) {
            GuiUtil.error(this, GuiUtil.FORMAT_MSG);
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void delete() {
        if (txtId.getText().isEmpty()) { GuiUtil.error(this, "Select an appointment from the table first."); return; }
        if (!GuiUtil.confirm(this, "Delete this appointment? Its consultation and bill (if any) will also be deleted.")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            clear();
            loadTable();
        } catch (SQLException ex) {
            GuiUtil.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void clear() {
        txtId.setText("");
        txtDate.setText("");
        txtTime.setText("");
        txtReason.setText("");
        cmbStatus.setSelectedIndex(0);
        if (cmbPatient.getItemCount() > 0) cmbPatient.setSelectedIndex(0);
        if (cmbDoctor.getItemCount() > 0) cmbDoctor.setSelectedIndex(0);
        table.clearSelection();
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtId.setText(GuiUtil.str(model.getValueAt(r, 0)));
        GuiUtil.select(cmbPatient, GuiUtil.str(model.getValueAt(r, 1)));
        GuiUtil.select(cmbDoctor, GuiUtil.str(model.getValueAt(r, 2)));
        txtDate.setText(GuiUtil.str(model.getValueAt(r, 3)));
        String t = GuiUtil.str(model.getValueAt(r, 4));
        txtTime.setText(t.length() >= 5 ? t.substring(0, 5) : t);
        cmbStatus.setSelectedItem(GuiUtil.str(model.getValueAt(r, 5)));
        txtReason.setText(GuiUtil.str(model.getValueAt(r, 6)));
    }
}
