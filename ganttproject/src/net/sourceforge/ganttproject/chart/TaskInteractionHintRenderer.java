package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Graphics;

/** Note paint of the graphic Area */

public class TaskInteractionHintRenderer {
    private Color myColor = new Color((float) 0.930, (float) 0.930,
            (float) 0.930);

    /** The notes to paint */

    String n = new String();

    /** The coords */
    int x, y;

    boolean draw;

    public TaskInteractionHintRenderer() {
        draw = false;
    }

    public TaskInteractionHintRenderer(String s, int x, int y) {
        this.n = s;
        this.x = x;
        this.y = y;
        this.draw = true;
    }

    public void setDraw(boolean d) {
        draw = d;
    }

    public boolean getDraw() {
        return draw;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setString(String s) {
        n = s;
    }

    public void paint(Graphics g) {
        if (draw) {
            g.setColor(myColor);
            g.fillRect(x - 2, y, 70, 16);
            g.setColor(Color.black);
            g.drawRect(x - 2, y, 70, 16);
            g.drawString(n, x, y + 12);
        }
    }
}