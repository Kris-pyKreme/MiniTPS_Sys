package gui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

public final class Theme {
    public static final Color BG=new Color(0xF4F7FA), INK=new Color(0x17333C), MUTED=new Color(0x75858E),
            TEAL=new Color(0x087F8C), PALE=new Color(0xE7F5F4), LINE=new Color(0xE3EBEF), WHITE=Color.WHITE;
    private Theme() { }
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont",new Font("SansSerif",Font.PLAIN,13));
        UIManager.put("Panel.background",BG); UIManager.put("Label.foreground",INK);
        UIManager.put("Component.arc",12); UIManager.put("Button.arc",12); UIManager.put("TextComponent.arc",10);
        UIManager.put("Component.focusColor",TEAL); UIManager.put("Component.accentColor",TEAL);
        UIManager.put("TextField.background",WHITE); UIManager.put("PasswordField.background",WHITE);
        UIManager.put("Table.rowHeight",38); UIManager.put("Table.selectionBackground",PALE);
        UIManager.put("Table.selectionForeground",INK); UIManager.put("Table.gridColor",LINE);
        UIManager.put("TableHeader.background",new Color(0xF8FAFC)); UIManager.put("TableHeader.foreground",MUTED);
        UIManager.put("ScrollBar.thumbArc",99); UIManager.put("ScrollBar.width",10);
        UIManager.put("Button.margin",new Insets(9,16,9,16));
        UIManager.put("TextField.margin",new Insets(9,10,9,10));
        UIManager.put("PasswordField.margin",new Insets(9,10,9,10));
        UIManager.put("ComboBox.padding",new Insets(7,9,7,9));
        UIManager.put("OptionPane.background",WHITE);
    }
    public static JLabel label(String text,int size,Color color,boolean bold) {
        JLabel l=new JLabel(text); l.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size)); l.setForeground(color); return l;
    }
    public static JButton primary(String text) {
        JButton b=new JButton(text); b.setBackground(TEAL); b.setForeground(WHITE); b.setFont(b.getFont().deriveFont(Font.BOLD)); return b;
    }
    public static JButton quiet(String text) {
        JButton b=new JButton(text); b.setBackground(WHITE); b.setForeground(TEAL); return b;
    }
    public static JPanel card(LayoutManager layout) {
        JPanel p=new JPanel(layout); p.setBackground(WHITE);
        p.setBorder(new CompoundBorder(new RoundedBorder(LINE,18),new EmptyBorder(18,18,18,18))); return p;
    }
    public static JPanel column() {
        JPanel p=new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); return p;
    }
    public static void polish(Container root) {
        for(Component c:root.getComponents()) {
            if(c instanceof JTable) {
                JTable t=(JTable)c; t.setRowHeight(40); t.setShowVerticalLines(false); t.setShowHorizontalLines(true);
                t.setIntercellSpacing(new Dimension(0,1)); t.setFillsViewportHeight(true);
                t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,11)); t.getTableHeader().setPreferredSize(new Dimension(0,38));
                DefaultTableCellRenderer renderer=new DefaultTableCellRenderer(); renderer.setBorder(new EmptyBorder(0,10,0,10));
                t.setDefaultRenderer(Object.class,renderer);
            }
            if(c instanceof JPanel && ((JPanel)c).getBorder() instanceof TitledBorder) {
                TitledBorder b=(TitledBorder)((JPanel)c).getBorder(); b.setTitleColor(TEAL); b.setTitleFont(new Font("SansSerif",Font.BOLD,13));
                b.setBorder(new RoundedBorder(LINE,14));
                ((JPanel)c).setBorder(new CompoundBorder(b,new EmptyBorder(8,6,10,6)));
                ((JPanel)c).setBackground(WHITE);
            }
            if(c instanceof Container) polish((Container)c);
        }
    }
    private static class RoundedBorder extends AbstractBorder {
        final Color color; final int arc;
        RoundedBorder(Color c,int a){color=c;arc=a;}
        @Override public Insets getBorderInsets(Component c){return new Insets(1,1,1,1);}
        @Override public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){
            Graphics2D p=(Graphics2D)g.create(); p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(color); p.drawRoundRect(x,y,w-1,h-1,arc,arc); p.dispose();
        }
    }
}
