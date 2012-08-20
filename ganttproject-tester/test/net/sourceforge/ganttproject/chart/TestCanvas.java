/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2012 GanttProject Team

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

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import junit.framework.TestCase;

/**
 * Tests canvas operations.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestCanvas extends TestCase {
  /**
   * Tests whether labels are returned by getPrimitive() method.
   */
  public void testTextLabelSearch() {
    Canvas canvas = new Canvas();
    {
      Text text = canvas.createText(100, 20, "");
      Label label = text.createLabel("foobar", 50, 10);
      label.setVisible(true);
    }
    // Visible labels will be indexed
    assertTrue(canvas.getPrimitive(120, 15) instanceof Text);
    assertNull(canvas.getPrimitive(120, 30));
    assertNull(canvas.getPrimitive(151, 15));
    assertNull(canvas.getPrimitive(99, 15));

    {
      Text text = canvas.createText(200, 20, "");
      text.createLabel("foobar", 50, 10);
    }
    // Labels which have not been made visible are not indexed
    assertNull(canvas.getPrimitive(220, 15));
  }
}
