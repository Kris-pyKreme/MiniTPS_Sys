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
    private final JPanel recordsBody = new JPanel(new CardLayout());
    private final JLabel recordsCount = Theme.label("0 records", 13, Theme.MUTED, false);
    private final JLabel emptyTitle = Theme.label("No consultations yet", 18, Theme.INK, true);
    private final JLabel emptyHint = Theme.label("Completed consultation records will appear here.", 13, Theme.MUTED, false);

    public ConsultationPanel() {
        setLayout(new BorderLayout(0, 18));
        setOpaque(false);
        txtId.setEditable(false);
        txtId.putClientProperty("JTextField.placeholderText", "Assigned automatically");
        txtFee.putClientProperty("JTextField.placeholderText", "0.00");
        for (JTextArea a : new JTextArea[]{txtDiagnosis, txtNotes, txtPrescription}) {
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GuiUtil.addRow(form, 0, 0, "Consultation ID", txtId);
        GuiUtil.addRow(form, 0, 1, "Pending appointment", cmbAppointment);
        GuiUtil.addRow(form, 1, 0, "Consultation fee (PHP)", txtFee);
        GuiUtil.addRow(form, 2, 0, "Diagnosis", new JScrollPane(txtDiagnosis));
        GuiUtil.addRow(form, 2, 1, "Treatment notes", new JScrollPane(txtNotes));
        GuiUtil.addRow(form, 1, 1, "Prescription", new JScrollPane(txtPrescription));

        JButton btnAdd = Theme.primary("Save consultation"), btnUpdate = Theme.quiet("Save changes"),
                btnDelete = Theme.quiet("Delete"), btnClear = Theme.quiet("Clear"),
                btnSearch = Theme.quiet("Search"), btnAll = Theme.quiet("Reset");
        btnDelete.setForeground(new Color(0xAD3D3D));

        boolean editable = Session.current().role.consults();
        btnAdd.setVisible(editable); btnUpdate.setVisible(editable); btnClear.setVisible(editable);
        btnDelete.setVisible(Session.current().role == Role.ADMIN);
        JPanel formCard = Theme.card(new BorderLayout(0, 12));
        formCard.add(Theme.label("Consultation details", 18, Theme.INK, true), BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);
        formCard.add(GuiUtil.buttonBar(btnAdd, btnUpdate, btnClear, btnDelete), BorderLayout.SOUTH);
        formCard.setVisible(editable);

        JPanel recordsCard = Theme.card(new BorderLayout(0, 14));
        JPanel recordsHeader = new JPanel(new BorderLayout(0, 12));recordsHeader.setOpaque(false);
        JPanel recordTitle = new JPanel(new BorderLayout());recordTitle.setOpaque(false);
        recordTitle.add(Theme.label("Consultation records", 20, Theme.INK, true), BorderLayout.WEST);
        recordTitle.add(recordsCount, BorderLayout.EAST);
        recordsHeader.add(recordTitle, BorderLayout.NORTH);
        recordsHeader.add(GuiUtil.searchBar(txtSearch, btnSearch, btnAll), BorderLayout.SOUTH);
        recordsCard.add(recordsHeader, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.WHITE);
        table.setName("consultationRecords");
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
        btnSearch.addActionListener(e -> loadTable());
        btnAll.addActionListener(e -> { txtSearch.setText(""); loadTable(); });
    }

    private void updateRecordsState() {
        int count = model.getRowCount();
        recordsCount.setText(count + (count == 1 ? " record" : " records"));
        boolean searching = !txtSearch.getText().trim().isEmpty();
        emptyTitle.setText(searching ? "No matching consultations" : "No consultations yet");
        emptyHint.setText(searching ? "Try a different search or reset the filter." : "Completed consultation records will appear here.");
        ((CardLayout) recordsBody.getLayout()).show(recordsBody, count == 0 ? "empty" : "table");
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
