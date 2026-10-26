package gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Small vector icons keep navigation crisp without depending on installed symbol fonts. */
public final class LineIcon implements Icon {
    private final String name;private final int size;private final Color color;
    public LineIcon(String name,int size,Color color){this.name=name;this.size=size;this.color=color;}
    @Override public int getIconWidth(){return size;}
    @Override public int getIconHeight(){return size;}
    @Override public void paintIcon(Component c,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(size/24.0,size/24.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(color);g.setStroke(new BasicStroke(1.65f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(name){
            case "overview":for(int a:new int[]{3,14})for(int b:new int[]{3,14})g.draw(new RoundRectangle2D.Double(a,b,7,7,1.5,1.5));break;
            case "patients":g.drawOval(8,3,8,8);g.draw(new Arc2D.Double(4,13,16,13,0,180,Arc2D.OPEN));break;
            case "appointments":case "calendar":g.drawRoundRect(3,5,18,16,3,3);g.drawLine(3,10,21,10);g.drawLine(8,3,8,7);g.drawLine(16,3,16,7);g.fillOval(7,14,2,2);g.fillOval(12,14,2,2);break;
            case "consultations":g.drawRoundRect(5,4,14,18,2,2);g.drawRoundRect(9,2,6,4,1,1);g.drawLine(9,13,15,13);g.drawLine(12,10,12,16);break;
            case "billing":g.drawRoundRect(4,3,16,18,2,2);g.drawLine(8,8,16,8);g.drawLine(8,12,16,12);g.drawLine(8,16,13,16);break;
            case "staff":g.drawLine(3,9,12,3);g.drawLine(12,3,21,9);g.drawLine(5,9,5,21);g.drawLine(19,9,19,21);g.drawLine(9,12,9,21);g.drawLine(15,12,15,21);g.drawLine(3,21,21,21);g.drawLine(3,9,21,9);break;
            case "profile":g.drawOval(2,2,20,20);g.drawOval(8,6,8,8);g.draw(new Arc2D.Double(6,15,12,10,0,180,Arc2D.OPEN));break;
            case "logout":g.drawLine(10,4,4,4);g.drawLine(4,4,4,20);g.drawLine(4,20,10,20);g.drawLine(10,12,21,12);g.drawLine(17,8,21,12);g.drawLine(17,16,21,12);break;
            case "refresh":g.draw(new Arc2D.Double(4,4,16,16,40,290,Arc2D.OPEN));g.drawLine(19,3,20,9);g.drawLine(20,9,14,8);break;
            case "bell":g.draw(new Arc2D.Double(6,3,12,14,0,180,Arc2D.OPEN));g.drawLine(6,10,6,15);g.drawLine(18,10,18,15);g.drawLine(4,17,20,17);g.drawLine(6,15,4,17);g.drawLine(18,15,20,17);g.draw(new Arc2D.Double(9,17,6,5,180,180,Arc2D.OPEN));break;
            case "left":g.drawLine(15,5,8,12);g.drawLine(8,12,15,19);break;
            case "right":case "arrow":g.drawLine(9,5,16,12);g.drawLine(16,12,9,19);if(name.equals("arrow"))g.drawLine(3,12,16,12);break;
            case "cross":g.draw(new Path2D.Double(){{moveTo(9,3);lineTo(15,3);lineTo(15,9);lineTo(21,9);lineTo(21,15);lineTo(15,15);lineTo(15,21);lineTo(9,21);lineTo(9,15);lineTo(3,15);lineTo(3,9);lineTo(9,9);closePath();}});break;
            default:g.drawOval(4,4,16,16);
        }
        g.dispose();
    }
}
