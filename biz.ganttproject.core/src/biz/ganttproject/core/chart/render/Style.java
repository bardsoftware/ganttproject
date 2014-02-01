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
package biz.ganttproject.core.chart.render;

import java.awt.BasicStroke;
import java.awt.Image;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.option.ColorOption;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
  final static BasicStroke DEFAULT_STROKE = new BasicStroke();
  private static final Map<String, Style> ourCache = Maps.newHashMap();
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

  private static BasicStroke parseStroke(String[] components) {
    boolean solid = true;
    for (int i = 0; i < components.length; i++) {
      if ("dashed".equalsIgnoreCase(components[i])) {
        solid = false;
        components[i] = null;
        break;
      } else if ("solid".equalsIgnoreCase(components[i])) {
        solid = true;
        components[i] = null;
        break;        
      }
    }
    int width = 1;
    for (int i = 0; i < components.length; i++) {
      String s = components[i];
      if (s != null && s.endsWith("px")) {
        width = Integer.parseInt(s.substring(0, s.length() - 2));
        components[i] = null;
      }
    }    
    if (solid) {
      return new BasicStroke(width);
    }
    return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 2.5f }, 0f);
  }
  /**
   * Border style. Property name is 'border' and in the value only color is supported,
   * and it should be a 6-digit hex RGB value prefixed with #
   * Border is always 1px thick and is drawn at all sides.
   *
   * Example: text.foo.border = #000000
   */
  static class Border {
    private final BasicStroke myStroke;
    private final java.awt.Color myColor;

    Border(java.awt.Color color) {
      this(color, DEFAULT_STROKE);
    }
    
    Border(java.awt.Color color, BasicStroke stroke) {
      myColor = color;
      myStroke = stroke;
    }


    java.awt.Color getColor() {
      return myColor;
    }

    BasicStroke getStroke() {
      return myStroke;
    }
    
    static Border parse(String value) {
      if (value == null) {
        return null;
      }
      String[] components = value.trim().split("\\s+");
      BasicStroke stroke = parseStroke(components);
      java.awt.Color color = java.awt.Color.BLACK;
      for (String s : components) {
        if (s != null) {
          color = ColorOption.Util.determineColor(s);
          break;
        }
      }
      return new Border(color, stroke);
    }
  }
  
  static class Borders {
    private Border myTop;
    private Border myLeft;
    private Border myRight;
    private Border myBottom;
    private boolean isHomogeneous = false;
    
    Borders(Border top, Border left, Border bottom, Border right) {
      myTop = top;
      myLeft = left;
      myRight = right;
      myBottom = bottom;
    }
    
    public Borders(java.awt.Color color) {
      this(new Border(color));
    }

    public Borders(Border border) {
      this(border, border, border, border);
      isHomogeneous = true;
    }

    public Border getTop() {
      return myTop;
    }
    
    public Border getLeft() {
      return myLeft;
    }
    
    public Border getBottom() {
      return myBottom;
    }
    
    public Border getRight() {
      return myRight;
    }

    public boolean isHomogeneous() {
      return isHomogeneous;
    }
    
    public Borders withColor(java.awt.Color color) {
      return isHomogeneous ? new Borders(new Border(color, myTop.getStroke())) : 
        new Borders(
          new Border(color, myTop.getStroke()), 
          new Border(color, myLeft.getStroke()), 
          new Border(color, myBottom.getStroke()), 
          new Border(color, myRight.getStroke()));
    }
  }

  static class Color {
    private final java.awt.Color myColor;

    Color(java.awt.Color color) {
      myColor = color;
    }

    java.awt.Color get() {
      return myColor;
    }

    public static Color parse(String value) {
      if (value == null) {
        return null;
      }
      return new Color(ColorOption.Util.determineColor(value));
    }
  }

  static class BackgroundImage {
    private static final String BITMAP_PREFIX = "bitmap(";
    private static final String URL_PREFIX = "url(";
    private final Paint myPaint;
    private final Image myImage;
    
    private BackgroundImage(Paint paint, Image image) {
      myPaint = paint;
      myImage = image;
    }
    
    Paint getPaint() {
      return myPaint;
    }
    
    static BackgroundImage parse(String value) {
      if (value == null) {
        return null;
      }
      value = value.trim().toLowerCase();
      if (value.startsWith(BITMAP_PREFIX)) {
        int closingBrace = value.lastIndexOf(')');
        if (closingBrace < 0) {
          return null;
        }
        value = value.substring(BITMAP_PREFIX.length(), closingBrace);
        if (value.length() != 16) {
          return null;
        }
        int[] bitmap = new int[16];
        for (int i = 0; i < value.length(); i++) {
          if (value.charAt(i) == '1') {
            bitmap[i] = 1;
          }
        }
        return new BackgroundImage(new ShapePaint(4, 4, bitmap), null);
      } else if (value.startsWith(URL_PREFIX)) {
        int closingBrace = value.lastIndexOf(')');
        if (closingBrace < 0) {
          return null;
        }
        value = value.substring(URL_PREFIX.length(), closingBrace);
        URL url = Style.class.getResource(value);
        if (url == null) {
          return null;
        }
        try {
          BufferedImage image = ImageIO.read(url);
          return new BackgroundImage(null, image);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }
  }
  enum Visibility {
    VISIBLE, HIDDEN
  }
  
  private Padding myPadding;

  /**
   * Foreground color. Property name is 'color' and value is
   * a 6-digit hex RGB value prefixed with #
   *
   * Example: text.foo.color = #ffffff
   */
  private Color myColor;
  
  /**
   * Background color. Property name is 'background-color' and value is
   * a 6-digit hex RGB value prefixed with #
   *
   * Example: text.foo.background-color = #ffffff
   */
  private Color myBackground;
  private final Properties myProperties;
  private final String myStyleName;
  private BackgroundImage myBackgroundImage;
  private Borders myBorders;

  Style(Properties props, String styleName) {
    myProperties = props;
    myStyleName = styleName;
    myPadding = Padding.parse(props.getProperty(styleName + ".padding"));
    myBackground = Color.parse(props.getProperty(styleName + ".background-color"));
    Border border = Border.parse(props.getProperty(styleName + ".border"));
    Border top = Border.parse(props.getProperty(styleName + ".border-top"));
    Border left = Border.parse(props.getProperty(styleName + ".border-left"));
    Border bottom = Border.parse(props.getProperty(styleName + ".border-bottom"));
    Border right = Border.parse(props.getProperty(styleName + ".border-right"));
    if (top == null && left == null && right == null && bottom == null) {
      myBorders = (border != null) ? new Borders(border) : null;
    } else {
      myBorders = new Borders(top == null ? border : top, left == null ? border : left, bottom == null ? border : bottom, right == null ? border : right);
    }
    myColor = Color.parse(props.getProperty(styleName + ".color"));
  }

  Padding getPadding() {
    return myPadding;
  }

  Color getForegroundColor(Canvas.Shape shape) {
    if (shape.getForegroundColor() != null) {
      return new Color(shape.getForegroundColor());
    }
    return myColor;
  }
  
  Color getBackgroundColor(Canvas.Shape primitive) {
    if (primitive.getBackgroundColor() != null) {
      return new Color(primitive.getBackgroundColor());
    }
    return myBackground;
  }

  Paint getBackgroundPaint(Canvas.Rectangle rect) {
    if (rect.getBackgroundPaint() != null) {
      return rect.getBackgroundPaint();
    }
    if (myBackgroundImage != null) {
      return myBackgroundImage.getPaint();
    }
    myBackgroundImage = BackgroundImage.parse(myProperties.getProperty(myStyleName + ".background-image"));
    return myBackgroundImage == null ? null : myBackgroundImage.getPaint();
  }
  
  Image getBackgroundImage() {
    if (myBackgroundImage != null) {
      return myBackgroundImage.myImage;
    }
    myBackgroundImage = BackgroundImage.parse(myProperties.getProperty(myStyleName + ".background-image"));
    return myBackgroundImage == null ? null : myBackgroundImage.myImage;    
  }
  
  Borders getBorder(Canvas.Shape shape) {
    if ((shape instanceof Canvas.Line || shape instanceof Canvas.Text) && shape.getForegroundColor() != null) {
      return myBorders == null ? new Borders(shape.getForegroundColor()) : myBorders.withColor(shape.getForegroundColor());
    }
    return myBorders;
  }
  
  static Style getStyle(Properties props, String styleName) {
    Style result = ourCache.get(styleName);
    if (result == null) {
      result = new Style(props, styleName);
      ourCache.put(styleName, result);
    }
    return result;
  }
  
  Visibility getVisibility(Canvas.Shape shape) {
    if (!shape.isVisible()) {
      return Visibility.HIDDEN;
    }
    String value = myProperties.getProperty(myStyleName + ".visibility");
    if (value == null) {
     // ugly hack for Rectangles to let RectangleRenderer report if it consumed a shape or not
      return (shape instanceof Canvas.Rectangle) ? Visibility.HIDDEN : Visibility.VISIBLE;  
    }
    try {
      return Visibility.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Visibility.VISIBLE;
    }
  }
  
  Float getOpacity(Canvas.Shape shape) {
    Float result = shape.getOpacity();
    if (result != null) {
      return result;
    }
    String value = myProperties.getProperty(myStyleName + ".opacity");
    return (value == null) ? null : Float.valueOf(value);
  }
}
