package clinic;

import gui.GuiUtil;
import gui.Login;
import gui.Theme;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Swing layout checks: typed glyphs must fit inside the editor's clipping rectangle. */
class InputLayoutTest {
    private static final String SAMPLE="AgjypQÉ 09171234567";

    @Test void formTextAndPasswordLinesFitEvenInNarrowRows() throws Exception {
        onEdt(()->{
            Theme.install();JPanel panel=new JPanel(new GridBagLayout());
            JTextField phone=new JTextField(SAMPLE),email=new JTextField("patient@example.com");
            JPasswordField password=new JPasswordField(SAMPLE);
            GuiUtil.addRow(panel,0,0,"Contact number",phone);GuiUtil.addRow(panel,0,1,"Email",email);
            GuiUtil.addRow(panel,1,0,"Password",password);
            for(int width:new int[]{760,500}){
                panel.setSize(width,260);panel.doLayout();
                assertFits(phone);assertFits(email);assertFits(password);
            }
            return null;
        });
    }
    @Test void searchLineFitsInsideItsPadding() throws Exception {
        onEdt(()->{
            Theme.install();JTextField search=new JTextField(SAMPLE);
            JPanel panel=GuiUtil.searchBar(search,new JButton("Search"));panel.setSize(640,150);panel.doLayout();
            assertFits(search);return null;
        });
    }
    @Test void comboUsesAtLeastItsNativeHeight() throws Exception {
        onEdt(()->{
            Theme.install();JPanel panel=new JPanel(new GridBagLayout());
            JComboBox<String> combo=new JComboBox<>(new String[]{"Scheduled","Completed"});
            GuiUtil.addRow(panel,0,0,"Status",combo);panel.setSize(500,150);panel.doLayout();
            Dimension natural=combo.getUI().getPreferredSize(combo);
            assertTrue(combo.getHeight()>=natural.height,"Combo box is shorter than its text and padding");return null;
        });
    }
    @Test void consultationAreasFitThreeCompleteTextLines() throws Exception {
        onEdt(()->{
            Theme.install();JPanel panel=new JPanel(new GridBagLayout());
            JTextArea area=new JTextArea(SAMPLE+"\n"+SAMPLE+"\n"+SAMPLE,3,20);area.setLineWrap(true);
            JScrollPane scroll=new JScrollPane(area);GuiUtil.addRow(panel,0,0,"Diagnosis",scroll);Theme.polish(panel);
            panel.setSize(600,180);panel.doLayout();scroll.doLayout();scroll.getViewport().doLayout();
            int line=area.getFontMetrics(area.getFont()).getHeight();
            assertTrue(scroll.getViewport().getHeight()>=3*line,"Consultation editor must show three full rows");return null;
        });
    }
    @Test void staffDialogFieldsFitTheirNaturalHeight() throws Exception {
        onEdt(()->{
            Theme.install();JPanel panel=new JPanel(new GridLayout(2,1,0,8));
            JTextField name=new JTextField(SAMPLE);JPasswordField password=new JPasswordField(SAMPLE);
            panel.add(name);panel.add(password);panel.setSize(panel.getPreferredSize());panel.doLayout();
            assertFits(name);assertFits(password);return null;
        });
    }
    @Test @EnabledIfSystemProperty(named="clinic.ui.tests",matches="true")
    void loginEditorsFitTheirLargerFont() throws Exception {
        onEdt(()->{
            Theme.install();Login login=new Login();login.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            try{
                login.setVisible(true);
                JTextField username=(JTextField)find(login,"username");JPasswordField password=(JPasswordField)find(login,"password");
                username.setText(SAMPLE);password.setText(SAMPLE);login.validate();
                assertFits(username);assertFits(password);
            }finally{login.dispose();}
            return null;
        });
    }
    private void assertFits(JTextField field) throws Exception {
        Insets insets=field.getInsets();int available=field.getHeight()-insets.top-insets.bottom;
        int line=field.getFontMetrics(field.getFont()).getHeight();
        assertTrue(available>=line,"Editor has "+available+"px for a "+line+"px text line (height "+field.getHeight()+", insets "+insets+")");
        Rectangle2D text=field.modelToView2D(0);assertNotNull(text);
        assertTrue(text.getY()>=insets.top && text.getMaxY()<=field.getHeight()-insets.bottom,
                "Typed text extends outside the editor's clipping rectangle: "+text);
    }
    private Component find(Container root,String name){
        for(Component c:root.getComponents()){
            if(name.equals(c.getName()))return c;
            if(c instanceof Container){Component found=find((Container)c,name);if(found!=null)return found;}
        }
        return null;
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
