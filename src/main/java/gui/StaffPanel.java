package gui;

import dao.AppointmentDAO;
import security.*;
import service.AccountService;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StaffPanel extends JPanel implements GuiUtil.Refreshable {
    private final AccountService accounts=new AccountService();
    private final DefaultTableModel staff=GuiUtil.tableModel("ID","Username","Full name","Role","Linked doctor");
    private final DefaultTableModel requests=GuiUtil.tableModel("Request","User ID","Staff member","Username","Role","Requested on");
    private final JTable staffTable=new JTable(staff),requestTable=new JTable(requests);
    private final JLabel error=Theme.label(" ",12,new Color(0xB34747),false);
    public StaffPanel() {
        if(Session.current().role!=Role.ADMIN)throw new SecurityException("Administrator access required.");
        setLayout(new BorderLayout(0,16));setOpaque(false);
        JPanel top=Theme.card(new BorderLayout(0,10));
        JPanel title=new JPanel(new BorderLayout());title.setOpaque(false);title.add(Theme.label("Password requests",18,Theme.INK,true),BorderLayout.WEST);
        JButton resolve=Theme.primary("Set password for selected request");resolve.setName("resolvePasswordRequest");resolve.addActionListener(e->reset(requestTable,1));title.add(resolve,BorderLayout.EAST);top.add(title,BorderLayout.NORTH);
        JScrollPane reqScroll=new JScrollPane(requestTable);reqScroll.setPreferredSize(new Dimension(700,155));top.add(reqScroll,BorderLayout.CENTER);
        top.add(Theme.label("Requests refresh automatically. Set a new password and share it directly with the staff member.",11,Theme.MUTED,false),BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        JPanel bottom=Theme.card(new BorderLayout(0,12));JPanel toolbar=new JPanel(new BorderLayout());toolbar.setOpaque(false);toolbar.add(Theme.label("Staff directory",18,Theme.INK,true),BorderLayout.WEST);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.setOpaque(false);
        JButton create=Theme.primary("+  Add staff"),reset=Theme.quiet("Reset selected password"),reload=Theme.quiet("Refresh");
        create.setName("addStaff");create.addActionListener(e->create());reset.addActionListener(e->reset(staffTable,0));reload.addActionListener(e->refresh());JButton edit=Theme.quiet("Edit access");edit.addActionListener(e->edit());buttons.add(reload);buttons.add(edit);buttons.add(reset);buttons.add(create);toolbar.add(buttons,BorderLayout.EAST);
        bottom.add(toolbar,BorderLayout.NORTH);bottom.add(new JScrollPane(staffTable),BorderLayout.CENTER);add(bottom,BorderLayout.CENTER);add(error,BorderLayout.SOUTH);Theme.polish(this);
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    @Override public void refresh(){try{GuiUtil.fill(staff,accounts.staff());GuiUtil.fill(requests,accounts.requests());error.setForeground(Theme.MUTED);error.setText(requests.getRowCount()+" pending password requests");}catch(Exception ex){error.setForeground(new Color(0xB34747));error.setText(ex.getMessage());}}
    private void reset(JTable table,int idColumn) {
        int selected=table.getSelectedRow();if(selected<0){GuiUtil.error(this,"Select a staff account or request first.");return;}
        int row=table.convertRowIndexToModel(selected);int id=((Number)table.getModel().getValueAt(row,idColumn)).intValue();
        String name=String.valueOf(table.getModel().getValueAt(row,2));
        JPasswordField pass=new JPasswordField(22),confirm=new JPasswordField(22);
        JPanel form=new JPanel(new GridLayout(0,1,0,8));form.add(new JLabel("New password for "+name+" (8–128 characters)"));form.add(pass);form.add(new JLabel("Confirm password"));form.add(confirm);
        if(JOptionPane.showConfirmDialog(this,form,"Reset staff password",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        char[] a=pass.getPassword(),b=confirm.getPassword();
        try{
            if(!Arrays.equals(a,b))throw new IllegalArgumentException("Passwords do not match.");
            accounts.resetPassword(id,a);refresh();GuiUtil.info(this,"Password updated. The request is resolved.\nThe staff member must sign in again with their new password.");
        }catch(Exception ex){GuiUtil.error(this,ex.getMessage());}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');pass.setText("");confirm.setText("");}
    }
    private void edit() {
        int row=staffTable.getSelectedRow();if(row<0){GuiUtil.error(this,"Select a staff account first.");return;}
        int id=((Number)staff.getValueAt(row,0)).intValue();
        JTextField name=new JTextField(String.valueOf(staff.getValueAt(row,2)));
        JComboBox<Role> role=new JComboBox<>(Role.values());
        try{role.setSelectedItem(Role.from(String.valueOf(staff.getValueAt(row,3))));}catch(IllegalArgumentException legacy){role.setSelectedItem(Role.RECEPTIONIST);}
        JComboBox<GuiUtil.Item> doctor=new JComboBox<>();
        try{GuiUtil.fillCombo(doctor,new AppointmentDAO().doctorsForCombo());}catch(Exception ex){GuiUtil.error(this,ex.getMessage());return;}
        String linked=String.valueOf(staff.getValueAt(row,4));
        for(int i=0;i<doctor.getItemCount();i++)if(doctor.getItemAt(i).label.endsWith(" - "+linked))doctor.setSelectedIndex(i);
        doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR);role.addActionListener(e->doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR));
        JPanel form=new JPanel(new GridLayout(0,2,12,12));form.add(new JLabel("Full name"));form.add(name);form.add(new JLabel("Role"));form.add(role);form.add(new JLabel("Linked doctor"));form.add(doctor);
        if(JOptionPane.showConfirmDialog(this,form,"Edit staff access",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{GuiUtil.Item d=(GuiUtil.Item)doctor.getSelectedItem();accounts.updateStaff(id,name.getText(),(Role)role.getSelectedItem(),d==null?null:d.id);refresh();GuiUtil.info(this,"Staff access updated. The account must sign in again.");}catch(Exception ex){GuiUtil.error(this,ex.getMessage());}
    }
    private void create() {
        JTextField username=new JTextField(),name=new JTextField();JPasswordField password=new JPasswordField();
        JComboBox<Role> role=new JComboBox<>(Role.values());JComboBox<GuiUtil.Item> doctor=new JComboBox<>();
        try{GuiUtil.fillCombo(doctor,new AppointmentDAO().doctorsForCombo());}catch(Exception ex){GuiUtil.error(this,ex.getMessage());return;}
        doctor.setEnabled(false);role.addActionListener(e->doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR));
        JPanel form=new JPanel(new GridLayout(0,2,12,12));
        form.add(new JLabel("Full name"));form.add(name);form.add(new JLabel("Username"));form.add(username);form.add(new JLabel("Role"));form.add(role);form.add(new JLabel("Doctor (for Doctor role)"));form.add(doctor);form.add(new JLabel("Password (8–128 characters)"));form.add(password);
        if(JOptionPane.showConfirmDialog(this,form,"Add staff account",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        char[] p=password.getPassword();
        try{GuiUtil.Item d=(GuiUtil.Item)doctor.getSelectedItem();accounts.create(username.getText().trim(),name.getText().trim(),(Role)role.getSelectedItem(),d==null?null:d.id,p);refresh();GuiUtil.info(this,"Staff account created. They can sign in with the selected role.");}
        catch(Exception ex){GuiUtil.error(this,ex.getMessage());}finally{Arrays.fill(p,'\0');password.setText("");}
    }
}
