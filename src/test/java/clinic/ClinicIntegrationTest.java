package clinic;

import dao.*;
import database.*;
import gui.*;
import security.*;
import service.AccountService;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in only against a disposable database whose name ends in _test. */
@EnabledIfSystemProperty(named="clinic.integration", matches="true")
class ClinicIntegrationTest {
    @BeforeAll static void clinicTimeZone(){TimeZone.setDefault(TimeZone.getTimeZone("Asia/Manila"));}
    private final AccountService accounts=new AccountService();
    private User admin,nurse,doctor,receptionist,otherDoctor;
    @BeforeEach void fixtures() throws Exception {
        try(Connection c=DBConnection.getConnection()) {
            assertTrue(c.getCatalog().endsWith("_test"),"Integration tests require a disposable *_test database");
        }
        DatabaseSetup.upgrade();
        admin=accounts.authenticate("admin","admin123".toCharArray(),Role.ADMIN);assertNotNull(admin);Session.login(admin);
        execute("DELETE FROM patients WHERE first_name LIKE 'IT_%'");execute("DELETE FROM staff_users WHERE username LIKE 'it_%'");
        for(Role role:new Role[]{Role.NURSE,Role.DOCTOR,Role.RECEPTIONIST}) accounts.create("it_"+role.name(),"Integration "+role,role,role==Role.DOCTOR?1:null,"testpass123".toCharArray());
        accounts.create("it_other","Other doctor",Role.DOCTOR,2,"testpass123".toCharArray());
        nurse=login(Role.NURSE);doctor=login(Role.DOCTOR);receptionist=login(Role.RECEPTIONIST);
        otherDoctor=accounts.authenticate("it_other","testpass123".toCharArray(),Role.DOCTOR);
        Session.login(admin);
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(()->{for(Frame f:Frame.getFrames())f.dispose();});
        Session.login(admin);
        execute("DELETE FROM patients WHERE first_name LIKE 'IT_%'");execute("DELETE FROM staff_users WHERE username LIKE 'it_%'");Session.logout();
    }
    private User login(Role role)throws Exception{return accounts.authenticate("it_"+role.name(),"testpass123".toCharArray(),role);}
    private void execute(String sql)throws Exception{try(Connection c=DBConnection.getConnection();Statement s=c.createStatement()){s.executeUpdate(sql);}}
    private int number(String sql)throws Exception{try(Connection c=DBConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){assertTrue(r.next());return r.getInt(1);}}
    private int patient(String name)throws Exception{new PatientDAO().insert(name,"Test",java.sql.Date.valueOf("2000-01-01"),"Female","","","");return ((Number)new PatientDAO().getAll(name).get(0)[0]).intValue();}
    private int appointment(int patient,int doctor,String time)throws Exception{
        new AppointmentDAO().insert(patient,doctor,java.sql.Date.valueOf("2099-06-01"),Time.valueOf(time),"Scheduled","IT_visit");
        return number("SELECT appointment_id FROM appointments WHERE patient_id="+patient+" AND doctor_id="+doctor);
    }
    @Test void patientContactFormatIsEnforcedForInsertAndUpdate()throws Exception {
        Session.login(nurse);PatientDAO dao=new PatientDAO();
        dao.insert("IT_contact","Test",java.sql.Date.valueOf("2000-01-01"),"Female","9123456789","","");
        int id=((Number)dao.getAll("IT_contact").get(0)[0]).intValue();
        assertEquals("9123456789",dao.getAll("IT_contact").get(0)[5]);
        assertThrows(IllegalArgumentException.class,()->dao.insert("IT_badcontact","Test",java.sql.Date.valueOf("2000-01-01"),"Female","09123456789","",""));
        assertThrows(IllegalArgumentException.class,()->dao.update(id,"IT_contact","Test",java.sql.Date.valueOf("2000-01-01"),"Female","8123456789","",""));
        assertEquals("9123456789",dao.getAll("IT_contact").get(0)[5]);
        dao.update(id,"IT_contact","Test",java.sql.Date.valueOf("2000-01-01"),"Female","9987654321","","");
        assertEquals("9987654321",dao.getAll("IT_contact").get(0)[5]);
    }
    @Test void roleSelectorMustMatchDatabaseRole()throws Exception{
        assertNull(accounts.authenticate("it_NURSE","testpass123".toCharArray(),Role.ADMIN));
        assertNull(accounts.authenticate("it_NURSE","wrong".toCharArray(),Role.NURSE));
        assertNotNull(accounts.authenticate("it_NURSE","testpass123".toCharArray(),Role.NURSE));
        assertThrows(IllegalArgumentException.class,()->accounts.create("it_bad","Bad doctor",Role.DOCTOR,null,"testpass123".toCharArray()));
    }
    @Test void requestsAreDeduplicatedAndResetRevokesSessions()throws Exception{
        Session.login(nurse);accounts.requestReset(nurse.username,nurse.role);accounts.requestReset(nurse.username,nurse.role);
        assertTrue(accounts.requestStatus().startsWith("Pending"));
        assertThrows(SecurityException.class,()->accounts.resetPassword(nurse.id,"newpass123".toCharArray()));
        assertThrows(SecurityException.class,accounts::requests);
        Session.login(admin);assertEquals(1,accounts.requests().size());accounts.resetPassword(nurse.id,"newpass123".toCharArray());assertEquals(0,accounts.requests().size());
        assertNull(accounts.authenticate(nurse.username,"testpass123".toCharArray(),Role.NURSE));
        User updated=accounts.authenticate(nurse.username,"newpass123".toCharArray(),Role.NURSE);assertNotNull(updated);
        Session.login(nurse);assertThrows(SecurityException.class,Session::verified);
        Session.login(updated);assertDoesNotThrow(Session::verified);assertTrue(accounts.requestStatus().startsWith("Resolved"));
        accounts.requestReset("missing-user",Role.NURSE);Session.login(admin);assertEquals(0,accounts.requests().size());
        accounts.requestReset(updated.username,Role.NURSE);assertEquals(1,accounts.requests().size());
    }
    @Test void changedAccessInvalidatesOldSessionAndDoctorLinkIsRequired()throws Exception {
        accounts.updateStaff(nurse.id,"Changed staff",Role.RECEPTIONIST,null);
        Session.login(nurse);assertThrows(SecurityException.class,Session::verified);
        assertNull(accounts.authenticate(nurse.username,"testpass123".toCharArray(),Role.NURSE));
        assertNotNull(accounts.authenticate(nurse.username,"testpass123".toCharArray(),Role.RECEPTIONIST));
        Session.login(admin);
        assertThrows(IllegalArgumentException.class,()->accounts.updateStaff(admin.id,"Admin",Role.NURSE,null));
        assertThrows(IllegalArgumentException.class,()->accounts.updateStaff(doctor.id,"Doctor",Role.DOCTOR,null));
    }
    @Test void clinicWorkflowAndFinancialTotals()throws Exception{
        Session.login(receptionist);int p=patient("IT_workflow"),a=appointment(p,1,"09:00:00");
        assertThrows(SQLException.class,()->new AppointmentDAO().insert(p,1,java.sql.Date.valueOf("2099-06-01"),Time.valueOf("09:00:00"),"Scheduled","duplicate"));
        assertThrows(SecurityException.class,()->new ConsultationDAO().save(a,"diagnosis","","",new BigDecimal("500.00")));
        Session.login(doctor);new ConsultationDAO().save(a,"diagnosis","notes","prescription",new BigDecimal("500.00"));
        assertEquals(1,number("SELECT COUNT(*) FROM appointments WHERE appointment_id="+a+" AND status='Completed'"));
        int consultation=number("SELECT consultation_id FROM consultations WHERE appointment_id="+a);
        assertThrows(SQLException.class,()->new ConsultationDAO().save(a,"again","","",new BigDecimal("500.00")));
        Session.login(receptionist);new BillingDAO().insert(consultation,new BigDecimal("50.00"),"Paid","Cash");
        assertEquals(550,number("SELECT total_amount FROM billing WHERE consultation_id="+consultation));
        assertThrows(IllegalArgumentException.class,()->new BillingDAO().insert(consultation,new BigDecimal("-1"),"Paid","Cash"));
        Session.login(doctor);new ConsultationDAO().update(consultation,"updated","notes","prescription",new BigDecimal("600.00"));
        assertEquals(650,number("SELECT total_amount FROM billing WHERE consultation_id="+consultation));
        assertThrows(SecurityException.class,()->new BillingDAO().getAll(""));
        Session.login(admin);new PatientDAO().delete(p);assertEquals(0,number("SELECT COUNT(*) FROM billing WHERE consultation_id="+consultation));
    }
    @Test void doctorScopeAppliesToAllSearchAlternativesAndWrites()throws Exception{
        int p1=patient("IT_assigned"),p2=patient("IT_other");int a1=appointment(p1,1,"10:00:00"),a2=appointment(p2,2,"10:00:00");
        Session.login(doctor);
        assertEquals(1,new PatientDAO().getAll("IT_").size());assertEquals(0,new PatientDAO().getAll("IT_other").size());
        assertEquals(1,new AppointmentDAO().getAll("Scheduled").size());
        assertEquals(1,new AppointmentDAO().pendingForCombo().size());
        assertThrows(SecurityException.class,()->new ConsultationDAO().save(a2,"other patient","","",BigDecimal.ZERO));
        new ConsultationDAO().save(a1,"same diagnosis","","",BigDecimal.ZERO);
        Session.login(otherDoctor);new ConsultationDAO().save(a2,"same diagnosis","","",BigDecimal.ZERO);
        assertEquals(1,new ConsultationDAO().getAll("same diagnosis").size());
        int foreignConsult=number("SELECT consultation_id FROM consultations WHERE appointment_id="+a1);
        assertThrows(SecurityException.class,()->new ConsultationDAO().update(foreignConsult,"bad","","",BigDecimal.ZERO));
    }
    @Test void nurseCanRegisterButCannotMutateScheduleOrReadClinicalAndBillingData()throws Exception{
        Session.login(nurse);int p=patient("IT_nurse");assertTrue(new PatientDAO().getAll("IT_nurse").size()>0);
        assertDoesNotThrow(()->new AppointmentDAO().getAll(""));
        assertThrows(SecurityException.class,()->appointment(p,1,"11:00:00"));
        assertThrows(SecurityException.class,()->new PatientDAO().delete(p));
        assertThrows(SecurityException.class,()->new ConsultationDAO().getAll(""));
        assertThrows(SecurityException.class,()->new BillingDAO().getAll(""));
        Session.logout();assertThrows(SecurityException.class,()->new PatientDAO().getAll(""));
    }
    @Test void calendarHasBothScheduledAndCreationDates()throws Exception{
        int p=patient("IT_calendar");appointment(p,1,"12:00:00");Object[] row=new AppointmentDAO().getAll("IT_calendar").get(0);
        assertEquals("2099-06-01",row[3].toString());assertInstanceOf(Timestamp.class,row[7]);
    }
    @Test @EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
    void actualLoginOpensRoleWorkspace()throws Exception {
        Theme.install();Session.logout();final Login[] login=new Login[1];
        SwingUtilities.invokeAndWait(()->{
            login[0]=new Login();login[0].setVisible(true);
            ((JTextField)find(login[0],"username")).setText("it_NURSE");
            ((JPasswordField)find(login[0],"password")).setText("testpass123");
            ((JComboBox<?>)find(login[0],"role")).setSelectedItem(Role.NURSE);
            ((JButton)find(login[0],"signIn")).doClick();
        });
        boolean opened=false;
        for(int i=0;i<100;i++){
            final boolean[] visible={false};SwingUtilities.invokeAndWait(()->{for(Frame f:Frame.getFrames())if(f instanceof MainFrame&&f.isVisible())visible[0]=true;});
            if(visible[0]){opened=true;break;}Thread.sleep(100);
        }
        assertTrue(opened,"Asynchronous login must open a main window");assertEquals(Role.NURSE,Session.current().role);
        SwingUtilities.invokeAndWait(()->{assertFalse(login[0].isVisible());});
    }
    @Test @EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
    void calendarDateModeAndDaySelectionChangeAgenda()throws Exception {
        int p=patient("IT_calendarui");appointment(p,1,"13:00:00");Theme.install();
        SwingUtilities.invokeAndWait(()->{
            DashboardPanel panel=new DashboardPanel(()->{});panel.refresh();
            JTable table=(JTable)find(panel,"dailyAgenda");assertEquals(0,table.getRowCount());
            ((JComboBox<?>)find(panel,"calendarDateMode")).setSelectedIndex(1);
            assertEquals(1,table.getRowCount());assertTrue(table.getValueAt(0,1).toString().contains("IT_calendarui"));
            ((JButton)find(panel,"day-"+java.time.LocalDate.now().plusDays(1))).doClick();assertEquals(0,table.getRowCount());
        });
    }
    private Component find(Container root,String name) {
        for(Component c:root.getComponents()){
            if(name.equals(c.getName()))return c;
            if(c instanceof Container){Component found=find((Container)c,name);if(found!=null)return found;}
        }
        return null;
    }
    @Test @EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
    void roleNavigationAndAllScreensRender()throws Exception{
        Theme.install();
        for(User user:new User[]{admin,doctor,nurse,receptionist}){
            Session.login(user);
            SwingUtilities.invokeAndWait(()->{
                MainFrame f=new MainFrame();f.setVisible(true);f.validate();
                JButton signOut=(JButton)find(f,"signOut");
                assertTrue(signOut.getWidth()>=signOut.getUI().getPreferredSize(signOut).width,"Sign out label must fit for every role");
                f.navigate("Patients");f.navigate("Appointments");f.navigate("My profile");
                if(user.role.consults())f.navigate("Consultations");else assertThrows(SecurityException.class,()->f.navigate("Consultations"));
                if(user.role.bills())f.navigate("Billing");else assertThrows(SecurityException.class,()->f.navigate("Billing"));
                if(user.role==Role.ADMIN)f.navigate("Staff & requests");else assertThrows(SecurityException.class,()->f.navigate("Staff & requests"));
                f.navigate("Overview");f.dispose();
            });
        }
    }
}
