/*
Copyright 2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.util;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class MouseUtil {
  public static String toString(MouseEvent e) {
    int modifiers = e.getModifiersEx();
    StringBuilder buf = new StringBuilder();
    append(buf, modifiers, InputEvent.META_DOWN_MASK, "Meta");
    append(buf, modifiers, InputEvent.CTRL_DOWN_MASK, "Ctrl");
    append(buf, modifiers, InputEvent.ALT_DOWN_MASK, "Alt");
    append(buf, modifiers, InputEvent.SHIFT_DOWN_MASK, "Shift");
    append(buf, modifiers, InputEvent.ALT_GRAPH_DOWN_MASK, "Alt Graph");
    if (e instanceof MouseWheelEvent) {
      append(buf, 1, 1, "Wheel");
    } else {
      append(buf, modifiers, InputEvent.BUTTON1_DOWN_MASK, "Button1");
      append(buf, modifiers, InputEvent.BUTTON2_DOWN_MASK, "Button2");
      append(buf, modifiers, InputEvent.BUTTON3_DOWN_MASK, "Button3");
    }
    return buf.toString().trim();
  }

  private static void append(StringBuilder builder, int modifiers, int mask, String text) {
    if ((modifiers & mask) == 0) {
      return;
    }
    if (builder.length() > 0) {
      builder.append('+');
    }
    builder.append(text);
  }
}
