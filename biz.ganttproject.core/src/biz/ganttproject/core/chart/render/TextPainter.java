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
package biz.ganttproject.core.chart.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Supplier;

import biz.ganttproject.core.chart.canvas.FontChooser;
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;

/**
 * Paints text labels.
 * @author Dmitry Barashev
 */
public class TextPainter {
  private Graphics2D myGraphics;

  private final Properties myProperties;

  private final TextLengthCalculatorImpl myTextLengthCalculator;

  private final Supplier<Font> myBaseFont;

  public TextPainter(Properties props, Supplier<Font> baseFont) {
    myProperties = props;
    myTextLengthCalculator = new TextLengthCalculatorImpl(null);
    myBaseFont = baseFont;
  }

  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
    myTextLengthCalculator.setGraphics(myGraphics);
  }

  public void paint(Text next) {
    Color foreColor = next.getForegroundColor();
    if (foreColor == null) {
      foreColor = Color.BLACK;
    }
    myGraphics.setColor(foreColor);

    Style style = Style.getStyle(myProperties, next.getStyle());

    Label[] labels = next.getLabels(myTextLengthCalculator);
    if (labels.length == 0) {
      return;
    }

    Label label = labels[0];
    if (label == null) {
      return;
    }
    paint(next.getLeftX(), next.getBottomY(), next.getHAlignment(), next.getVAlignment(), next, label, style);
  }

  private void paint(int xleft, int ybottom, HAlignment alignHor, VAlignment alignVer, Text text, Label label,
      Style style) {
    label.setVisible(true);
    int textHeight = myGraphics.getFont().getSize();
    Style.Padding padding = style.getPadding();
    switch (alignHor) {
    case LEFT:
      xleft += padding.getLeft();
      break;
    case CENTER:
      xleft = xleft - (label.lengthPx + padding.getX()) / 2 + padding.getLeft();
      break;
    case RIGHT:
      xleft = xleft - (label.lengthPx + padding.getRight());
      break;
    }
    switch (alignVer) {
    case CENTER:
      ybottom = ybottom + (textHeight + padding.getY()) / 2 - padding.getBottom();
      break;
    case TOP:
      ybottom = ybottom + (textHeight + padding.getY()) + padding.getTop();
      break;
    case BOTTOM:
      ybottom -= (padding.getBottom() + myGraphics.getFontMetrics().getDescent());
      break;
    }
    Style.Color background = style.getBackgroundColor(text);
    Style.Borders border = style.getBorder(text);
    if (border != null || background != null) {
      int x = xleft - padding.getLeft(), y = ybottom - textHeight - padding.getTop(), w = label.lengthPx
          + padding.getX(), h = textHeight + padding.getY();
      Color savedColor = myGraphics.getColor();
      if (background != null) {
        myGraphics.setColor(background.get());
        myGraphics.fillRect(x, y, w, h);
      }
      if (border != null) {
        RectangleRenderer.renderBorders(myGraphics, border, x, y, w, h);
      }
      myGraphics.setColor(savedColor);
    }
    myGraphics.drawString(label.text, xleft, ybottom);
  }

  public void paint(TextGroup textGroup) {
    TextLengthCalculatorImpl calculator = new TextLengthCalculatorImpl((Graphics2D) myGraphics.create());
    FontChooser fontChooser = new FontChooser(myProperties, calculator, myBaseFont);
    textGroup.setFonts(fontChooser);
    for (int i = 0; i < textGroup.getLineCount(); i++) {
      paintTextLine(textGroup, i);
    }
  }

  private void paintTextLine(TextGroup textGroup, int lineNum) {
    List<Text> line = textGroup.getLine(lineNum);
    Font savedFont = myGraphics.getFont();
    Color savedColor = myGraphics.getColor();

    if (textGroup.getFont(lineNum) == null) {
      return;
    }
    myGraphics.setFont(textGroup.getFont(lineNum));
    myGraphics.setColor(textGroup.getColor(lineNum));

    List<Label[]> labelList = new ArrayList<Label[]>();
    int maxIndex = Integer.MAX_VALUE;
    for (Text t : line) {
      Label[] labels = t.getLabels(myTextLengthCalculator);
      maxIndex = Math.min(maxIndex, labels.length);
      if (maxIndex == 0) {
        return;
      }
      labelList.add(labels);
    }

    for (int i = 0; i < labelList.size(); i++) {
      Label longest = labelList.get(i)[maxIndex - 1];
      Text t = line.get(i);
      Style style = new Style(myProperties, t.getStyle());
      paint(textGroup.getLeftX() + t.getLeftX(), textGroup.getBottomY(lineNum), t.getHAlignment(), t.getVAlignment(),
          t, longest, style);
    }

    myGraphics.setFont(savedFont);
    myGraphics.setColor(savedColor);
  }

}
