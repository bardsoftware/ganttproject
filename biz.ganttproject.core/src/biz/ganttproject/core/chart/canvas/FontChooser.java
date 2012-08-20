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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FontChooser {

  private Properties myProperties;
  private Map<String, Font> myFonts = new HashMap<String, Font>();
  private TextMetrics myCalculator;

  public FontChooser(Properties properties, TextMetrics calculator) {
    myProperties = properties;
    myCalculator = calculator;
  }

  public void decreaseBaseFontSize() {
    Map<String, Font> newFonts = new HashMap<String, Font>();
    for (String style : myFonts.keySet()) {
      Font f = myFonts.get(style);
      f = f.deriveFont(f.getSize() - 1f);
      newFonts.put(style, f);
    }
    myFonts = newFonts;
  }

  public int getMarginTop(String style) {
    return Integer.parseInt(myProperties.getProperty(style + ".margin-top", "0"));
  }

  public int getTextHeight(String style) {
    Font f = getFont(style);
    return myCalculator.getTextHeight(f, "A");
  }

  public int getMarginBottom(String style) {
    return Integer.parseInt(myProperties.getProperty(style + ".margin-bottom", "0"));
  }

  public Font getFont(String style) {
    Font f = myFonts.get(style);
    if (f == null) {
      f = Font.decode(myProperties.getProperty(style + ".font", "Dialog 10"));
      myFonts.put(style, f);
    }
    return f;
  }

  public Color getColor(String style) {
    return Color.decode(myProperties.getProperty(style + ".color", "#000"));
  }
}
