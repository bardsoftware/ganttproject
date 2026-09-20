/*
Copyright 2003-2026 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui.options;

import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.PaintOption;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the colour swatch built by {@link OptionsPageBuilder} is able to show a
 * {@link Paint}, and that an option which offers no paint is still drawn exactly as a plain
 * background-filled panel would draw it.
 */
public class ColorSwatchPaintTest {
  private static final int SWATCH = 16;
  private static final Color HOST_BACKGROUND = new Color(238, 238, 238);

  /** The default values of every colour option which is shown through a swatch today. */
  private static final Color[] SHIPPED_COLORS = {
      new Color(140, 182, 206),   // ganttChartDefaults.taskDefaultColor, resourceChartColors.normalLoad
      Color.LIGHT_GRAY,           // ganttChartStateDiffColors.taskOnScheduleColor
      new Color(50, 229, 50),     // ...taskAheadOfScheduleColor, resourceChartColors.underLoad
      new Color(229, 50, 50),     // ...taskBehindScheduleColor, resourceChartColors.overLoad
      new Color(0.9f, 1f, 0.17f), // resourceChartColors.dayOff
      null,                       // the option in CalendarEditorPanel starts without a value
  };

  /** An option which offers a texture next to the flat colour which approximates it. */
  private static class TexturedOption extends DefaultColorOption implements PaintOption {
    private final Paint myPaint;

    TexturedOption(String id, Color approximation, Paint paint) {
      super(id, approximation);
      myPaint = paint;
    }

    @Override
    public Paint getPaint() {
      return myPaint;
    }
  }

  /** A 4x4 tile, red over blue. Two colours, so a flat fill cannot be mistaken for it. */
  private static TexturePaint stripes() {
    BufferedImage tile = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = tile.createGraphics();
    g.setColor(Color.RED);
    g.fillRect(0, 0, 4, 2);
    g.setColor(Color.BLUE);
    g.fillRect(0, 2, 4, 2);
    g.dispose();
    return new TexturePaint(tile, new Rectangle(0, 0, 4, 4));
  }

  private static JComponent swatchOf(ColorOption option) {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    // The swatch is the first child of the panel which the builder returns.
    return (JComponent) builder.createColorComponent(option).getJComponent().getComponent(0);
  }

  /** Paints the component inside a host of a known colour, so an inherited background shows up. */
  private static BufferedImage render(JComponent component) {
    JPanel host = new JPanel(null);
    host.setBackground(HOST_BACKGROUND);
    host.setSize(SWATCH, SWATCH);
    host.add(component);
    component.setBounds(0, 0, SWATCH, SWATCH);
    component.doLayout();

    BufferedImage image = new BufferedImage(SWATCH, SWATCH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    host.paint(g);
    g.dispose();
    return image;
  }

  private static Set<Integer> distinctColors(BufferedImage image) {
    Set<Integer> result = new HashSet<>();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        result.add(image.getRGB(x, y));
      }
    }
    return result;
  }

  private static int differingPixels(BufferedImage left, BufferedImage right) {
    assertEquals(left.getWidth(), right.getWidth());
    assertEquals(left.getHeight(), right.getHeight());
    int count = 0;
    for (int y = 0; y < left.getHeight(); y++) {
      for (int x = 0; x < left.getWidth(); x++) {
        if (left.getRGB(x, y) != right.getRGB(x, y)) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * The point of the change: an option which offers a paint gets that paint on the screen.
   * Before the change the swatch was filled with the panel background and came out flat.
   */
  @Test
  public void paintOptionIsDrawnWithItsPaint() {
    TexturePaint stripes = stripes();
    BufferedImage image = render(swatchOf(new TexturedOption("test.textured", Color.GREEN, stripes)));

    Set<Integer> colors = distinctColors(image);
    assertEquals(2, colors.size(), "Expected the two colours of the texture, got " + colors);
    assertTrue(colors.contains(Color.RED.getRGB()), "Expected the texture's red, got " + colors);
    assertTrue(colors.contains(Color.BLUE.getRGB()), "Expected the texture's blue, got " + colors);
  }

  /**
   * The guard on the existing swatches: every option which offers no paint has to come out
   * pixel for pixel like a plain panel filled with its background colour. The reference is built
   * without touching the code under test, so the change cannot move it.
   */
  @Test
  public void optionWithoutPaintLooksLikeAPlainBackgroundFill() {
    for (Color color : SHIPPED_COLORS) {
      BufferedImage actual = render(swatchOf(new DefaultColorOption("test.flat", color)));

      JPanel reference = new JPanel();
      reference.setPreferredSize(new Dimension(SWATCH, SWATCH));
      reference.setBackground(color);
      BufferedImage expected = render(reference);

      assertEquals(0, differingPixels(expected, actual),
          "The swatch of " + color + " no longer looks like a plain background fill");
    }
  }

  /** An option which implements the interface but has no paint to offer falls back to its colour. */
  @Test
  public void paintOptionWithoutPaintFallsBackToItsColor() {
    BufferedImage actual = render(swatchOf(new TexturedOption("test.nopaint", Color.MAGENTA, null)));

    JPanel reference = new JPanel();
    reference.setPreferredSize(new Dimension(SWATCH, SWATCH));
    reference.setBackground(Color.MAGENTA);
    BufferedImage expected = render(reference);

    assertEquals(0, differingPixels(expected, actual));
  }
}
