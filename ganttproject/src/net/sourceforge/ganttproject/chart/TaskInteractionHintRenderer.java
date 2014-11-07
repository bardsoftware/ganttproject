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

/** Note paint of the graphic Area */

public class TaskInteractionHintRenderer {
  private Color myColor = new Color((float) 0.930, (float) 0.930, (float) 0.930);

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
