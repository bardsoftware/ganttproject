/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import net.sourceforge.ganttproject.util.ColorConvertion;

import biz.ganttproject.core.chart.canvas.Canvas;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Encapsulates style information for rendering graphic primitives. Styles
 * are CSS-like and stored in {@link java.util.Properties}. Keys there are composed
 * from primitive's style name and specific property name, e.g.
 * {@code text.timeline.label.border}. Values roughly correspond to the values of corresponding
 * properties in CSS. See docs of the subclasses in this class for more information.
 *
 * @author Dmitry Barashev
 */
class Style {
  /**
   * Padding which is added between text and border. Property name is 'padding' and
   * its value is four space-delimited numbers, which specify padding at top, right, bottom and left
   * sides, in this order. Measurement unit is pixel always.
   *
   * Example: text.foo.padding = 2 2 2 2
   */
  static class Padding {
    private List<Integer> myValues;

    public Padding(Collection<Integer> values) {
      myValues = Lists.newArrayList(values);
      while (myValues.size() < 4) {
        myValues.add(0);
      }
    }

    static Padding parse(String value) {
      if (value == null) {
        return new Padding(Arrays.asList(0, 0, 0, 0));
      }

      String[] values = value.trim().split("\\s+");
      return new Padding(Collections2.transform(Arrays.asList(values), new Function<String, Integer>() {
        @Override
        public Integer apply(String input) {
          return Integer.valueOf(input);
        }
      }));
    }

    int getX() {
      return getLeft() + getRight();
    }

    int getY() {
      return getTop() + getBottom();
    }

    int getTop() {
      return myValues.get(0);
    }

    int getRight() {
      return myValues.get(1);
    }

    int getBottom() {
      return myValues.get(2);
    }

    int getLeft() {
      return myValues.get(3);
    }
  }

  /**
   * Border style. Property name is 'border' and in the value only color is supported,
   * and it should be a 6-digit hex RGB value prefixed with #
   * Border is always 1px thick and is drawn at all sides.
   *
   * Example: text.foo.border = #000000
   */
  static class Border {
    private final java.awt.Color myColor;

    Border(java.awt.Color color) {
      myColor = color;
    }

    java.awt.Color getColor() {
      return myColor;
    }

    static Border parse(String value) {
      if (value == null) {
        return null;
      }
      return new Border(ColorConvertion.determineColor(value));
    }

  }

  static class Color {

  }

  /**
   * Background color. Property name is 'background-color' and value is
   * a 6-digit hex RGB value prefixed with #
   *
   * Example: text.foo.background-color = #ffffff
   */
  static class BackgroundColor {
    private final java.awt.Color myColor;

    BackgroundColor(java.awt.Color color) {
      myColor = color;
    }

    java.awt.Color get() {
      return myColor;
    }

    public static BackgroundColor parse(String value) {
      if (value == null) {
        return null;
      }
      return new BackgroundColor(ColorConvertion.determineColor(value));
    }

  }

  private Padding myPadding;
  private Border myBorder;
  private Color myColor;
  private BackgroundColor myBackground;

  Style(Properties props, String styleName) {
    myPadding = Padding.parse(props.getProperty(styleName + ".padding"));
    myBackground = BackgroundColor.parse(props.getProperty(styleName + ".background-color"));
    myBorder = Border.parse(props.getProperty(styleName + ".border"));
  }

  Padding getPadding() {
    return myPadding;
  }

  BackgroundColor getBackgroundColor(Canvas.Shape primitive) {
    if (primitive.getBackgroundColor() != null) {
      return new BackgroundColor(primitive.getBackgroundColor());
    }
    return myBackground;
  }

  Border getBorder(Canvas.Shape primitive) {
    if (primitive.getForegroundColor() != null) {
      return new Border(primitive.getForegroundColor());
    }
    return myBorder;
  }
}
