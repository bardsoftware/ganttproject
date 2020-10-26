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
package net.sourceforge.ganttproject.util;

import java.awt.Color;
import biz.ganttproject.core.option.ColorOption;

public class ColorConvertion {

  /** @return the color as hexadecimal version like #RRGGBB */
  public static String getColor(Color color) {
    return ColorOption.Util.getColor(color);
  }

  /** parse a string as hew and return the corresponding color. */
  public static Color determineColor(String hexString) {
    return ColorOption.Util.determineColor(hexString);
  }
}
