package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Graphics;

/** Draw arrow between two points */
public class DependencyInteractionRenderer {

    private int x1, x2, y1, y2;

    private boolean draw;

    public DependencyInteractionRenderer() {
        x1 = x2 = y1 = y2 = 0;
        draw = false;
    }

    public DependencyInteractionRenderer(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.draw = true;
    }

    public void setDraw(boolean d) {
        draw = d;
    }

    public boolean getDraw() {
        return draw;
    }

    public void changePoint2(int x2, int y2) {
        this.x2 = x2;
        this.y2 = y2;
    }

    public void paint(Graphics g) {
        if (draw) {
            // draw the line
            g.setColor(Color.black);
            g.drawLine(x1, y1, x2, y2);
            // Draw the triangle
            int xPoints[] = new int[3];
            int yPoints[] = new int[3];
            int vx = x2 - x1;
            int vy = y2 - y1;
            int px = (int) (0.08f * (float) vx);
            int py = (int) (0.08f * (float) vy);
            int total = ((px < 0) ? -px : px) + ((py < 0) ? -py : py);
            px = (int) ((float) px * 10.f / (float) total);
            py = (int) ((float) py * 10.f / (float) total);
            xPoints[0] = x2;
            yPoints[0] = y2;
            xPoints[1] = x2 - px + py / 2;
            yPoints[1] = y2 - py - px / 2;
            xPoints[2] = x2 - px - py / 2;
            yPoints[2] = y2 - py + px / 2;
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }
}