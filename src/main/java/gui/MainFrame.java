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
    private final JLabel heading=Theme.label("Overview",30,Theme.INK,true),subheading=Theme.label("",16,Theme.MUTED,false);
    private final JButton alerts=Theme.quiet("Requests");
    private final javax.swing.Timer updates;
    private String active="Overview";

    public MainFrame(){
        super("KlinikAccess · "+Session.current().role);
        Rectangle area=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setDefaultCloseOperation(EXIT_ON_CLOSE);setMinimumSize(new Dimension(Math.min(1150,area.width),Math.min(720,area.height)));
        setSize(Math.min(1320,area.width),Math.min(860,area.height));setLocationRelativeTo(null);
        JPanel outer=new JPanel(new BorderLayout());outer.setBackground(Theme.BG);outer.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel shell=Theme.surface(new BorderLayout(),Theme.BG,22,0);
        JPanel sidebar=new JPanel(new BorderLayout());sidebar.setBackground(Theme.WHITE);sidebar.setPreferredSize(new Dimension(254,0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,Theme.LINE),BorderFactory.createEmptyBorder(26,16,22,16)));
        JPanel menu=Theme.column();JPanel brand=Theme.brand(40,23);brand.setAlignmentX(0);brand.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));menu.add(brand);menu.add(Box.createVerticalStrut(30));
        factories.put("Overview",()->new DashboardPanel(()->navigate(user.role==Role.ADMIN?"Staff & requests":"My profile"),()->navigate("Appointments")));
        factories.put("Patients",PatientPanel::new);factories.put("Appointments",AppointmentPanel::new);
        if(user.role.consults())factories.put("Consultations",ConsultationPanel::new);
        if(user.role.bills())factories.put("Billing",BillingPanel::new);
        if(user.role==Role.ADMIN)factories.put("Staff & requests",StaffPanel::new);
        factories.put("My profile",ProfilePanel::new);
        for(String name:factories.keySet()){
            if(name.equals("My profile"))continue;
            String icon=name.equals("Overview")?"overview":name.equals("Staff & requests")?"staff":name.toLowerCase(Locale.ROOT);
            JButton b=new JButton(name.equals("Staff & requests")?"Staff":name,new LineIcon(icon,20,Theme.MUTED));
            b.setName("nav-"+name);b.setFont(Theme.font(17,false));b.setIconTextGap(16);b.setHorizontalAlignment(SwingConstants.LEFT);b.setFocusPainted(true);b.setBorderPainted(false);
            b.setMargin(new Insets(12,16,12,12));b.setBackground(Theme.WHITE);b.setForeground(Theme.MUTED);int height=Theme.controlHeight(b,52);b.setMinimumSize(new Dimension(0,height));b.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));b.setPreferredSize(new Dimension(Math.max(220,b.getUI().getPreferredSize(b).width),height));b.setAlignmentX(0);
            b.addActionListener(e->navigate(name));navigation.put(name,b);menu.add(b);menu.add(Box.createVerticalStrut(7));
        }
        menu.add(Box.createVerticalGlue());
        sidebar.setPreferredSize(new Dimension(Math.max(254,menu.getPreferredSize().width+32),0));
        JScrollPane menuScroll=new JScrollPane(menu);menuScroll.setBorder(BorderFactory.createEmptyBorder());menuScroll.setOpaque(false);menuScroll.getViewport().setOpaque(false);menuScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);menuScroll.getVerticalScrollBar().setUnitIncrement(18);sidebar.add(menuScroll,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout(0,18));bottom.setOpaque(false);JSeparator line=new JSeparator();line.setForeground(Theme.LINE);bottom.add(line,BorderLayout.NORTH);
        JPanel identity=new JPanel(new BorderLayout(12,0));identity.setOpaque(false);identity.setMaximumSize(new Dimension(Integer.MAX_VALUE,54));
        JLabel avatar=new JLabel(initials(user.fullName),SwingConstants.CENTER);avatar.setFont(Theme.font(15,true));avatar.setForeground(Theme.TEAL);
        JPanel circle=Theme.surface(new BorderLayout(),Theme.PALE,48,0);circle.setBorder(BorderFactory.createEmptyBorder());circle.setPreferredSize(new Dimension(44,44));circle.add(avatar);identity.add(circle,BorderLayout.WEST);
        JButton profile=new JButton("<html><b>"+html(user.fullName)+"</b><br><font color='#60625F'>"+user.role+"</font></html>");
        profile.setName("nav-My profile");profile.setFont(Theme.font(15,false));profile.setHorizontalAlignment(SwingConstants.LEFT);profile.setBorderPainted(false);profile.setContentAreaFilled(false);profile.setMargin(new Insets(0,0,0,0));profile.setToolTipText("My profile");profile.addActionListener(e->navigate("My profile"));identity.add(profile);bottom.add(identity,BorderLayout.CENTER);
        JButton logout=Theme.quiet("Sign out");logout.setName("signOut");logout.setIcon(new LineIcon("logout",20,Theme.MUTED));logout.setIconTextGap(16);logout.setFont(Theme.font(17,false));logout.setForeground(Theme.MUTED);logout.setHorizontalAlignment(SwingConstants.LEFT);logout.setBorderPainted(false);int logoutHeight=Theme.controlHeight(logout,44);logout.setPreferredSize(new Dimension(Math.max(220,logout.getUI().getPreferredSize(logout).width),logoutHeight));logout.addActionListener(e->{if(GuiUtil.confirm(this,"Sign out of KlinikAccess?"))signOut();});bottom.add(logout,BorderLayout.SOUTH);sidebar.add(bottom,BorderLayout.SOUTH);shell.add(sidebar,BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout(0,22));main.setOpaque(false);main.setBorder(BorderFactory.createEmptyBorder(28,28,26,28));
        JPanel header=new JPanel(new BorderLayout(12,0));header.setOpaque(false);JPanel titles=Theme.column();titles.add(heading);titles.add(Box.createVerticalStrut(7));titles.add(subheading);header.add(titles,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,4));actions.setOpaque(false);
        if(user.role==Role.ADMIN){alerts.setFont(Theme.font(16,false));alerts.setIcon(new LineIcon("bell",18,Theme.INK));alerts.setIconTextGap(9);alerts.addActionListener(e->navigate("Staff & requests"));actions.add(alerts);}
        JButton refresh=Theme.iconButton("refresh","Refresh current page");refresh.setName("refreshPage");refresh.addActionListener(e->refreshActive());actions.add(refresh);header.add(actions,BorderLayout.EAST);main.add(header,BorderLayout.NORTH);
        pages.setOpaque(false);main.add(pages,BorderLayout.CENTER);shell.add(main);outer.add(shell);setContentPane(outer);
        updates=new javax.swing.Timer(15000,e->poll());updates.start();addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){updates.stop();}});navigate("Overview");
    }
    private static String initials(String name){String[] words=name.trim().split("\\s+");return (words[0].substring(0,1)+(words.length>1?words[words.length-1].substring(0,1):"")).toUpperCase(Locale.ROOT);}
    private static String html(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    public void navigate(String name){
        if(!factories.containsKey(name))throw new SecurityException("This page is not available for your role.");
        try{Session.verified();}catch(SecurityException ex){expired(ex.getMessage());return;}catch(Exception ex){GuiUtil.error(this,ex.getMessage());return;}
        if(!screens.containsKey(name)){
            JComponent screen=factories.get(name).get();screens.put(name,screen);
            if(!name.equals("Overview")&&!name.equals("My profile")){
                JScrollPane scroll=new JScrollPane(new PageViewport(screen));scroll.setBorder(BorderFactory.createEmptyBorder());
                scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(18);pages.add(scroll,name);
            }else pages.add(screen,name);
        }
        active=name;((CardLayout)pages.getLayout()).show(pages,name);
        for(Map.Entry<String,JButton> entry:navigation.entrySet()){
            boolean selected=entry.getKey().equals(name);JButton button=entry.getValue();Color color=selected?Theme.TEAL:Theme.MUTED;
            button.setBackground(selected?Theme.PALE:Theme.WHITE);button.setForeground(color);button.setFont(Theme.font(17,selected));
            button.setIcon(new LineIcon(entry.getKey().equals("Overview")?"overview":entry.getKey().equals("Staff & requests")?"staff":entry.getKey().toLowerCase(Locale.ROOT),20,color));
        }
        heading.setText(name.equals("Staff & requests")?"Staff":name.equals("My profile")?"My profile":name);subheading.setText(description(name));refreshActive();
    }
    private String description(String name){
        switch(name){
            case "Overview":return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d",Locale.ENGLISH));
            case "Patients":return user.role==Role.DOCTOR?"Patients assigned to your appointments.":"Patient registration and contact details.";
            case "Appointments":return user.role==Role.DOCTOR?"Your appointments and upcoming visits.":"Schedule and manage patient visits.";
            case "Consultations":return "Consultation notes, prescriptions, and fees.";
            case "Billing":return "Patient bills and payment records.";
            case "Staff & requests":return "Staff accounts and password requests.";
            default:return "Account details and password requests.";
        }
    }
    private void refreshActive(){JComponent screen=screens.get(active);if(screen instanceof GuiUtil.Refreshable)((GuiUtil.Refreshable)screen).refresh();}
    private void poll(){
        try{Session.verified();if(user.role==Role.ADMIN){int count=new AccountService().requests().size();alerts.setText(count==0?"Requests":"Requests ("+count+")");}
            if(active.equals("Overview")||active.equals("Staff & requests")||active.equals("My profile"))refreshActive();
        }catch(SecurityException ex){expired(ex.getMessage());}catch(Exception ex){subheading.setText("Connection interrupted. Refresh to try again.");}
    }
    private void expired(String message){updates.stop();GuiUtil.info(this,message);signOut();}
    private void signOut(){updates.stop();Session.logout();dispose();new Login().setVisible(true);}
    /** Short laptop displays can scroll the page without crushing the records table. */
    private static class PageViewport extends JPanel implements Scrollable {
        PageViewport(JComponent content){super(new BorderLayout());setOpaque(false);add(content);}
        @Override public Dimension getPreferredSize(){return new Dimension(super.getPreferredSize().width,Math.max(620,super.getMinimumSize().height));}
        @Override public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        @Override public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 18;}
        @Override public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(18,r.height-36);}
        @Override public boolean getScrollableTracksViewportWidth(){return true;}
        @Override public boolean getScrollableTracksViewportHeight(){return getParent() instanceof JViewport&&getParent().getHeight()>=getPreferredSize().height;}
    }
}
