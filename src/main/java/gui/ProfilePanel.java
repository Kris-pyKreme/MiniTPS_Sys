package gui;

import security.Session;
import security.User;
import service.AccountService;
import java.awt.*;
import javax.swing.*;

public class ProfilePanel extends JPanel implements GuiUtil.Refreshable {
    private final JLabel status=Theme.label("",13,Theme.MUTED,false);
    private final AccountService accounts=new AccountService();

    public ProfilePanel() {
        setLayout(new BorderLayout());setOpaque(false);
        User user=Session.current();
        JPanel content=new JPanel(new GridBagLayout());content.setOpaque(false);
        GridBagConstraints g=new GridBagConstraints();
        g.gridx=0;g.gridy=0;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.NORTH;

        JPanel account=Theme.card(new BorderLayout(0,24));
        account.add(Theme.label("Account details",20,Theme.INK,true),BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(1,3,28,0));fields.setOpaque(false);
        fields.add(detail("Full name",user.fullName));
        fields.add(detail("Username",user.username));
        fields.add(detail("Role",user.role.toString()));
        account.add(fields,BorderLayout.CENTER);content.add(account,g);

        JPanel password=Theme.card(new BorderLayout(0,20));
        JPanel heading=Theme.column();
        heading.add(Theme.label("Password",20,Theme.INK,true));heading.add(Box.createVerticalStrut(8));
        heading.add(Theme.label("Request a password change from your clinic administrator.",14,Theme.MUTED,false));
        password.add(heading,BorderLayout.NORTH);
        JPanel body=Theme.column();
        status.setName("passwordRequestStatus");body.add(status);body.add(Box.createVerticalStrut(10));
        body.add(Theme.label("You will sign in again after your administrator updates the password.",13,Theme.MUTED,false));
        password.add(body,BorderLayout.CENTER);
        JButton change=Theme.primary("Request password change");change.setName("requestPasswordChange");
        change.addActionListener(e->{
            try{
                Session.verified();accounts.requestReset(user.username,user.role);refresh();
                GuiUtil.info(this,"Request sent to the administrator.\nContact them to receive your new password.");
            }catch(Exception ex){GuiUtil.error(this,ex.getMessage());}
        });
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));actions.setOpaque(false);actions.add(change);
        password.add(actions,BorderLayout.SOUTH);
        g.gridy=1;g.insets=new Insets(18,0,0,0);content.add(password,g);
        g.gridy=2;g.weighty=1;g.fill=GridBagConstraints.BOTH;content.add(Box.createGlue(),g);

        JScrollPane scroll=new JScrollPane(content);scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll,BorderLayout.CENTER);
    }
    private JPanel detail(String title,String value){
        JPanel field=Theme.column();field.add(Theme.label(title,13,Theme.MUTED,false));
        field.add(Box.createVerticalStrut(8));field.add(Theme.label(value,17,Theme.INK,false));return field;
    }
    @Override public void refresh(){
        try{
            String text=accounts.requestStatus();status.setText(text);
            status.setForeground(text.startsWith("Pending")?Theme.TEAL:Theme.MUTED);
        }catch(Exception ex){status.setForeground(new Color(0xB34747));status.setText(ex.getMessage());}
    }
}
