package gui;

import dao.*;
import security.*;
import service.AccountService;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel implements GuiUtil.Refreshable {
    private final Role role=Session.current().role;
    private final JPanel calendar=new JPanel(new GridLayout(0,7,5,5));
    private final JLabel monthTitle=Theme.label("",17,Theme.INK,true), agendaTitle=Theme.label("",15,Theme.INK,true);
    private final JLabel[] values=new JLabel[4];
    private final JLabel notice=Theme.label("",12,Theme.MUTED,false);
    private final JComboBox<String> dateMode=new JComboBox<>(new String[]{"Appointment date","Date booked"});
    private final DefaultTableModel agenda=GuiUtil.tableModel("Time","Patient","Doctor","Status");
    private final JTable agendaTable=new JTable(agenda);
    private final JLabel emptyAgenda=Theme.label("No appointments for this day.",11,Theme.MUTED,false);
    private List<Object[]> appointments=new ArrayList<>();
    private YearMonth month=YearMonth.now(); private LocalDate selected=LocalDate.now();
    private final JLabel error=Theme.label(" ",12,new Color(0xB34747),false);
    private final Runnable openStaff;

    public DashboardPanel(Runnable openStaff) {
        this.openStaff=openStaff;setLayout(new BorderLayout(0,20));setOpaque(false);
        JPanel stats=new JPanel(new GridLayout(1,4,14,0));stats.setOpaque(false);
        String[] names=role==Role.DOCTOR?new String[]{"MY PATIENTS","TODAY’S VISITS","UPCOMING VISITS","COMPLETED VISITS"}
                :new String[]{"REGISTERED PATIENTS","TODAY’S VISITS","UPCOMING VISITS",role.bills()?"UNBILLED CONSULTATIONS":"CARE PROVIDERS"};
        String[] hints={role==Role.DOCTOR?"Patients assigned to you":"Your clinic community","Appointments scheduled today","Scheduled from today onward",role.bills()?"Ready for billing":"Your care team"};
        if(role==Role.DOCTOR)hints[3]="Completed appointments";
        for(int i=0;i<4;i++) {
            JPanel card=Theme.card(new BorderLayout(0,9));card.add(Theme.label(names[i],10,Theme.MUTED,true),BorderLayout.NORTH);
            values[i]=Theme.label("—",29,i==1?Theme.TEAL:Theme.INK,true);card.add(values[i],BorderLayout.CENTER);card.add(Theme.label(hints[i],10,Theme.MUTED,false),BorderLayout.SOUTH);stats.add(card);
        }
        add(stats,BorderLayout.NORTH);
        JPanel body=new JPanel(new GridBagLayout());body.setOpaque(false);
        JPanel calendarCard=Theme.card(new BorderLayout(0,14));
        JPanel calTop=new JPanel(new BorderLayout());calTop.setOpaque(false);
        JPanel calHead=Theme.column();calHead.add(Theme.label("Appointment calendar",18,Theme.INK,true));calHead.add(Box.createVerticalStrut(5));calHead.add(Theme.label("Select a day to explore your schedule",11,Theme.MUTED,false));calTop.add(calHead,BorderLayout.WEST);
        dateMode.setName("calendarDateMode");agendaTable.setName("dailyAgenda");
        dateMode.setFont(dateMode.getFont().deriveFont(11f));dateMode.addActionListener(e->renderCalendar());calTop.add(dateMode,BorderLayout.EAST);
        JPanel calBody=new JPanel(new BorderLayout(0,13));calBody.setOpaque(false);
        JPanel monthBar=new JPanel(new BorderLayout());monthBar.setOpaque(false);monthBar.add(monthTitle,BorderLayout.WEST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));controls.setOpaque(false);
        JButton prev=Theme.quiet("‹"),today=Theme.quiet("Today"),next=Theme.quiet("›");
        prev.setToolTipText("Previous month");next.setToolTipText("Next month");prev.addActionListener(e->move(-1));next.addActionListener(e->move(1));today.addActionListener(e->{selected=LocalDate.now();month=YearMonth.from(selected);renderCalendar();});
        controls.add(prev);controls.add(today);controls.add(next);monthBar.add(controls,BorderLayout.EAST);
        calBody.add(monthBar,BorderLayout.NORTH);calendar.setOpaque(false);calBody.add(calendar,BorderLayout.CENTER);
        calendarCard.add(calTop,BorderLayout.NORTH);calendarCard.add(calBody,BorderLayout.CENTER);
        JLabel legend=Theme.label("●  Has appointments       ◯  Today       ■  Selected day",10,Theme.TEAL,false);calendarCard.add(legend,BorderLayout.SOUTH);
        JPanel side=Theme.column();
        JPanel note=Theme.card(new BorderLayout(0,12));note.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));
        note.add(Theme.label(role==Role.ADMIN?"Team requests":"Your workspace",16,Theme.INK,true),BorderLayout.NORTH);
        notice.setVerticalAlignment(SwingConstants.TOP);note.add(notice,BorderLayout.CENTER);
        JButton action=Theme.quiet(role==Role.ADMIN?"Manage staff & requests   →":"View account profile   →");action.addActionListener(e->openStaff.run());note.add(action,BorderLayout.SOUTH);side.add(note);side.add(Box.createVerticalStrut(16));
        JPanel agendaCard=Theme.card(new BorderLayout(0,10));agendaCard.add(agendaTitle,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(agendaTable);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setPreferredSize(new Dimension(360,250));agendaCard.add(scroll,BorderLayout.CENTER);agendaCard.add(emptyAgenda,BorderLayout.SOUTH);side.add(agendaCard);
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=0;g.weightx=.62;g.weighty=1;g.fill=GridBagConstraints.BOTH;g.insets=new Insets(0,0,0,16);body.add(calendarCard,g);
        g.gridx=1;g.weightx=.38;g.insets=new Insets(0,0,0,0);body.add(side,g);add(body,BorderLayout.CENTER);add(error,BorderLayout.SOUTH);
        calendar.setMinimumSize(new Dimension(420,300));Theme.polish(this);renderCalendar();
    }
    @Override public void refresh() {
        try {
            appointments=new AppointmentDAO().getAll("");
            values[0].setText(""+new PatientDAO().getAll("").size());
            LocalDate now=LocalDate.now();int today=0,upcoming=0,completed=0;
            for(Object[] a:appointments){LocalDate d=((java.sql.Date)a[3]).toLocalDate();if(d.equals(now))today++;if(!d.isBefore(now)&&"Scheduled".equals(a[5]))upcoming++;if("Completed".equals(a[5]))completed++;}
            values[1].setText(""+today);values[2].setText(""+upcoming);
            values[3].setText(""+(role==Role.DOCTOR?completed:role.bills()?new BillingDAO().unbilledForCombo().size():new AppointmentDAO().doctorsForCombo().size()));
            if(role==Role.ADMIN){int requests=new AccountService().requests().size();notice.setText("<html><b>"+requests+" pending password "+(requests==1?"request":"requests")+"</b><br><br>Keep your team connected to care.</html>");}
            else notice.setText("<html>"+(role==Role.DOCTOR?"Your assigned visits and patient records.":role==Role.NURSE?"Register patients and prepare for today’s visits.":"Coordinate visits, registrations, and payments.")+"<br><br>Need a new password? Visit your profile.</html>");
            error.setText(" ");renderCalendar();
        }catch(Exception ex){error.setText("Could not refresh dashboard: "+ex.getMessage());}
    }
    private LocalDate calendarDate(Object[] row) {
        if(dateMode.getSelectedIndex()==1 && row.length>7 && row[7] instanceof java.sql.Timestamp) return ((java.sql.Timestamp)row[7]).toLocalDateTime().toLocalDate();
        return ((java.sql.Date)row[3]).toLocalDate();
    }
    private void move(int offset){month=month.plusMonths(offset);selected=month.atDay(1);renderCalendar();}
    private void renderCalendar() {
        monthTitle.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));calendar.removeAll();
        for(String day:new String[]{"MON","TUE","WED","THU","FRI","SAT","SUN"}){JLabel l=Theme.label(day,10,Theme.MUTED,true);l.setHorizontalAlignment(SwingConstants.CENTER);calendar.add(l);}
        LocalDate start=month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()-1);
        for(int i=0;i<42;i++) {
            LocalDate date=start.plusDays(i);long count=appointments.stream().filter(a->calendarDate(a).equals(date)).count();
            JButton day=new CalendarDay(date,count,date.equals(selected),date.getMonth()==month.getMonth());
            day.setName("day-"+date);
            day.setToolTipText(date+" · "+count+(dateMode.getSelectedIndex()==0?" scheduled appointments":" appointments booked"));
            day.addActionListener(e->{selected=date;month=YearMonth.from(date);renderCalendar();});calendar.add(day);
        }
        agendaTitle.setText(selected.format(DateTimeFormatter.ofPattern("MMM d"))+" · "+(dateMode.getSelectedIndex()==0?"Daily schedule":"Booked appointments"));
        agenda.setRowCount(0);
        appointments.stream().filter(a->calendarDate(a).equals(selected)).sorted(Comparator.comparing(a->a[4].toString())).forEach(a->agenda.addRow(new Object[]{a[4].toString().substring(0,5),a[1].toString().replaceFirst("^\\d+ - ",""),a[2].toString().replaceFirst("^\\d+ - ",""),a[5]}));
        emptyAgenda.setVisible(agenda.getRowCount()==0);
        calendar.revalidate();calendar.repaint();
    }
    private static class CalendarDay extends JButton {
        private final LocalDate date; private final long count; private final boolean selected,inMonth;
        CalendarDay(LocalDate date,long count,boolean selected,boolean inMonth){
            this.date=date;this.count=count;this.selected=selected;this.inMonth=inMonth;
            setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);
            setPreferredSize(new Dimension(68,46));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(date+", "+count+" appointments");
        }
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            g.setColor(selected?Theme.TEAL:count>0?Theme.PALE:getModel().isRollover()?Theme.BG:Color.WHITE);
            g.fillRoundRect(1,1,w-2,h-2,12,12);
            if(date.equals(LocalDate.now())||hasFocus()){g.setColor(Theme.TEAL);g.setStroke(new BasicStroke(1.5f));g.drawRoundRect(1,1,w-3,h-3,12,12);}
            g.setColor(selected?Color.WHITE:inMonth?Theme.INK:new Color(0xBDCAD0));g.setFont(new Font("SansSerif",Font.BOLD,12));
            String number=""+date.getDayOfMonth();g.drawString(number,(w-g.getFontMetrics().stringWidth(number))/2,count>0?h/2:h/2+4);
            if(count>0){g.setFont(new Font("SansSerif",Font.PLAIN,9));String visits=count+" visit"+(count==1?"":"s");g.drawString(visits,(w-g.getFontMetrics().stringWidth(visits))/2,h-6);}
            g.dispose();
        }
    }
}
