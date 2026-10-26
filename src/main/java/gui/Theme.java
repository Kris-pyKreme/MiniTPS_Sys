package gui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.io.InputStream;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;

/** Shared typography and surfaces for every KlinikAccess screen. */
public final class Theme {
    public static final Color BG=new Color(0xFCFCFB), INK=new Color(0x111312), MUTED=new Color(0x60625F),
            TEAL=new Color(0x078490), PALE=new Color(0xE1EFF0), LINE=new Color(0xE3E5E3), WHITE=Color.WHITE,
            LOGIN_BG=new Color(0xEAF3F2);
    private static final Font REGULAR=load("Lato-Regular.ttf"),BOLD=load("Lato-Bold.ttf");
    private Theme() { }
    private static Font load(String name){
        try(InputStream in=Theme.class.getResourceAsStream("/fonts/"+name)){
            if(in==null)throw new IllegalStateException("Missing bundled font: "+name);
            Font font=Font.createFont(Font.TRUETYPE_FONT,in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);return font;
        }catch(Exception e){throw new IllegalStateException("Cannot load KlinikAccess typography",e);}
    }
    public static Font font(int size,boolean bold){return (bold?BOLD:REGULAR).deriveFont((float)size);}
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont",new FontUIResource(font(15,false)));
        UIManager.put("Panel.background",BG);UIManager.put("Label.foreground",INK);
        UIManager.put("Component.arc",14);UIManager.put("Button.arc",16);UIManager.put("TextComponent.arc",14);
        UIManager.put("Component.focusWidth",1);UIManager.put("Component.focusColor",new Color(0xC5E0E2));
        UIManager.put("Component.focusedBorderColor",TEAL);UIManager.put("Component.borderColor",LINE);UIManager.put("Component.accentColor",TEAL);
        UIManager.put("TextField.background",WHITE);UIManager.put("PasswordField.background",WHITE);
        UIManager.put("TextField.foreground",INK);UIManager.put("PasswordField.foreground",INK);
        UIManager.put("TextField.placeholderForeground",new Color(0x898B87));UIManager.put("PasswordField.placeholderForeground",new Color(0x898B87));
        UIManager.put("Table.rowHeight",42);UIManager.put("Table.selectionBackground",PALE);UIManager.put("Table.selectionForeground",INK);
        UIManager.put("Table.gridColor",LINE);UIManager.put("Table.background",WHITE);UIManager.put("Table.foreground",INK);
        UIManager.put("TableHeader.background",new Color(0xF8F9F7));UIManager.put("TableHeader.foreground",MUTED);
        UIManager.put("ScrollBar.thumbArc",99);UIManager.put("ScrollBar.width",9);UIManager.put("ScrollPane.border",BorderFactory.createLineBorder(LINE));
        UIManager.put("Button.margin",new Insets(10,16,10,16));UIManager.put("Button.foreground",INK);UIManager.put("Button.background",WHITE);
        UIManager.put("Button.focusedBorderColor",TEAL);UIManager.put("Button.default.background",TEAL);UIManager.put("Button.default.foreground",WHITE);
        UIManager.put("Button.default.borderColor",TEAL);UIManager.put("Button.default.focusedBorderColor",TEAL);UIManager.put("Button.default.hoverBorderColor",TEAL);UIManager.put("Button.default.focusColor",PALE);
        UIManager.put("TextField.margin",new Insets(10,12,10,12));UIManager.put("PasswordField.margin",new Insets(10,12,10,12));
        UIManager.put("ComboBox.padding",new Insets(9,12,9,12));UIManager.put("ComboBox.background",WHITE);
        UIManager.put("CheckBox.icon.selectedBackground",TEAL);UIManager.put("CheckBox.icon.selectedBorderColor",TEAL);
        UIManager.put("OptionPane.background",WHITE);
    }
    public static JLabel label(String text,int size,Color color,boolean bold){
        JLabel l=new JLabel(text);l.setFont(font(size,bold));l.setForeground(color);return l;
    }
    public static JButton primary(String text){
        JButton b=new JButton(text);b.setBackground(TEAL);b.setForeground(WHITE);b.setFont(font(15,true));return b;
    }
    public static JButton quiet(String text){
        JButton b=new JButton(text);b.setBackground(WHITE);b.setForeground(INK);b.setFont(font(15,false));return b;
    }
    public static JButton iconButton(String icon,String description){
        JButton b=quiet("");b.setIcon(new LineIcon(icon,19,INK));b.setPreferredSize(new Dimension(42,40));
        b.setToolTipText(description);b.getAccessibleContext().setAccessibleName(description);return b;
    }
    public static JPanel card(LayoutManager layout){return surface(layout,WHITE,20,20);}
    public static JPanel surface(LayoutManager layout,Color background,int radius,int padding){
        JPanel p=new RoundedPanel(layout,radius);p.setBackground(background);
        p.setBorder(new CompoundBorder(new RoundedBorder(LINE,radius),new EmptyBorder(padding,padding,padding,padding)));return p;
    }
    public static JPanel brand(int badgeSize,int textSize){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));p.setOpaque(false);
        JPanel badge=new RoundedPanel(new GridBagLayout(),14);badge.setBackground(TEAL);badge.setPreferredSize(new Dimension(badgeSize,badgeSize));
        badge.add(new JLabel(new LineIcon("cross",Math.max(20,badgeSize/2),WHITE)));p.add(badge);p.add(label("KlinikAccess",textSize,INK,true));return p;
    }
    public static JPanel column(){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    public static void polish(Container root){
        for(Component c:root.getComponents()){
            if(c instanceof JTable){
                JTable t=(JTable)c;t.setRowHeight(42);t.setShowVerticalLines(false);t.setShowHorizontalLines(true);
                t.setIntercellSpacing(new Dimension(0,1));t.setFillsViewportHeight(true);t.setBackground(WHITE);
                t.getTableHeader().setFont(font(13,true));t.getTableHeader().setPreferredSize(new Dimension(0,40));t.getTableHeader().setReorderingAllowed(false);
                t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
                    @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col){
                        super.getTableCellRendererComponent(table,value,selected,focus,row,col);setBorder(new EmptyBorder(0,12,0,12));setFont(font(13,false));return this;
                    }
                });
            }
            if(c instanceof JScrollPane){
                JScrollPane s=(JScrollPane)c;s.getViewport().setBackground(WHITE);
                if(s.getViewport().getView() instanceof JTable)s.setBorder(BorderFactory.createEmptyBorder());
                else if(s.getViewport().getView() instanceof javax.swing.text.JTextComponent){
                    s.setBorder(new CompoundBorder(new RoundedBorder(LINE,12),new EmptyBorder(6,8,6,8)));
                    s.getViewport().getView().setFont(font(14,false));
                }
            }
            if(c instanceof JPanel && ((JPanel)c).getBorder() instanceof TitledBorder){
                TitledBorder b=(TitledBorder)((JPanel)c).getBorder();b.setTitleColor(INK);b.setTitleFont(font(16,true));b.setBorder(new RoundedBorder(LINE,18));
                ((JPanel)c).setBorder(new CompoundBorder(b,new EmptyBorder(8,8,10,8)));((JPanel)c).setBackground(WHITE);
            }
            if(c instanceof Container)polish((Container)c);
        }
    }
    private static class RoundedPanel extends JPanel {
        private final int radius;
        RoundedPanel(LayoutManager layout,int radius){super(layout);this.radius=radius;setOpaque(false);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(getBackground());p.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);p.dispose();super.paintComponent(g);
        }
    }
    private static class RoundedBorder extends AbstractBorder {
        final Color color;final int arc;RoundedBorder(Color c,int a){color=c;arc=a;}
        @Override public Insets getBorderInsets(Component c){return new Insets(1,1,1,1);}
        @Override public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){
            Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(color);p.drawRoundRect(x,y,w-1,h-1,arc,arc);p.dispose();
        }
    }
}
