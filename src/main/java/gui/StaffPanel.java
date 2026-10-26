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
    private final JLabel error=Theme.label(" ",13,new Color(0xB34747),false);
    private final JLabel staffCount=Theme.label("",13,Theme.MUTED,false);
    private final JLabel requestCount=Theme.label("",13,Theme.MUTED,false);
    private final JPanel requestBody=new JPanel(new CardLayout());
    private final JButton resolve=Theme.primary("Set password");
    private final JButton editAccess=Theme.quiet("Edit access"),resetPassword=Theme.quiet("Reset password");

    public StaffPanel() {
        if(Session.current().role!=Role.ADMIN)throw new SecurityException("Administrator access required.");
        setLayout(new BorderLayout(0,18));setOpaque(false);
        JPanel requestCard=Theme.card(new BorderLayout(0,16));
        JPanel requestHeading=new JPanel(new BorderLayout(16,0));requestHeading.setOpaque(false);
        JPanel title=Theme.column();title.add(Theme.label("Password requests",20,Theme.INK,true));
        title.add(Box.createVerticalStrut(5));title.add(requestCount);requestHeading.add(title,BorderLayout.CENTER);
        resolve.setName("resolvePasswordRequest");resolve.setEnabled(false);
        resolve.addActionListener(e->reset(requestTable,1));requestHeading.add(resolve,BorderLayout.EAST);
        requestCard.add(requestHeading,BorderLayout.NORTH);
        requestTable.setName("passwordRequests");
        JScrollPane requestScroll=tableScroll(requestTable);requestScroll.setPreferredSize(new Dimension(650,145));
        requestBody.setOpaque(false);requestBody.add(requestScroll,"table");
        JPanel empty=new JPanel(new GridBagLayout());empty.setOpaque(false);
        empty.add(Theme.label("No pending password requests",14,Theme.MUTED,false));requestBody.add(empty,"empty");
        requestBody.setPreferredSize(new Dimension(650,145));requestCard.add(requestBody,BorderLayout.CENTER);
        add(requestCard,BorderLayout.NORTH);

        JPanel directory=Theme.card(new BorderLayout(0,16));
        JPanel directoryTop=new JPanel(new BorderLayout(0,16));directoryTop.setOpaque(false);
        JPanel heading=new JPanel(new BorderLayout(16,0));heading.setOpaque(false);
        heading.add(Theme.label("Staff directory",20,Theme.INK,true),BorderLayout.WEST);
        JButton create=Theme.primary("Add staff");create.setName("addStaff");create.addActionListener(e->create());
        heading.add(create,BorderLayout.EAST);directoryTop.add(heading,BorderLayout.NORTH);
        JPanel toolbar=new JPanel(new BorderLayout(12,0));toolbar.setOpaque(false);toolbar.add(staffCount,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);
        JButton reload=Theme.quiet("Refresh");reload.addActionListener(e->refresh());
        editAccess.setEnabled(false);editAccess.addActionListener(e->edit());
        resetPassword.setEnabled(false);resetPassword.addActionListener(e->reset(staffTable,0));
        actions.add(reload);actions.add(editAccess);actions.add(resetPassword);toolbar.add(actions,BorderLayout.EAST);
        directoryTop.add(toolbar,BorderLayout.SOUTH);directory.add(directoryTop,BorderLayout.NORTH);
        staffTable.setName("staffDirectory");directory.add(tableScroll(staffTable),BorderLayout.CENTER);
        add(directory,BorderLayout.CENTER);error.setVisible(false);add(error,BorderLayout.SOUTH);

        Theme.polish(this);
        // Keep internal IDs in the models for account operations, but out of the visible directory.
        staffTable.removeColumn(staffTable.getColumnModel().getColumn(0));
        requestTable.removeColumn(requestTable.getColumnModel().getColumn(1));
        requestTable.removeColumn(requestTable.getColumnModel().getColumn(0));
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staffTable.getSelectionModel().addListSelectionListener(e->{boolean selected=staffTable.getSelectedRow()>=0;editAccess.setEnabled(selected);resetPassword.setEnabled(selected);});
        requestTable.getSelectionModel().addListSelectionListener(e->resolve.setEnabled(requestTable.getSelectedRow()>=0));
    }
    private JScrollPane tableScroll(JTable table){
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Theme.WHITE);return scroll;
    }
    @Override public void refresh(){
        Integer staffId=selectedId(staffTable,0),requestUserId=selectedId(requestTable,1);
        try{
            GuiUtil.fill(staff,accounts.staff());GuiUtil.fill(requests,accounts.requests());
            selectId(staffTable,0,staffId);selectId(requestTable,1,requestUserId);
            staffCount.setText(staff.getRowCount()+" staff "+(staff.getRowCount()==1?"member":"members"));
            int pending=requests.getRowCount();requestCount.setText(pending+" pending "+(pending==1?"request":"requests"));
            requestBody.setPreferredSize(new Dimension(650,pending==0?76:145));revalidate();
            ((CardLayout)requestBody.getLayout()).show(requestBody,pending==0?"empty":"table");error.setVisible(false);
        }catch(Exception ex){error.setText(ex.getMessage());error.setVisible(true);}
    }
    private Integer selectedId(JTable table,int column){
        int row=table.getSelectedRow();return row<0?null:((Number)table.getModel().getValueAt(table.convertRowIndexToModel(row),column)).intValue();
    }
    private void selectId(JTable table,int column,Integer id){
        if(id==null)return;
        for(int row=0;row<table.getModel().getRowCount();row++){
            if(((Number)table.getModel().getValueAt(row,column)).intValue()==id){
                int view=table.convertRowIndexToView(row);if(view>=0)table.setRowSelectionInterval(view,view);return;
            }
        }
    }
    private void reset(JTable table,int idColumn) {
        int selected=table.getSelectedRow();if(selected<0){GuiUtil.error(this,"Select a staff account or request first.");return;}
        int row=table.convertRowIndexToModel(selected);int id=((Number)table.getModel().getValueAt(row,idColumn)).intValue();
        String name=String.valueOf(table.getModel().getValueAt(row,2));
        JPasswordField pass=new JPasswordField(22),confirm=new JPasswordField(22);
        pass.putClientProperty("JTextField.placeholderText","8–128 characters");confirm.putClientProperty("JTextField.placeholderText","Enter the same password again");
        JPanel form=dialogForm();
        addField(form,0,"New password",pass);addField(form,1,"Confirm password",confirm);
        if(JOptionPane.showConfirmDialog(this,form,"Set password · "+name,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        char[] a=pass.getPassword(),b=confirm.getPassword();
        try{
            if(!Arrays.equals(a,b))throw new IllegalArgumentException("Passwords do not match.");
            accounts.resetPassword(id,a);refresh();GuiUtil.info(this,"Password updated. The request is resolved.\nThe staff member must sign in again with their new password.");
        }catch(Exception ex){GuiUtil.error(this,ex.getMessage());}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');pass.setText("");confirm.setText("");}
    }
    private void edit() {
        int selected=staffTable.getSelectedRow();if(selected<0){GuiUtil.error(this,"Select a staff account first.");return;}
        int row=staffTable.convertRowIndexToModel(selected);int id=((Number)staff.getValueAt(row,0)).intValue();
        JTextField name=new JTextField(String.valueOf(staff.getValueAt(row,2)),22);
        JComboBox<Role> role=new JComboBox<>(Role.values());
        try{role.setSelectedItem(Role.from(String.valueOf(staff.getValueAt(row,3))));}catch(IllegalArgumentException legacy){role.setSelectedItem(Role.RECEPTIONIST);}
        JComboBox<GuiUtil.Item> doctor=new JComboBox<>();
        try{GuiUtil.fillCombo(doctor,new AppointmentDAO().doctorsForCombo());}catch(Exception ex){GuiUtil.error(this,ex.getMessage());return;}
        String linked=String.valueOf(staff.getValueAt(row,4));
        for(int i=0;i<doctor.getItemCount();i++)if(doctor.getItemAt(i).label.endsWith(" - "+linked))doctor.setSelectedIndex(i);
        doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR);role.addActionListener(e->doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR));
        JPanel form=dialogForm();addField(form,0,"Full name",name);addField(form,1,"Role",role);addField(form,2,"Linked doctor",doctor);
        if(JOptionPane.showConfirmDialog(this,form,"Edit staff access",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{GuiUtil.Item d=(GuiUtil.Item)doctor.getSelectedItem();accounts.updateStaff(id,name.getText(),(Role)role.getSelectedItem(),d==null?null:d.id);refresh();GuiUtil.info(this,"Staff access updated. The account must sign in again.");}catch(Exception ex){GuiUtil.error(this,ex.getMessage());}
    }
    private void create() {
        JTextField username=new JTextField(22),name=new JTextField(22);JPasswordField password=new JPasswordField(22);
        username.putClientProperty("JTextField.placeholderText","Username");password.putClientProperty("JTextField.placeholderText","8–128 characters");
        JComboBox<Role> role=new JComboBox<>(Role.values());JComboBox<GuiUtil.Item> doctor=new JComboBox<>();
        try{GuiUtil.fillCombo(doctor,new AppointmentDAO().doctorsForCombo());}catch(Exception ex){GuiUtil.error(this,ex.getMessage());return;}
        doctor.setEnabled(false);role.addActionListener(e->doctor.setEnabled(role.getSelectedItem()==Role.DOCTOR));
        JPanel form=dialogForm();addField(form,0,"Full name",name);addField(form,1,"Username",username);addField(form,2,"Role",role);addField(form,3,"Linked doctor",doctor);addField(form,4,"Password",password);
        if(JOptionPane.showConfirmDialog(this,form,"Add staff account",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        char[] p=password.getPassword();
        try{GuiUtil.Item d=(GuiUtil.Item)doctor.getSelectedItem();accounts.create(username.getText().trim(),name.getText().trim(),(Role)role.getSelectedItem(),d==null?null:d.id,p);refresh();GuiUtil.info(this,"Staff account created.");}
        catch(Exception ex){GuiUtil.error(this,ex.getMessage());}finally{Arrays.fill(p,'\0');password.setText("");}
    }
    private JPanel dialogForm(){
        JPanel panel=new JPanel(new GridBagLayout());panel.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));return panel;
    }
    private void addField(JPanel form,int row,String title,JComponent field){
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=row*2;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(row==0?0:14,0,7,0);
        form.add(Theme.label(title,13,Theme.INK,false),g);g.gridy++;g.insets=new Insets(0,0,0,0);form.add(field,g);
    }
}
