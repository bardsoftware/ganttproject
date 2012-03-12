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
package net.sourceforge.ganttproject.parser;

import java.awt.Color;
import java.util.regex.Pattern;

import net.sourceforge.ganttproject.GanttGraphicArea;

class ColorValueParser {
  public static Color parseString(String value) {
    if (!Pattern.matches("#[0-9abcdefABCDEF]{6}+", value)) {
      return GanttGraphicArea.taskDefaultColor;
    }
    int r, g, b;
    r = Integer.valueOf(value.substring(1, 3), 16).intValue();
    g = Integer.valueOf(value.substring(3, 5), 16).intValue();
    b = Integer.valueOf(value.substring(5, 7), 16).intValue();
    return new Color(r, g, b);
  }
}
