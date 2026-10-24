package gui;

import security.*;
import service.AccountService;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import javax.swing.*;

public class MainFrame extends JFrame {
    private final User user=Session.current();
    private final JPanel pages=new JPanel(new CardLayout());
    private final Map<String,JComponent> screens=new LinkedHashMap<>();
    private final Map<String,JButton> navigation=new LinkedHashMap<>();
    private final Map<String,Supplier<JComponent>> factories=new LinkedHashMap<>();
    private final JLabel heading=Theme.label("",27,Theme.INK,true),subheading=Theme.label("",12,Theme.MUTED,false);
    private final JButton alerts=Theme.quiet("Requests  ·  0");
    private final javax.swing.Timer updates;
    private String active="Overview";

    public MainFrame() {
        super("ClinicFlow · "+Session.current().role+" workspace");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1320,860);setMinimumSize(new Dimension(1180,760));setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(Theme.BG);
        JPanel sidebar=new JPanel(new BorderLayout());sidebar.setBackground(Color.WHITE);sidebar.setPreferredSize(new Dimension(210,0));sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,Theme.LINE),BorderFactory.createEmptyBorder(30,16,22,16)));
        JPanel menu=Theme.column();
        menu.add(Theme.label("✚  ClinicFlow",22,Theme.TEAL,true));menu.add(Box.createVerticalStrut(7));menu.add(Theme.label("   YOUR CONNECTED CLINIC",9,Theme.MUTED,true));menu.add(Box.createVerticalStrut(38));menu.add(Theme.label("WORKSPACE",10,Theme.MUTED,true));menu.add(Box.createVerticalStrut(14));
        factories.put("Overview",()->new DashboardPanel(()->navigate(user.role==Role.ADMIN?"Staff & requests":"My profile")));
        factories.put("Patients",PatientPanel::new);factories.put("Appointments",AppointmentPanel::new);
        if(user.role.consults())factories.put("Consultations",ConsultationPanel::new);
        if(user.role.bills())factories.put("Billing",BillingPanel::new);
        if(user.role==Role.ADMIN)factories.put("Staff & requests",StaffPanel::new);
        factories.put("My profile",ProfilePanel::new);
        String[] symbols={"▦","♙","▤","✚","▣","♧","○"};int i=0;
        for(String name:factories.keySet()){
            JButton b=new JButton(symbols[i++%symbols.length]+"    "+name);b.setName("nav-"+name);b.setHorizontalAlignment(SwingConstants.LEFT);b.setFocusPainted(false);b.setBorderPainted(false);b.setBackground(Color.WHITE);b.setForeground(Theme.MUTED);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.setPreferredSize(new Dimension(175,44));b.putClientProperty("JButton.buttonType","roundRect");b.addActionListener(e->navigate(name));navigation.put(name,b);menu.add(b);menu.add(Box.createVerticalStrut(6));
        }
        menu.add(Box.createVerticalGlue());sidebar.add(menu,BorderLayout.CENTER);
        JPanel bottom=Theme.column();JSeparator sep=new JSeparator();sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));bottom.add(sep);bottom.add(Box.createVerticalStrut(18));
        bottom.add(Theme.label(user.fullName,13,Theme.INK,true));bottom.add(Box.createVerticalStrut(4));bottom.add(Theme.label(user.role+" workspace",11,Theme.MUTED,false));bottom.add(Box.createVerticalStrut(18));
        JButton logout=Theme.quiet("↪   Sign out");logout.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));logout.addActionListener(e->{if(GuiUtil.confirm(this,"Sign out of your workspace?"))signOut();});bottom.add(logout);bottom.add(Box.createVerticalStrut(14));bottom.add(Theme.label("●  Connected to your clinic",10,Theme.TEAL,false));sidebar.add(bottom,BorderLayout.SOUTH);root.add(sidebar,BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout(0,22));main.setOpaque(false);main.setBorder(BorderFactory.createEmptyBorder(28,28,18,28));
        JPanel header=new JPanel(new BorderLayout(12,18));header.setOpaque(false);JPanel titles=Theme.column();titles.add(Theme.label("WORKSPACE  /  "+user.role.toString().toUpperCase(),10,Theme.MUTED,true));titles.add(Box.createVerticalStrut(10));titles.add(heading);titles.add(Box.createVerticalStrut(8));titles.add(subheading);header.add(titles,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);
        actions.add(Theme.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),11,Theme.MUTED,false));
        if(user.role==Role.ADMIN){alerts.addActionListener(e->navigate("Staff & requests"));actions.add(alerts);}
        JButton refresh=Theme.quiet("↻  Refresh");refresh.addActionListener(e->refreshActive());actions.add(refresh);header.add(actions,BorderLayout.EAST);main.add(header,BorderLayout.NORTH);
        pages.setOpaque(false);main.add(pages,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setOpaque(false);footer.add(Theme.label("CLINICFLOW  /  Patient care, thoughtfully connected.",9,Theme.MUTED,false),BorderLayout.WEST);footer.add(Theme.label(""+LocalDate.now().getYear()+"  ·  Clinic workspace",9,Theme.MUTED,false),BorderLayout.EAST);main.add(footer,BorderLayout.SOUTH);root.add(main,BorderLayout.CENTER);setContentPane(root);
        updates=new javax.swing.Timer(15000,e->poll());updates.start();addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){updates.stop();}});
        navigate("Overview");
    }
    public void navigate(String name) {
        if(!factories.containsKey(name))throw new SecurityException("This page is not available for your role.");
        try{Session.verified();}catch(SecurityException e){expired(e.getMessage());return;}catch(Exception e){GuiUtil.error(this,e.getMessage());return;}
        if(!screens.containsKey(name)){JComponent screen=factories.get(name).get();screens.put(name,screen);pages.add(screen,name);}
        active=name;((CardLayout)pages.getLayout()).show(pages,name);
        for(Map.Entry<String,JButton> entry:navigation.entrySet()){
            boolean selected=entry.getKey().equals(name);entry.getValue().setBackground(selected?Theme.PALE:Color.WHITE);entry.getValue().setForeground(selected?Theme.TEAL:Theme.MUTED);entry.getValue().setFont(entry.getValue().getFont().deriveFont(selected?Font.BOLD:Font.PLAIN));
        }
        heading.setText(name.equals("Overview")?user.role+" dashboard":name);
        subheading.setText(description(name));refreshActive();
    }
    private String description(String name) {
        switch(name){
            case "Overview":return "Welcome back, "+user.fullName+". Here’s your clinic at a glance.";
            case "Patients":return user.role==Role.DOCTOR?"Patient records for your assigned appointments.":"A clear picture of every patient, from their very first visit.";
            case "Appointments":return user.role==Role.DOCTOR?"Your assigned appointments and upcoming visits.":user.role==Role.NURSE?"View the clinic schedule and prepare for arriving patients.":"Plan visits, coordinate your care team, and keep the day moving.";
            case "Consultations":return "Record diagnoses, treatment notes, and prescriptions for each visit.";
            case "Billing":return "Turn completed consultations into clear, accurate patient bills.";
            case "Staff & requests":return "Manage staff access and resolve password requests in one place.";
            default:return "Your staff identity and password change requests.";
        }
    }
    private void refreshActive(){JComponent screen=screens.get(active);if(screen instanceof GuiUtil.Refreshable)((GuiUtil.Refreshable)screen).refresh();}
    private void poll(){
        try{
            Session.verified();
            if(user.role==Role.ADMIN)alerts.setText("Requests  ·  "+new AccountService().requests().size());
            if(active.equals("Overview")||active.equals("Staff & requests")||active.equals("My profile"))refreshActive();
        }catch(SecurityException ex){expired(ex.getMessage());}catch(Exception ex){subheading.setText("Connection interrupted. Use Refresh to try again.");}
    }
    private void expired(String message){updates.stop();GuiUtil.info(this,message);signOut();}
    private void signOut(){updates.stop();Session.logout();dispose();new Login().setVisible(true);}
}
