/*
Copyright 2026

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
package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.canvas.Canvas
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import net.sourceforge.ganttproject.chart.StyledPainterImpl
import net.sourceforge.ganttproject.gui.UIConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pixel tests of the hidden-tasks marker: a dashed line with a solid cap at each end, drawn opaque
 * on the row seam. The colour must stay at least 40 brightness steps away from the row separator it
 * lies on, and the shape is the second cue: a horizontal cut through the mark meets the dashes
 * and the ground between them.
 */
class HiddenTasksMarkerImageTest {

  private val bildBreite = 130
  private val bildHoehe = 50

  private val links = 10
  private val oben = 20
  private val breite = 100

  /** What the mark is drawn on: the empty chart background. */
  private val grundFarbe = Color.WHITE

  /** Paints one mark of [stil] onto [grund] with the real painter, no double. */
  private fun malen(stil: String, hoehe: Int, grund: Color): BufferedImage {
    val bild = BufferedImage(bildBreite, bildHoehe, BufferedImage.TYPE_INT_RGB)
    val g = bild.createGraphics()
    g.color = grund
    g.fillRect(0, 0, bildBreite, bildHoehe)
    val config = ChartUIConfiguration(UIConfiguration(Color.BLUE, false))
    config.rowHeight = 20
    val painter = StyledPainterImpl(config)
    painter.setGraphics(g)
    val canvas = Canvas()
    val rechteck = canvas.createRectangle(links, oben, breite, hoehe)
    rechteck.style = stil
    painter.paint(rechteck)
    g.dispose()
    return bild
  }

  private val balken: BufferedImage by lazy { malen(STYLE_HIDDEN_GAP, HIDDEN_GAP_HEIGHT, grundFarbe) }

  private fun helligkeit(rgb: Int): Int {
    val c = Color(rgb)
    return (0.299 * c.red + 0.587 * c.green + 0.114 * c.blue).roundToInt()
  }

  private fun beschreibe(name: String, rgb: Int): String {
    val c = Color(rgb)
    return String.format("%s rgb(%d,%d,%d) #%02X%02X%02X Helligkeit %d",
      name, c.red, c.green, c.blue, c.red, c.green, c.blue, helligkeit(rgb))
  }

  /** Every colour that occurs inside the mark's rectangle. */
  private fun farbenImBalken(): Set<Int> {
    val result = mutableSetOf<Int>()
    for (y in oben until oben + HIDDEN_GAP_HEIGHT) {
      for (x in links until links + breite) {
        result.add(balken.getRGB(x, y))
      }
    }
    return result
  }

  /**
   * IS IT THERE AT ALL. Something inside the rectangle has to be far enough from the ground it is
   * drawn on to be seen, measured in brightness rather than in „the triples differ".
   *
   * RED before the painter was registered:
   *   org.opentest4j.AssertionFailedError: nothing in the mark is 40 brightness steps away from
   *   the ground: [Grund rgb(255,255,255) #FFFFFF Helligkeit 255] ==> expected: <true> but was: <false>
   */
  @Test
  fun `the mark stands out from the chart background`() {
    val grund = helligkeit(grundFarbe.rgb)
    val weiteste = farbenImBalken().maxByOrNull { abs(helligkeit(it) - grund) }!!
    assertTrue(abs(helligkeit(weiteste) - grund) >= 40,
      "nothing in the mark is 40 brightness steps away from the ground: " +
        "${beschreibe("Grund", grundFarbe.rgb)} / ${beschreibe("dunkelste Stelle", weiteste)}")
    // AND IT IS THE COLOUR THAT WAS MEASURED, not merely some dark colour. Without this line the
    // check passes on the DEFAULT painter of `StyledPainterImpl`, which draws an unknown style as a
    // one-pixel BLACK outline -- black is 255 steps from white and satisfies everything above while
    // being none of the fork's colours. Found by running this file against the unregistered
    // painter; see the report of 08.09.2026.
    assertEquals(UIConfiguration(Color.BLUE, false).hiddenGapColor.rgb, weiteste,
      "the mark is drawn in some dark colour, but not in the one that was measured: " +
        beschreibe("gemalt", weiteste))
  }

  /**
   * The mark must stay readable against the row separator line it lies on: their brightness
   * differs by at least 40 steps (0.299 R + 0.587 G + 0.114 B).
   */
  @Test
  fun `the mark is told apart from the row line`() {
    val kollision = UIConfiguration(Color.BLUE, false).hiddenGapColor
    val abstand = abs(helligkeit(kollision.rgb) - helligkeit(Color.GRAY.rgb))
    assertTrue(abstand >= 40, "${beschreibe("Kollisionsbalken", kollision.rgb)} -- Abstand zur Trennlinie ist $abstand")
  }

  /**
   * THE SECOND CUE, and it is measured the way the other chart tests measure hatching: a
   * cut through the mark must meet more than one colour. Here the cut is HORIZONTAL, along the
   * middle of the mark, and what it meets are the dashes and the ground between them — that is the
   * „it is not really there" of a dashed line, and it is there without any colour.
   *
   * RED before the painter was registered:
   *   org.opentest4j.AssertionFailedError: a horizontal cut through the mark meets one colour only
   *   ==> expected: <true> but was: <false>
   */
  @Test
  fun `a horizontal cut through the mark meets more than one colour`() {
    val mitte = oben + HIDDEN_GAP_HEIGHT / 2
    val zeile = (links until links + breite).map { balken.getRGB(it, mitte) }

    assertTrue(zeile.toSet().size > 1,
      "a horizontal cut through the mark meets one colour only: " +
        zeile.toSet().joinToString { beschreibe("", it) })
    // AND IT IS DASHED, not a solid line with two painted ends. Counting the RUNS of ground colour
    // along the cut rather than the number of colours: two would be the outline of the default
    // painter, which is what this check silently accepted before. A dashed line over 100 pixels
    // leaves several.
    var luecken = 0
    var vorherGrund = false
    zeile.forEach { farbe ->
      val istGrund = farbe == grundFarbe.rgb
      if (istGrund && !vorherGrund) luecken++
      vorherGrund = istGrund
    }
    assertTrue(luecken >= 3, "the line is not dashed -- $luecken gaps along the cut")
  }

  /**
   * AND IT IS A LINE, NOT A BLOCK. The distinguishing form against both other signs: they fill
   * their rectangle, this one leaves most of it alone and marks its two ENDS. So the two end
   * columns of the rectangle are painted through their whole height and a column in the middle is
   * not.
   *
   * RED before the painter was registered:
   *   org.opentest4j.AssertionFailedError: the ends of the mark are not drawn solid ==>
   *   expected: <5> but was: <0>
   */
  @Test
  fun `the mark has two solid ends and a light middle`() {
    fun gemalt(x: Int) = (oben until oben + HIDDEN_GAP_HEIGHT).count { balken.getRGB(x, it) != grundFarbe.rgb }

    assertEquals(HIDDEN_GAP_HEIGHT, gemalt(links), "the ends of the mark are not drawn solid")
    assertEquals(HIDDEN_GAP_HEIGHT, gemalt(links + breite - 1), "the right end is not drawn solid")
    val innen = (links + 5 until links + breite - 5).map { gemalt(it) }
    assertTrue(innen.any { it < HIDDEN_GAP_HEIGHT },
      "the middle of the mark is as solid as its ends -- then it is a block and not a line")
  }
}
