package clinic;

import gui.Login;
import gui.Theme;
import security.Role;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Desktop interaction checks; no database is required. */
@EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
class LoginInteractionTest {
    private Login login;

    @BeforeEach void openLogin() throws Exception {
        onEdt(()->{
            Theme.install();login=new Login();login.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            login.setVisible(true);return null;
        });
    }
    @AfterEach void closeLogin() throws Exception {
        onEdt(()->{if(login!=null)login.dispose();return null;});
    }

    @Test void typingAndRoleChangesKeepTheChosenWindowSize() throws Exception {
        Rectangle original=onEdt(login::getBounds);
        onEdt(()->{
            ((JComboBox<?>)find(login,"role")).setSelectedItem(Role.NURSE);
            find(login,"username").requestFocusInWindow();return null;
        });
        await(()->find(login,"username").isFocusOwner());

        Robot robot=new Robot();robot.setAutoDelay(20);
        type(robot,"nurse1");
        robot.keyPress(KeyEvent.VK_TAB);robot.keyRelease(KeyEvent.VK_TAB);
        await(()->find(login,"password").isFocusOwner());
        type(robot,"testpass123");
        // Longer than the removed expansion duration: delayed resizing must not occur either.
        Thread.sleep(650);
        onEdt(()->{
            assertEquals(original,login.getBounds(),"Entering credentials must not resize or move the login");
            assertEquals("nurse1",((JTextField)find(login,"username")).getText());
            assertArrayEquals("testpass123".toCharArray(),((JPasswordField)find(login,"password")).getPassword());
            assertEquals(Role.NURSE,((JComboBox<?>)find(login,"role")).getSelectedItem());
            assertTrue(find(login,"password").isFocusOwner(),"Typing must preserve keyboard focus");
            ((JComboBox<?>)find(login,"role")).setSelectedItem(Role.RECEPTIONIST);
            assertEquals(original,login.getBounds(),"Changing the role must not resize the login");return null;
        });
    }

    @Test void showPasswordRestoresMaskingWithoutChangingTheValue() throws Exception {
        onEdt(()->{
            Rectangle original=login.getBounds();
            JPasswordField password=(JPasswordField)find(login,"password");
            JCheckBox show=(JCheckBox)find(login,"showPassword");
            assertNotNull(show,"The show-password control must be available");
            char masked=password.getEchoChar();assertNotEquals((char)0,masked);
            password.setText("demoPassword123");
            show.doClick();assertEquals((char)0,password.getEchoChar());
            assertArrayEquals("demoPassword123".toCharArray(),password.getPassword());
            show.doClick();assertEquals(masked,password.getEchoChar());assertFalse(show.isSelected());
            assertArrayEquals("demoPassword123".toCharArray(),password.getPassword());
            assertEquals(original,login.getBounds());return null;
        });
    }

    private void type(Robot robot,String text){
        for(char c:text.toCharArray()){
            int key=KeyEvent.getExtendedKeyCodeForChar(c);robot.keyPress(key);robot.keyRelease(key);
        }
    }
    private Component find(Container root,String name){
        for(Component c:root.getComponents()){
            if(name.equals(c.getName()))return c;
            if(c instanceof Container){Component found=find((Container)c,name);if(found!=null)return found;}
        }
        return null;
    }
    private void await(Callable<Boolean> condition) throws Exception {
        long deadline=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<deadline){if(onEdt(condition))return;Thread.sleep(25);}
        fail("The expected keyboard focus did not arrive within five seconds");
    }
    private <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeAndWait(task);
        try{return task.get();}catch(ExecutionException e){
            if(e.getCause() instanceof Error)throw (Error)e.getCause();
            if(e.getCause() instanceof Exception)throw (Exception)e.getCause();
            throw e;
        }
    }
}
