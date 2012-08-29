/*
Copyright 2003-2012GanttProject Team

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
package biz.ganttproject.core.chart.render;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

/**
 * @author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */
public class ShapePaint extends TexturePaint {
  private final int width, height;

  private final int[] array;

  private final Color foreground, background;

  public ShapePaint(ShapePaint pattern) {
    this(pattern.width, pattern.height, pattern.array, pattern.foreground, pattern.background);
  }

  public ShapePaint(ShapePaint pattern, Color foreground, Color background) {
    this(pattern.width, pattern.height, pattern.array, foreground, background);
  }

  public ShapePaint(int width, int height, int[] array) {
    this(width, height, array, Color.black, Color.white);
  }

  public ShapePaint(int width, int height, int[] array, Color foreground, Color background) {
    super(createTexture(width, height, array, foreground, background), new Rectangle(0, 0, width, height));
    this.width = width;
    this.height = height;
    this.array = array;
    this.foreground = foreground;
    this.background = background;
  }

  private static BufferedImage createTexture(int width, int height, int[] array, Color foreground, Color background) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        image.setRGB(x, y, array[x + y * width] > 0 ? foreground.getRGB() : background.getRGB());
      }
    }
    return image;
  }

  /** @return true if the two shape are the same */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ShapePaint) {
      ShapePaint paint = (ShapePaint) obj;

      if (array.length != paint.array.length) {
        return false;
      }

      for (int i = 0; i < array.length; i++) {
        if (array[i] != paint.array[i]) {
          return false;
        }
      }

      return paint.width == width && paint.height == height;
    }
    return false;
  }

  /** @return a string for the shape */
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("PatternPaint[");
    buffer.append("[width=" + width);
    buffer.append(",height=" + height);
    buffer.append(",array={");
    for (int i = 0; i < array.length; i++) {
      if (i > 0) {
        buffer.append(",");
      }
      buffer.append("" + array[i]);
    }
    buffer.append("},foreground=" + foreground);
    buffer.append(",background=" + background + "]");
    return buffer.toString();
  }

  /** @return the array of the shape on a string */
  public String getArray() {
    String result = "";
    if (array != null) {
      for (int i = 0; i < array.length; i++) {
        result += array[i] + ",";
      }
    }

    return (result.length() != 0) ? result.trim().substring(0, result.trim().length() - 1) : "";
  }

  /** @return the array of the shape */
  public int[] getarray() {
    return array;
  }

  /** @return foreground color */
  public Color getForeground() {
    return foreground;
  }

  /** @return background color */
  public Color getBackground() {
    return background;
  }
}
