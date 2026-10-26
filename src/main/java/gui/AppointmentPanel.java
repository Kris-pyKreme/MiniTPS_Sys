package gui;

import dao.AppointmentDAO;
import dao.PatientDAO;
import gui.GuiUtil.Item;
import java.awt.*;
import security.*;
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
            "ID", "Patient", "Doctor", "Date", "Time", "Status", "Reason", "Booked On");
    private final JTable table = new JTable(model);
    private final JPanel recordsBody = new JPanel(new CardLayout());
    private final JLabel recordsCount = Theme.label("0 records", 13, Theme.MUTED, false);
    private final JLabel emptyTitle = Theme.label("No appointments yet", 18, Theme.INK, true);
    private final JLabel emptyHint = Theme.label("Appointments will appear here once they are booked.", 13, Theme.MUTED, false);

    public AppointmentPanel() {
        setLayout(new BorderLayout(0, 18));
        setOpaque(false);
        txtId.setEditable(false);
        txtId.putClientProperty("JTextField.placeholderText", "Assigned automatically");
        txtDate.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        txtTime.putClientProperty("JTextField.placeholderText", "HH-MM");
        txtDate.setToolTipText("Appointment date: YYYY-MM-DD");
        txtTime.setToolTipText("24-hour time: HH-MM, for example 14-30");
        txtReason.putClientProperty("JTextField.placeholderText", "Reason for visit");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GuiUtil.addRow(form, 0, 0, "Appointment ID", txtId);
        GuiUtil.addRow(form, 0, 1, "Status", cmbStatus);
        GuiUtil.addRow(form, 1, 0, "Patient", cmbPatient);
        GuiUtil.addRow(form, 1, 1, "Doctor", cmbDoctor);
        GuiUtil.addRow(form, 2, 0, "Date", txtDate);
        GuiUtil.addRow(form, 2, 1, "Time", txtTime);
        GuiUtil.addRow(form, 3, 0, "Reason", txtReason);

        JButton btnAdd = Theme.primary("Book appointment"), btnUpdate = Theme.quiet("Save changes"),
                btnDelete = Theme.quiet("Delete"), btnClear = Theme.quiet("Clear"),
                btnSearch = Theme.quiet("Search"), btnAll = Theme.quiet("Reset");
        btnDelete.setForeground(new Color(0xAD3D3D));

        boolean editable = Session.current().role.booksAppointments();
        btnAdd.setVisible(editable); btnUpdate.setVisible(editable); btnClear.setVisible(editable);
        btnDelete.setVisible(Session.current().role == Role.ADMIN);
        JPanel formCard = Theme.card(new BorderLayout(0, 12));
        formCard.add(Theme.label("Appointment details", 18, Theme.INK, true), BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);
        formCard.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnClear, btnDelete), BorderLayout.SOUTH);
        formCard.setVisible(editable);

        JPanel recordsCard = Theme.card(new BorderLayout(0, 14));
        JPanel recordsHeader = new JPanel(new BorderLayout(0, 12));recordsHeader.setOpaque(false);
        JPanel recordTitle = new JPanel(new BorderLayout());recordTitle.setOpaque(false);
        recordTitle.add(Theme.label("Appointments", 20, Theme.INK, true), BorderLayout.WEST);
        recordTitle.add(recordsCount, BorderLayout.EAST);
        recordsHeader.add(recordTitle, BorderLayout.NORTH);
        recordsHeader.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.SOUTH);
        recordsCard.add(recordsHeader, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.WHITE);
        table.setName("appointmentRecords");
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
        btnAdd.addActionListener(e -> save(false));
        btnUpdate.addActionListener(e -> save(true));
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clear());
        btnSearch.addActionListener(e -> loadTable());
        btnAll.addActionListener(e -> { txtSearch.setText(""); loadTable(); });
    }

    private void updateRecordsState() {
        int count = model.getRowCount();
        recordsCount.setText(count + (count == 1 ? " record" : " records"));
        boolean searching = !txtSearch.getText().trim().isEmpty();
        emptyTitle.setText(searching ? "No matching appointments" : "No appointments yet");
        emptyHint.setText(searching ? "Try a different search or reset the filter." : "Appointments will appear here once they are booked.");
        ((CardLayout) recordsBody.getLayout()).show(recordsBody, count == 0 ? "empty" : "table");
    }

    /** Reload selections and records when the page is opened. */
    @Override
    public void refresh() {
        try {
            GuiUtil.fillCombo(cmbPatient, patientDao.forCombo());
            GuiUtil.fillCombo(cmbDoctor, dao.doctorsForCombo());
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
        } catch (SQLException | SecurityException ex) {
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
        } catch (SQLException | SecurityException ex) {
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
        txtTime.setText((t.length() >= 5 ? t.substring(0, 5) : t).replace(':', '-'));
        cmbStatus.setSelectedItem(GuiUtil.str(model.getValueAt(r, 5)));
        txtReason.setText(GuiUtil.str(model.getValueAt(r, 6)));
    }
}
