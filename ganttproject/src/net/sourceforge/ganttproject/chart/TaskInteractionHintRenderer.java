/*
Copyright 2003-2017 Dmitry Barashev, GanttProject Team

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

import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl;

import java.awt.*;

/**
 * Renders a small "hint" (text in a little framed box) at the specified position.
 * Used for rendering tooltips when changing task progress or boundaries on Gantt chart.
 */
public class TaskInteractionHintRenderer {
  private static final Color FG_COLOR = new Color((float) 0.930, (float) 0.930, (float) 0.930);

  private String myText;
  private int x, y;

  public TaskInteractionHintRenderer(String s, int x, int y) {
    this.myText = s;
    this.x = x;
    this.y = y;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setText(String s) {
    myText = s;
  }

  public void paint(Graphics2D g) {
    TextLengthCalculatorImpl calculator = new TextLengthCalculatorImpl(g);
    int lengthPx = calculator.getTextLength(myText) + 4;
    int heightPx = calculator.getTextHeight(myText) + 4;

    g.setColor(FG_COLOR);
    g.fillRect(x - 2, y, lengthPx, heightPx);
    g.setColor(Color.black);
    g.drawRect(x - 2, y, lengthPx, heightPx);
    g.drawString(myText, x, y + heightPx - 2);
  }
}
