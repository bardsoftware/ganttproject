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
package biz.ganttproject.core.option;

import java.awt.Color;
import java.util.regex.Pattern;

public interface ColorOption extends GPOption<Color> {
  class Util {
    private static Pattern HEX_COLOR = Pattern.compile("#[0-9abcdefABCDEF]{6}+");

    public static String getColor(Color color) {
      String res = "#";

      if (color.getRed() <= 15)
        res += "0";
      res += Integer.toHexString(color.getRed());
      if (color.getGreen() <= 15)
        res += "0";
      res += Integer.toHexString(color.getGreen());
      if (color.getBlue() <= 15)
        res += "0";
      res += Integer.toHexString(color.getBlue());

      return res;
    }

    /** parse a string as hew and return the corresponding color. */
    public static Color determineColor(String hexString) {
      assert isValidColor(hexString) : "Can't parse color " + hexString;
      int r, g, b;
      r = Integer.valueOf(hexString.substring(1, 3), 16).intValue();
      g = Integer.valueOf(hexString.substring(3, 5), 16).intValue();
      b = Integer.valueOf(hexString.substring(5, 7), 16).intValue();
      return new Color(r, g, b);
    }

    public static boolean isValidColor(String hexString) {
      return HEX_COLOR.matcher(hexString).matches();
    }
  }
}
