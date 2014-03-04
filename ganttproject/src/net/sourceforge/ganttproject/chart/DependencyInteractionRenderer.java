/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
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
      int px = (int) (0.08f * vx);
      int py = (int) (0.08f * vy);
      int total = ((px < 0) ? -px : px) + ((py < 0) ? -py : py);
      px = (int) (px * 10.f / total);
      py = (int) (py * 10.f / total);
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
