package gui;

import dao.AppointmentDAO;
import dao.BillingDAO;
import dao.PatientDAO;
import security.Role;
import security.Session;
import service.AccountService;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** A restrained overview of the clinic schedule and the signed-in role's work. */
public class DashboardPanel extends JPanel implements GuiUtil.Refreshable {
    private final Role role=Session.current().role;
    private final JPanel calendar=new JPanel(new GridLayout(0,7,4,4));
    private final JLabel monthTitle=Theme.label("",22,Theme.INK,true);
    private final JLabel agendaTitle=Theme.label("Today's schedule",20,Theme.INK,true);
    private final JLabel agendaDate=Theme.label("",15,Theme.MUTED,false);
    private final JLabel[] values=new JLabel[4];
    private final JLabel notice=Theme.label("",15,Theme.MUTED,false);
    private final JComboBox<String> dateMode=new JComboBox<>(new String[]{"Appointment date","Date booked"});
    private final DefaultTableModel agenda=GuiUtil.tableModel("Time","Patient","Doctor","Status");
    private final JTable agendaTable=new JTable(agenda);
    private final JPanel agendaViews=new JPanel(new CardLayout());
    private final JLabel emptyAgenda=Theme.label("No visits today.",17,Theme.MUTED,false);
    private final JLabel error=Theme.label("",13,new Color(0xB34747),false);
    private final Runnable openStaff;
    private final Runnable openAppointments;
    private List<Object[]> appointments=new ArrayList<>();
    private YearMonth month=YearMonth.now();
    private LocalDate selected=LocalDate.now();

    public DashboardPanel(Runnable openStaff) {
        this(openStaff,()->{});
    }

    public DashboardPanel(Runnable openStaff,Runnable openAppointments) {
        this.openStaff=openStaff;
        this.openAppointments=openAppointments;
        setLayout(new BorderLayout(0,20));
        setOpaque(false);
        add(buildStats(),BorderLayout.NORTH);

        JPanel body=new JPanel(new GridBagLayout());
        body.setOpaque(false);
        JPanel calendarCard=buildCalendar();
        JPanel side=new JPanel(new BorderLayout(0,20));
        side.setOpaque(false);
        side.add(buildAgenda(),BorderLayout.CENTER);
        side.add(buildRequests(),BorderLayout.SOUTH);

        GridBagConstraints g=new GridBagConstraints();
        g.gridx=0;g.gridy=0;g.weightx=.56;g.weighty=1;
        g.fill=GridBagConstraints.BOTH;g.insets=new Insets(0,0,0,20);
        body.add(calendarCard,g);
        g.gridx=1;g.weightx=.44;g.insets=new Insets(0,0,0,0);
        body.add(side,g);
        add(body,BorderLayout.CENTER);
        error.setVisible(false);
        add(error,BorderLayout.SOUTH);
        Theme.polish(this);
        renderCalendar();
    }

