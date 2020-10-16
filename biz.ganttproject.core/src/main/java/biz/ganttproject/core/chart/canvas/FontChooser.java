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
package biz.ganttproject.core.chart.canvas;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;

public class FontChooser {

  private final Properties myProperties;
  private Map<String, Font> myFonts = new HashMap<String, Font>();
  private final TextMetrics myCalculator;
  private final Supplier<Font> myBaseFont;

  public FontChooser(Properties properties, TextMetrics calculator, Supplier<Font> chartBaseFont) {
    myProperties = properties;
    myCalculator = calculator;
    myBaseFont = chartBaseFont;
  }

  public int decreaseBaseFontSize() {
    Map<String, Font> newFonts = new HashMap<String, Font>();
    int minSize = Integer.MAX_VALUE;
    for (String style : myFonts.keySet()) {
      Font f = myFonts.get(style);
      float newSize = f.getSize() - 1f;
      f = f.deriveFont(newSize);
      newFonts.put(style, f);
      minSize = Math.min(minSize, (int)newSize);
    }
    myFonts = newFonts;
    return minSize;
  }

  public int getMarginTop(String style) {
    if ("hidden".equalsIgnoreCase(myProperties.getProperty(style + ".visibility"))) {
      return 0;
    }
    return Integer.parseInt(myProperties.getProperty(style + ".margin-top", "0"));
  }

  public int getTextHeight(String style) {
    if ("hidden".equalsIgnoreCase(myProperties.getProperty(style + ".visibility"))) {
      return 0;
    }
    Font f = getFont(style);
    return myCalculator.getTextHeight(f, "A");
  }

  public int getMarginBottom(String style) {
    if ("hidden".equalsIgnoreCase(myProperties.getProperty(style + ".visibility"))) {
      return 0;
    }
    return Integer.parseInt(myProperties.getProperty(style + ".margin-bottom", "0"));
  }

  public Font getFont(String style) {
    if ("hidden".equalsIgnoreCase(myProperties.getProperty(style + ".visibility"))) {
      return null;
    }
    Font f = myFonts.get(style);
    if (f == null) {      
      String propValue = Strings.nullToEmpty(myProperties.getProperty(style + ".font")).trim();
      if (propValue.isEmpty()) {
        // If .font property is not set then we use the base font
        f = myBaseFont.get();
      } else {
        String[] components = propValue.split("\\s+");
        String last = components[components.length - 1];
        String family = "";
        float absoluteSize;
        try {
          // If the last component of .font property is int/float then
          // we check whether it is a relative increment (it should be prefixed with sign)
          // or an absolute value
          if (last.startsWith("+") || last.startsWith("-")) {            
            absoluteSize = Float.parseFloat(last) + myBaseFont.get().getSize();
          } else {
            absoluteSize = Float.parseFloat(last);
          }
          if (components.length > 1) {
            family = Joiner.on(' ').join(Arrays.asList(components).subList(0, components.length - 1));
          }
          if (family.isEmpty()) {
            f = myBaseFont.get().deriveFont(absoluteSize);
          } else {
            f = Font.decode(family + " 10");
            if (f == null) {
              f = myBaseFont.get();
            }
            f = f.deriveFont(absoluteSize);
          }
        } catch (NumberFormatException e) {
          f = Font.decode(propValue);
        }
      }
      myFonts.put(style, f);
    } 
    return f;
  }

  public Color getColor(String style) {
    return Color.decode(myProperties.getProperty(style + ".color", "#000"));
  }
}
