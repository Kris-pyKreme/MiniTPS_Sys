package clinic;

import gui.Login;
import gui.Theme;
import security.Role;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Desktop interaction checks; no database is required. */
@EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
class LoginAnimationTest {
    private Login login;

    @BeforeEach void openLogin() throws Exception {
        onEdt(()->{
            Theme.install();login=new Login();login.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            login.setVisible(true);return null;
        });
    }
    @AfterEach void closeLogin() throws Exception {onEdt(()->{if(login!=null)login.dispose();return null;});}

    @Test void typingExpandsOnceAndEscapeRestoresWithoutLosingInput() throws Exception {
        Rectangle original=onEdt(login::getBounds);
        Rectangle expanded=onEdt(this::availableBounds);
        Assumptions.assumeFalse(original.equals(expanded),"The test display must have space for expansion");
        onEdt(()->{
            ((JComboBox<?>)find(login,"role")).setSelectedItem(Role.NURSE);
            ((JPasswordField)find(login,"password")).setText("keepThisPassword");
            find(login,"username").requestFocusInWindow();return null;
        });
        await(()->find(login,"username").isFocusOwner());
        assertEquals(original,onEdt(login::getBounds),"Focus and role selection alone must not expand the window");

        // Real key events continue arriving while the asynchronous bounds animation runs.
        Robot robot=new Robot();robot.setAutoDelay(25);
        for(int key:new int[]{KeyEvent.VK_N,KeyEvent.VK_U,KeyEvent.VK_R,KeyEvent.VK_S,KeyEvent.VK_E}){
            robot.keyPress(key);robot.keyRelease(key);
        }
        await(()->login.getBounds().equals(expanded));
        onEdt(()->{
            assertEquals("nurse",((JTextField)find(login,"username")).getText());
            assertArrayEquals("keepThisPassword".toCharArray(),((JPasswordField)find(login,"password")).getPassword());
            assertEquals(Role.NURSE,((JComboBox<?>)find(login,"role")).getSelectedItem());
            assertTrue(find(login,"username").isFocusOwner(),"Expansion must preserve keyboard focus");return null;
        });
        robot.keyPress(KeyEvent.VK_ESCAPE);robot.keyRelease(KeyEvent.VK_ESCAPE);
        await(()->login.getBounds().equals(original));
        robot.keyPress(KeyEvent.VK_X);robot.keyRelease(KeyEvent.VK_X);
        // Allow more than one animation duration to prove typing does not undo Escape.
        Thread.sleep(650);
        onEdt(()->{
            assertEquals(original,login.getBounds());
            assertEquals("nursex",((JTextField)find(login,"username")).getText());return null;
        });
    }

    @Test void closingDuringExpansionCancelsFurtherWindowUpdates() throws Exception {
        Rectangle original=onEdt(login::getBounds);
        Assumptions.assumeFalse(original.equals(onEdt(this::availableBounds)),"The test display must have space for expansion");
        onEdt(()->{((JTextField)find(login,"username")).setText("admin");return null;});
        await(()->!login.getBounds().equals(original));
        Rectangle closedBounds=onEdt(()->{login.dispose();return login.getBounds();});
        Thread.sleep(650);
        onEdt(()->{
            assertFalse(login.isDisplayable());assertEquals(closedBounds,login.getBounds());return null;
        });
    }

    private Rectangle availableBounds(){
        GraphicsConfiguration monitor=login.getGraphicsConfiguration();Rectangle r=new Rectangle(monitor.getBounds());
        Insets i=Toolkit.getDefaultToolkit().getScreenInsets(monitor);
        r.x+=i.left;r.y+=i.top;r.width-=i.left+i.right;r.height-=i.top+i.bottom;return r;
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
        fail("Desktop transition did not complete within five seconds");
    }
    private <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeAndWait(task);return task.get();
    }
}