    private JPanel buildStats() {
        JPanel stats=new JPanel(new GridLayout(1,4,16,0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(800,120));
        String[] names=role==Role.DOCTOR
                ?new String[]{"My patients","Today","Upcoming","Completed"}
                :new String[]{"Patients","Today","Upcoming",role.bills()?"To bill":"Doctors"};
        for(int i=0;i<names.length;i++) {
            JPanel card=Theme.card(new BorderLayout(0,6));
            card.add(Theme.label(names[i],16,Theme.MUTED,false),BorderLayout.NORTH);
            values[i]=Theme.label("—",34,Theme.INK,true);
            card.add(values[i],BorderLayout.CENTER);
            stats.add(card);
        }
        return stats;
    }

    private JPanel buildCalendar() {
        JPanel card=Theme.card(new BorderLayout(0,14));
        card.setMinimumSize(new Dimension(385,360));
        JPanel header=new JPanel(new BorderLayout(12,12));
        header.setOpaque(false);
        header.add(monthTitle,BorderLayout.WEST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        controls.setOpaque(false);
        JButton previous=iconButton(-1,"Previous month");
        JButton next=iconButton(1,"Next month");
        JButton today=Theme.quiet("Today");
        today.setFont(Theme.font(15,false));
        today.setMargin(new Insets(6,12,6,12));
        today.setPreferredSize(new Dimension(Math.max(78,today.getUI().getPreferredSize(today).width),Theme.controlHeight(today,40)));
        previous.addActionListener(e->move(-1));
        next.addActionListener(e->move(1));
        today.addActionListener(e->{selected=LocalDate.now();month=YearMonth.from(selected);renderCalendar();});
        controls.add(previous);controls.add(today);controls.add(next);
        header.add(controls,BorderLayout.EAST);
        card.add(header,BorderLayout.NORTH);
        calendar.setOpaque(false);
        card.add(calendar,BorderLayout.CENTER);

        JPanel calendarOptions=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        calendarOptions.setOpaque(false);
        dateMode.setName("calendarDateMode");
        dateMode.setFont(Theme.font(12,false));
        dateMode.setToolTipText("Display visits by their appointment date or the date the booking was created");
        int dateModeHeight=Theme.controlHeight(dateMode,30);
        dateMode.setPreferredSize(new Dimension(Math.max(164,dateMode.getUI().getPreferredSize(dateMode).width),dateModeHeight));dateMode.setMinimumSize(new Dimension(100,dateModeHeight));
        dateMode.addActionListener(e->renderCalendar());
        calendarOptions.add(dateMode);
        card.add(calendarOptions,BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAgenda() {
        JPanel card=Theme.card(new BorderLayout(0,18));
        card.setMinimumSize(new Dimension(300,260));
        JPanel header=new JPanel(new BorderLayout(12,0));
        header.setOpaque(false);
        header.add(agendaTitle,BorderLayout.WEST);
        header.add(agendaDate,BorderLayout.EAST);
        card.add(header,BorderLayout.NORTH);

        agendaTable.setName("dailyAgenda");
        agendaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane records=new JScrollPane(agendaTable);
        records.setBorder(BorderFactory.createEmptyBorder());
        records.setPreferredSize(new Dimension(330,230));
        agendaViews.setOpaque(false);
        agendaViews.add(records,"records");

        JPanel empty=new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JPanel content=Theme.column();
        JLabel icon=new JLabel(new CalendarIcon(32,Theme.MUTED));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(icon);
        content.add(Box.createVerticalStrut(18));
        emptyAgenda.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(emptyAgenda);
        content.add(Box.createVerticalStrut(20));
        JButton action=Theme.quiet(role.booksAppointments()?"Add appointment":"View appointments");
        action.setName("dashboardAppointments");
        action.setFont(Theme.font(15,false));
        action.setForeground(Theme.TEAL);
        action.setAlignmentX(Component.CENTER_ALIGNMENT);
        action.addActionListener(e->openAppointments.run());
        content.add(action);
        empty.add(content);
        agendaViews.add(empty,"empty");
        card.add(agendaViews,BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRequests() {
        JPanel card=Theme.card(new BorderLayout(12,8));
        card.setPreferredSize(new Dimension(320,112));
        JPanel text=Theme.column();
        text.add(Theme.label(role==Role.ADMIN?"Staff requests":"My account",19,Theme.INK,true));
        text.add(Box.createVerticalStrut(6));
        text.add(notice);
        card.add(text,BorderLayout.CENTER);
        JButton open=iconButton(1,role==Role.ADMIN?"View staff requests":"Open account profile");
        open.setBorderPainted(false);
        open.setContentAreaFilled(false);
        open.addActionListener(e->openStaff.run());
        card.add(open,BorderLayout.EAST);
        return card;
    }

    private JButton iconButton(int direction,String description) {
        JButton button=Theme.quiet("");
        button.setIcon(new ChevronIcon(direction,Theme.INK));
        button.setMargin(new Insets(0,0,0,0));
        button.setPreferredSize(new Dimension(40,Theme.controlHeight(button,40)));
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleName(description);
        return button;
    }

    @Override public void refresh() {
        try {
            appointments=new AppointmentDAO().getAll("");
            values[0].setText(String.valueOf(new PatientDAO().getAll("").size()));
            LocalDate now=LocalDate.now();
            int today=0,upcoming=0,completed=0;
            for(Object[] appointment:appointments) {
                LocalDate date=((java.sql.Date)appointment[3]).toLocalDate();
                if(date.equals(now))today++;
                if(!date.isBefore(now)&&"Scheduled".equals(appointment[5]))upcoming++;
                if("Completed".equals(appointment[5]))completed++;
            }
            values[1].setText(String.valueOf(today));
            values[2].setText(String.valueOf(upcoming));
            int fourth=role==Role.DOCTOR?completed:role.bills()
                    ?new BillingDAO().unbilledForCombo().size():new AppointmentDAO().doctorsForCombo().size();
            values[3].setText(String.valueOf(fourth));
            if(role==Role.ADMIN) {
                int count=new AccountService().requests().size();
                notice.setText(count==0?"No pending requests":count+" pending "+(count==1?"request":"requests"));
            } else notice.setText(Session.current().role+" account");
            error.setVisible(false);
            renderCalendar();
        } catch(Exception ex) {
            error.setText("Unable to refresh overview: "+ex.getMessage());
            error.setVisible(true);
        }
    }

    private LocalDate calendarDate(Object[] row) {
        if(dateMode.getSelectedIndex()==1&&row.length>7&&row[7] instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp)row[7]).toLocalDateTime().toLocalDate();
        return ((java.sql.Date)row[3]).toLocalDate();
    }

    private void move(int offset) {
        month=month.plusMonths(offset);
        selected=month.atDay(1);
        renderCalendar();
    }

    private void renderCalendar() {
        monthTitle.setText("<html>"+month.format(DateTimeFormatter.ofPattern("MMMM"))+"<br>"+month.getYear()+"</html>");
        calendar.removeAll();
        for(String day:new String[]{"Mon","Tue","Wed","Thu","Fri","Sat","Sun"}) {
            JLabel label=Theme.label(day,14,Theme.MUTED,false);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            calendar.add(label);
        }
        LocalDate start=month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()-1);
        for(int i=0;i<42;i++) {
            LocalDate date=start.plusDays(i);
            long count=appointments.stream().filter(a->calendarDate(a).equals(date)).count();
            JButton day=new CalendarDay(date,count,date.equals(selected),YearMonth.from(date).equals(month));
            day.setName("day-"+date);
            day.setToolTipText(date+" · "+count+(dateMode.getSelectedIndex()==0?" scheduled visits":" bookings created"));
            day.addActionListener(e->{selected=date;month=YearMonth.from(date);renderCalendar();});
            calendar.add(day);
        }
        boolean today=selected.equals(LocalDate.now());
        agendaTitle.setText(dateMode.getSelectedIndex()==1?"Bookings created":today?"Today's schedule":"Daily schedule");
        agendaDate.setText(selected.format(DateTimeFormatter.ofPattern("MMM d")));
        agenda.setRowCount(0);
        appointments.stream().filter(a->calendarDate(a).equals(selected))
                .sorted(Comparator.comparing(a->a[4].toString()))
                .forEach(a->agenda.addRow(new Object[]{a[4].toString().substring(0,5),
                    a[1].toString().replaceFirst("^\\d+ - ",""),a[2].toString().replaceFirst("^\\d+ - ",""),a[5]}));
        emptyAgenda.setText(dateMode.getSelectedIndex()==1?"No bookings created.":today?"No visits today.":"No visits on this day.");
        ((CardLayout)agendaViews.getLayout()).show(agendaViews,agenda.getRowCount()==0?"empty":"records");
        calendar.revalidate();calendar.repaint();
    }

    private static class CalendarDay extends JButton {
        private final LocalDate date;
        private final long count;
        private final boolean selected,inMonth;
        CalendarDay(LocalDate date,long count,boolean selected,boolean inMonth) {
            this.date=date;this.count=count;this.selected=selected;this.inMonth=inMonth;
            setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);
            setPreferredSize(new Dimension(48,44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(date+", "+count+" appointments");
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int size=Math.min(44,Math.min(getWidth()-2,getHeight()-2));
            int x=(getWidth()-size)/2,y=(getHeight()-size)/2;
            if(selected||getModel().isRollover()) {
                g.setColor(selected?Theme.TEAL:Theme.BG);
                g.fillRoundRect(x,y,size,size,12,12);
            }
            if(hasFocus()) {
                g.setColor(Theme.TEAL);g.setStroke(new BasicStroke(1.4f));
                g.drawRoundRect(x,y,size-1,size-1,12,12);
            }
            g.setColor(selected?Color.WHITE:inMonth?Theme.INK:Theme.MUTED);
            g.setFont(Theme.font(17,selected));
            String number=String.valueOf(date.getDayOfMonth());
            FontMetrics fm=g.getFontMetrics();
            g.drawString(number,(getWidth()-fm.stringWidth(number))/2,(getHeight()-fm.getHeight())/2+fm.getAscent());
            if(count>0) {
                g.setColor(selected?Color.WHITE:Theme.TEAL);
                g.fillOval((getWidth()-4)/2,y+size-7,4,4);
            }
            g.dispose();
        }
    }

    private static class ChevronIcon implements Icon {
        private final int direction;
        private final Color color;
        ChevronIcon(int direction,Color color){this.direction=direction;this.color=color;}
        @Override public int getIconWidth(){return 18;}
        @Override public int getIconHeight(){return 18;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            int center=x+9,dx=direction>0?2:-2;
            g.drawLine(center-dx,y+5,center+dx,y+9);g.drawLine(center+dx,y+9,center-dx,y+13);g.dispose();
        }
    }

    private static class CalendarIcon implements Icon {
        private final int size;
        private final Color color;
        CalendarIcon(int size,Color color){this.size=size;this.color=color;}
        @Override public int getIconWidth(){return size;}
        @Override public int getIconHeight(){return size;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x,y);g.scale(size/32.0,size/32.0);g.setColor(color);
            g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.drawRoundRect(5,7,22,21,3,3);g.drawLine(5,13,27,13);g.drawLine(11,4,11,10);g.drawLine(21,4,21,10);
            g.drawLine(11,19,14,19);g.drawLine(19,19,22,19);g.drawLine(11,24,14,24);g.dispose();
        }
    }
}
