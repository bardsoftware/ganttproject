/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.util;

import java.awt.FontMetrics;

/**
 * This class groups static methods together to handle strings.
 *
 * @author Maarten Bezemer
 */
public class StringUtils {

  /**
   * @return string with the given amount of spaces padded to the left. For
   *         negative amounts the original string will be returned
   */
  static public String padLeft(String string, int padding) {
    if (padding > 0) {
      padding += string.length();
      return String.format("%1$" + padding + "s", string);
    }
    return string;
  }

  /**
   * @return string with the given amount of spaces padded to the right. For
   *         negative amounts the original string will be returned
   */
  static public String padRight(String string, int padding) {
    if (padding > 0) {
      padding += string.length();
      return String.format("%1$-" + padding + "s", string);
    }
    return string;
  }

  /** @return a comma separated list showing the names of the given objects */
  public static String getDisplayNames(Object[] objects) {
    if (objects.length == 1) {
      return objects[0].toString();
    }
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < objects.length; i++) {
      result.append(objects[i].toString());
      if (i < objects.length - 1) {
        result.append(", ");
      }
    }
    return result.toString();
  }

  /**
   * Truncates the <code>str</code> String according to <code>width</code> and
   * <code>fontMetrics</code>.
   *
   * @return the truncated String.
   */
  public static String getTruncatedString(String str, int width, FontMetrics fontMetrics) {
    int strWidth = fontMetrics.stringWidth(str);
    if (strWidth <= width) {
      // No truncation is required
      return str;
    }

    // Start with width of "..." part
    int maxWidth = fontMetrics.stringWidth("...");

    // Find amount of chars to reach the given width
    int i;
    for (i = 0; i < str.length() && maxWidth < width; i++) {
      maxWidth += fontMetrics.charWidth(str.charAt(i));
      ;
    }
    // last check went over allowed width, so use up to this length (see
    // substring documentation)
    return str.substring(0, i) + "...";
  }

  public static boolean isEmptyOrNull(String s) {
    if (s == null) {
      return true;
    }
    if (s.isEmpty()) {
      return true;
    }
    return false;
  }


}
