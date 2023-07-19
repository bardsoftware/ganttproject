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

import biz.ganttproject.core.chart.canvas.Canvas.HAlignment;
import biz.ganttproject.core.chart.canvas.Canvas.Label;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment;
import biz.ganttproject.core.chart.canvas.TextMetrics;

import java.awt.*;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Paints text labels.
 * @author Dmitry Barashev
 */
public class TextPainter extends AbstractTextPainter {
  private Graphics2D myGraphics;
  private final TextLengthCalculatorImpl myTextLengthCalculator;

  public TextPainter(Properties props, Supplier<Font> baseFont) {
    super(props, baseFont);
    myTextLengthCalculator = new TextLengthCalculatorImpl(null);
  }

  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
    myTextLengthCalculator.setGraphics(myGraphics);
  }

  @Override
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
    var font = text.getFont() == null ? myGraphics.getFont() : text.getFont().asAwtFont(myBaseFont.get().getSize2D());
    int textHeight = font.getSize();
    Style.Padding padding = style.getPadding();
    switch (alignHor) {
      case LEFT -> xleft += padding.getLeft();
      case CENTER -> xleft = xleft - (label.lengthPx + padding.getX()) / 2 + padding.getLeft();
      case RIGHT -> xleft = xleft - (label.lengthPx + padding.getRight());
    }
    switch (alignVer) {
      case CENTER -> ybottom = ybottom + (textHeight + padding.getY()) / 2 - padding.getBottom();
      case TOP -> ybottom = ybottom + (textHeight + padding.getY()) + padding.getTop();
      case BOTTOM -> ybottom -= (padding.getBottom() + myGraphics.getFontMetrics().getDescent());
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
    var oldFont = myGraphics.getFont();
    myGraphics.setFont(font);
    myGraphics.drawString(label.text, xleft, ybottom);
    myGraphics.setFont(oldFont);
  }

  @Override
  protected Map<String, Object> getFontStyles(Font font, Color color) {
    return Map.of("font", font, "color", color);
  }

  @Override
  protected TextMetrics getTextMetrics() {
    return myTextLengthCalculator;
  }

  @Override
  protected TextMetrics getTextMetrics(Map<String, Object> styles) {
    Graphics2D graphics = (Graphics2D) myGraphics.create();
    graphics.setFont((Font) styles.get("font"));
    graphics.setColor((Color) styles.get("color"));
    return new TextLengthCalculatorImpl(graphics);
  }

  @Override
  protected void paint(Text t, Label label, int x, int y, Map<String, Object> styles) {
    Font savedFont = myGraphics.getFont();
    Color savedColor = myGraphics.getColor();

    myGraphics.setFont((Font) styles.get("font"));
    myGraphics.setColor((Color) styles.get("color"));
    Style style = new Style(myProperties, t.getStyle());
    paint(x, y, t.getHAlignment(), t.getVAlignment(), t, label, style);

    myGraphics.setFont(savedFont);
    myGraphics.setColor(savedColor);
  }
}
