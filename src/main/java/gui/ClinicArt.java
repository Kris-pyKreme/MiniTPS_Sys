package gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** Resolution-independent clinic illustration; no image downloads or assets required. */
public class ClinicArt extends JComponent {
    public ClinicArt(){setPreferredSize(new Dimension(390,270));}
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(getWidth()/390.0,getHeight()/270.0);
        g.setColor(new Color(0xD8EFEB));g.fillOval(40,12,290,242);
        g.setColor(new Color(0xBCDFD7));g.fillRoundRect(18,221,350,10,10,10);
        // Building and soft shadow.
        g.setColor(new Color(0xBEDDD7));g.fillRoundRect(92,63,224,165,16,16);
        g.setColor(Color.WHITE);g.fillRoundRect(83,48,224,176,16,16);
        g.setColor(new Color(0xE6F3F1));g.fillRoundRect(71,38,247,33,12,12);
        g.setColor(Theme.TEAL);g.fillRoundRect(179,17,31,60,7,7);g.fillRoundRect(165,31,60,31,7,7);
        g.setColor(new Color(0xEDF6F6));g.fillRoundRect(108,95,49,45,8,8);g.fillRoundRect(235,95,49,45,8,8);
        g.setColor(new Color(0xB8D9DB));g.fillRoundRect(179,100,34,124,6,6);
        g.setColor(Theme.TEAL);g.fillRoundRect(171,156,49,68,7,7);
        g.setColor(new Color(0xD1EDE6));g.fillRoundRect(108,157,49,42,8,8);g.fillRoundRect(235,157,49,42,8,8);
        g.setStroke(new BasicStroke(3));g.setColor(Color.WHITE);g.drawLine(133,98,133,137);g.drawLine(111,118,154,118);
        g.drawLine(260,98,260,137);g.drawLine(238,118,281,118);
        g.setColor(new Color(0xE8BA7B));g.fillOval(203,188,5,5);
        // Plants.
        for(int x:new int[]{53,337}) {
            g.setColor(new Color(0x64A993));g.fillOval(x-13,142,28,49);g.fillOval(x-24,165,32,34);g.fillOval(x+1,161,30,35);
            g.setColor(new Color(0x468F7D));g.fillRect(x,175,3,43);
            g.setColor(new Color(0xE9D9BB));g.fillRoundRect(x-15,204,33,23,5,5);
        }
        g.dispose();
    }
}
