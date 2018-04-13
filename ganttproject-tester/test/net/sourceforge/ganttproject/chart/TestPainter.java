/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Painter;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;

/**
 * Painter for tests
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestPainter implements Painter {

  private final TextMetrics myCalculator;

  protected TestPainter(TextMetrics calculator) {
    myCalculator = calculator;
  }

  @Override
  public void prePaint() {
    // TODO Auto-generated method stub

  }

  @Override
  public void paint(Rectangle rectangle) {
    // TODO Auto-generated method stub

  }

  @Override
  public void paint(Line line) {
    // TODO Auto-generated method stub

  }

  @Override
  public void paint(Text text) {
    Label[] labels = text.getLabels(myCalculator);
    for (Label l : labels) {
      l.setVisible(true);
    }
  }

  @Override
  public void paint(TextGroup textGroup) {
    // TODO Auto-generated method stub

  }

  public void paint(Polygon p) {
    // TODO Auto-generated method stub

  }

}
