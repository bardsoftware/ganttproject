/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.grid.Offset
import biz.ganttproject.core.chart.grid.OffsetBuilderImpl
import biz.ganttproject.core.chart.grid.OffsetList
import biz.ganttproject.core.chart.text.DayTextFormatter
import biz.ganttproject.core.chart.text.TimeFormatter
import biz.ganttproject.core.chart.text.TimeUnitText
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.TimeUnit
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import junit.framework.TestCase
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.chart.TestPainter
import net.sourceforge.ganttproject.chart.TestTextLengthCalculator
import java.text.DateFormat
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class TestBottomUnitSceneBuilder: TestCase() {
  init {
    object: CalendarFactory() {
      init {
        setLocaleApi(object: CalendarFactory.LocaleApi {
          override fun getLocale(): Locale {
            return Locale.US
          }
          override fun getShortDateFormat(): DateFormat {
            return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
          }
        })
      }
    };
  }

  // Tests that label corresponding to weekend days are rendered with empty labels
  fun testWeekendLabelsAreEmpty() {
    val calendar = WeekendCalendarImpl()
    val start = TestSetupHelper.newMonday().time

    // Build day offsets
    val builder = OffsetBuilderImpl.FactoryImpl()
        .withStartDate(start).withViewportStartDate(start)
        .withCalendar(calendar).withTopUnit(GPTimeUnitStack.WEEK).withBottomUnit(GPTimeUnitStack.DAY)
        .withAtomicUnitWidth(20).withEndOffset(210).withWeekendDecreaseFactor(10f)
        .build()
    val bottomUnitOffsets = OffsetList()
    builder.constructOffsets(ArrayList(), bottomUnitOffsets)

    // Fill canvas with simple bottom line
    val canvas = Canvas()
    val dumbFormatter = DayTextFormatter()
    val bottomUnitSceneBuilder = BottomUnitSceneBuilder(canvas, object: BottomUnitSceneBuilder.InputApi {
      override fun getTopLineHeight(): Int {
        return 10
      }

      override fun getBottomUnitOffsets(): OffsetList {
        return bottomUnitOffsets
      }

      override fun getFormatter(offsetUnit: TimeUnit?, lowerLine: TimeUnitText.Position?): TimeFormatter {
        return dumbFormatter
      }
    })
    bottomUnitSceneBuilder.build()

    // Get text groups from canvas. The only legal way of doing that is "painting"
    val textLengthCalculator = TestTextLengthCalculator(10)
    val textGroups = mutableListOf<Canvas.TextGroup>()
    canvas.paint(object : TestPainter(textLengthCalculator) {
      override fun paint(textGroup: Canvas.TextGroup?) {
        textGroups.add(textGroup!!)
      }
    })
    assertEquals(1, textGroups.size)

    // Now iterate through all texts and check that those which were built for weekend offsets
    // are empty.
    textGroups[0].getLine(0).forEachIndexed({index, text ->
      val offset = findOffset(bottomUnitOffsets, text.leftX)
      assertNotNull(offset)
      val label = text.getLabels(textLengthCalculator)[0].text
      if (offset!!.dayMask.and(GPCalendar.DayMask.WEEKEND) == 0) {
        assertFalse(label.isEmpty())
      } else {
        assertTrue(label.isEmpty())
      }
    })
  }

  private fun findOffset(offsets: OffsetList, leftX: Int): Offset? {
    var result: Offset? = null
    for (offset in offsets) {
      if (offset.startPixels < leftX && offset.startPixels > result?.startPixels ?: -1) {
        result = offset
      }
    }
    return result
  }
}
