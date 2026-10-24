package gui;

import security.*;
import service.AccountService;
import java.awt.*;
import javax.swing.*;

public class ProfilePanel extends JPanel implements GuiUtil.Refreshable {
    private final JLabel status=Theme.label("",12,Theme.MUTED,false);
    private final AccountService accounts=new AccountService();
    public ProfilePanel() {
        setLayout(new BorderLayout());setOpaque(false);User u=Session.current();
        JPanel card=Theme.card(new BorderLayout(0,24));
        JPanel details=Theme.column();details.add(Theme.label("Your account",24,Theme.INK,true));details.add(Box.createVerticalStrut(24));
        details.add(Theme.label(u.fullName,19,Theme.INK,true));details.add(Box.createVerticalStrut(10));
        details.add(Theme.label("@"+u.username+"  ·  "+u.role,14,Theme.TEAL,false));details.add(Box.createVerticalStrut(30));
        details.add(Theme.label("Password & account access",16,Theme.INK,true));details.add(Box.createVerticalStrut(10));
        details.add(Theme.label("<html>Password changes are handled by your clinic administrator.<br>Send a request below, then contact them to receive your new password.<br>Your current session will close after the administrator resets it.</html>",13,Theme.MUTED,false));
        details.add(Box.createVerticalStrut(20));details.add(status);details.add(Box.createVerticalStrut(20));
        JButton change=Theme.primary("Change password · Send request");change.setName("requestPasswordChange");
        change.addActionListener(e->{try{Session.verified();accounts.requestReset(u.username,u.role);refresh();GuiUtil.info(this,"Request sent to the Admin dashboard.\nYour administrator will set your new password.");}catch(Exception ex){GuiUtil.error(this,ex.getMessage());}});details.add(change);
        card.add(details,BorderLayout.NORTH);add(card,BorderLayout.CENTER);
    }
    @Override public void refresh(){try{status.setText(accounts.requestStatus());}catch(Exception e){status.setText(e.getMessage());}}
}
