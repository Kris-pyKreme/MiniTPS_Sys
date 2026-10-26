package clinic;

import gui.GuiUtil;
import gui.Theme;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordTableLayoutTest {
    @Test void bookedOnCanBeScrolledFullyIntoView() throws Exception {
        onEdt(()->{
            Theme.install();
            DefaultTableModel model=GuiUtil.tableModel("ID","Patient","Doctor","Date","Time","Status","Reason","Booked On");
            model.addRow(new Object[]{1,"Alex Santos","Dr. Reyes","2026-10-27","09:30:00","Scheduled","Checkup","2026-10-27 01:23:27"});
            JTable table=new JTable(model);JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());
            JPanel panel=new JPanel(new BorderLayout());panel.add(scroll);Theme.polish(panel);
            panel.setSize(650,220);panel.doLayout();scroll.doLayout();scroll.getViewport().doLayout();table.doLayout();
            assertEquals(JTable.AUTO_RESIZE_OFF,table.getAutoResizeMode());
            assertTrue(scroll.getHorizontalScrollBar().isVisible());
            assertTrue(table.getPreferredSize().width>scroll.getViewport().getExtentSize().width);
            scroll.getHorizontalScrollBar().setValue(scroll.getHorizontalScrollBar().getMaximum());
            Rectangle cell=table.getCellRect(0,7,true);
            assertTrue(scroll.getViewport().getViewRect().contains(cell),"The complete Booked On cell must be reachable");
            JLabel rendered=(JLabel)table.prepareRenderer(table.getCellRenderer(0,7),0,7);
            Insets insets=rendered.getInsets();
            assertTrue(cell.width>=rendered.getFontMetrics(rendered.getFont()).stringWidth(rendered.getText())+insets.left+insets.right);
            assertTrue(table.getRowHeight()>=rendered.getFontMetrics(rendered.getFont()).getHeight()+insets.top+insets.bottom);
            return null;
        });
    }
    @Test void searchControlsBeginOnTheLeft() throws Exception {
        onEdt(()->{
            Theme.install();JTextField field=new JTextField();JButton search=new JButton("Search"),reset=new JButton("Reset");
            JPanel panel=GuiUtil.searchBar(field,search,reset);panel.setSize(900,70);panel.doLayout();
            assertTrue(field.getX()<30,"Search input should use the available left side");
            assertTrue(field.getBounds().getMaxX()<search.getX());assertTrue(search.getBounds().getMaxX()<reset.getX());
            return null;
        });
    }
    private <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeAndWait(task);
        try{return task.get();}catch(ExecutionException e){
            if(e.getCause() instanceof Error)throw (Error)e.getCause();
            if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;
        }
    }
}
