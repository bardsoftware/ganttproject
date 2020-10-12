/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package biz.ganttproject.core.chart.render;

import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.FontChooser;
import biz.ganttproject.core.chart.canvas.TextMetrics;
import com.google.common.base.Supplier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractTextPainter {
  protected final Properties myProperties;
  protected final Supplier<Font> myBaseFont;

  public AbstractTextPainter(Properties props, Supplier<Font> baseFont) {
    myProperties = props;
    myBaseFont = baseFont;
  }

  abstract public void paint(Text next);

  public void paint(TextGroup textGroup) {
    FontChooser fontChooser = new FontChooser(myProperties, getTextMetrics(), myBaseFont);
    textGroup.setFonts(fontChooser);
    for (int i = 0; i < textGroup.getLineCount(); i++) {
      final int lineNumber = i;
      Font font = textGroup.getFont(lineNumber);
      if (font != null) {
        Map<String, Object> styles = getFontStyles(font, textGroup.getColor(lineNumber));
        paintTextLine(textGroup, lineNumber, styles);
      }
    }
  }

  private void paintTextLine(TextGroup textGroup, int lineNum, Map<String, Object> styles) {
    List<Text> line = textGroup.getLine(lineNum);
    for (Text t : line) {
      Label[] labels = t.getLabels(getTextMetrics());
      paint(t, labels[labels.length - 1], textGroup.getLeftX() + t.getLeftX(), textGroup.getBottomY(lineNum), styles);
    }
  }

  abstract protected Map<String, Object> getFontStyles(Font font, Color color);
  
  abstract protected TextMetrics getTextMetrics();

  abstract protected void paint(Text t, Label label, int x, int y, Map<String, Object> styles);
}
