/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.GanttPreviousStateTask
import net.sourceforge.ganttproject.chart.ChartComparison
import net.sourceforge.ganttproject.chart.ComparisonResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/** The bands which each comparison view draws underneath a task bar, counted in the built scene. */
class ComparisonBandSceneTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  @Test
  fun `the effort view draws a band without a baseline`() {
    val bands = bands(ChartComparison.EFFORT, baseline = null, effort = ComparisonResult.MORE)
    assertEquals(1, bands.size)
    assertTrue(bands[0].hasStyle("later"), "more hours spent than first estimated")
    assertEquals(barLeftX(SEP_3), bands[0].leftX(), "the effort band lies under the bar")
  }

  @Test
  fun `the effort view draws nothing for a task without original effort`() {
    assertEquals(0, bands(ChartComparison.EFFORT, baseline = null, effort = ComparisonResult.NO_BAND).size)
  }

  @Test
  fun `an original effort without actual effort draws a neutral band`() {
    val bands = bands(ChartComparison.EFFORT, baseline = null, effort = ComparisonResult.NEUTRAL)
    assertEquals(1, bands.size)
    assertFalse(bands[0].hasStyle("later") || bands[0].hasStyle("earlier"))
  }

  @Test
  fun `the dates view ignores the effort`() {
    assertEquals(0, bands(ChartComparison.DATES, baseline = null, effort = ComparisonResult.MORE).size)
  }

  @Test
  fun `the durations view shows nothing for a task which only moved`() {
    val baseline = baseline(start = AUG_27, duration = 5)
    assertEquals(1, bands(ChartComparison.DATES, baseline).size, "the dates view sees the shift")
    assertEquals(0, bands(ChartComparison.DURATIONS, baseline).size, "five days are still five days")
  }

  @Test
  fun `the durations band starts with the bar and shows a longer task`() {
    val bands = bands(ChartComparison.DURATIONS, baseline(start = AUG_27, duration = 3))
    assertEquals(1, bands.size)
    assertTrue(bands[0].hasStyle("later"))
    assertEquals(barLeftX(SEP_3), bands[0].leftX(), "anchored at the task start, not at the baseline start")
  }

  @Test
  fun `without a baseline the durations view draws a neutral band`() {
    val bands = bands(ChartComparison.DURATIONS, baseline = null)
    assertEquals(1, bands.size)
    assertFalse(bands[0].hasStyle("later") || bands[0].hasStyle("earlier"))
  }

  @Test
  fun `the label below the bar goes under the band in the comparison views`() {
    for (comparison in listOf(ChartComparison.EFFORT, ChartComparison.DURATIONS, ChartComparison.DATES_AND_DURATIONS)) {
      val scene = labelScene(comparison, baseline = null)
      val bandBottom = scene.bands.maxOf { (it as Canvas.Polygon).bottomY }
      assertTrue(scene.downLabelTopY > bandBottom, "$comparison: the label starts below the band")
      assertEquals(scene.dateViewRowHeight + BAR_HEIGHT, scene.rowHeight, "$comparison: the row makes room for the band")
    }
  }

  @Test
  fun `with a baseline the dates view puts the label below the bar under the band`() {
    val scene = labelScene(ChartComparison.DATES, baseline(start = AUG_27, duration = 5))
    val band = scene.bands.single() as Canvas.Polygon
    assertTrue(scene.downLabelTopY > band.bottomY, "the label starts below the band")
    assertEquals(labelScene(ChartComparison.DATES, baseline = null).rowHeight + BAR_HEIGHT, scene.rowHeight,
      "the row makes room for the band")
  }

  @Test
  fun `without a baseline the dates view keeps the label below the bar where it was`() {
    val scene = labelScene(ChartComparison.DATES, baseline = null)
    assertEquals(0, scene.bands.size)
    // Measured on 908451c85, before the label moved under the band with a baseline
    assertEquals(11, scene.downLabelTopY)
    assertEquals(29, scene.rowHeight)
  }

  @Test
  fun `without a baseline the dates and durations view draws only a neutral lower half`() {
    val bands = bands(ChartComparison.DATES_AND_DURATIONS, baseline = null)
    assertEquals(1, bands.size, "no hatched dates half without dates to compare")
    assertTrue(bands[0].hasStyle(GanttChartSceneBuilder.BAND_LOWER))
    assertFalse(bands[0].hasStyle("later") || bands[0].hasStyle("earlier"))
    assertEquals(0, bands(ChartComparison.DATES_AND_DURATIONS, baseline = null, summary = true).size,
      "a summary task keeps only the dates comparison, and there is none")
  }

  @Test
  fun `the dates and durations view draws a moved and longer task in both halves`() {
    val bands = bands(ChartComparison.DATES_AND_DURATIONS, baseline(start = AUG_27, duration = 3))
    assertEquals(2, bands.size)
    val upper = bands.single { it.hasStyle(GanttChartSceneBuilder.BAND_UPPER) }
    val lower = bands.single { it.hasStyle(GanttChartSceneBuilder.BAND_LOWER) }
    assertEquals(barLeftX(AUG_27), upper.leftX(), "the dates half is anchored at the baseline start")
    assertEquals(barLeftX(SEP_3), lower.leftX(), "the durations half is anchored at the task start")
    assertTrue(upper.hasStyle("later") && lower.hasStyle("later"))
  }

  @Test
  fun `the dates and durations view draws a task which only moved in the upper half`() {
    val bands = bands(ChartComparison.DATES_AND_DURATIONS, baseline(start = AUG_27, duration = 5))
    assertEquals(1, bands.size)
    assertTrue(bands[0].hasStyle(GanttChartSceneBuilder.BAND_UPPER))
  }

  @Test
  fun `a summary task keeps only the dates comparison at full height`() {
    val bands = bands(ChartComparison.DATES_AND_DURATIONS, baseline(start = AUG_27, duration = 3), summary = true)
    assertEquals(1, bands.size)
    assertFalse(bands[0].hasStyle(GanttChartSceneBuilder.BAND_UPPER) || bands[0].hasStyle(GanttChartSceneBuilder.BAND_LOWER))
  }

  @Test
  fun `a milestone gets a band with the milestone style`() {
    val bands = bands(ChartComparison.EFFORT, baseline = null, effort = ComparisonResult.LESS, milestone = true)
    assertEquals(1, bands.size)
    assertTrue(bands[0].hasStyle("milestone") && bands[0].hasStyle("earlier"))
  }

  @Test
  fun `a one day task gets a band in every view`() {
    val baseline = baseline(start = AUG_27, duration = 2)
    for (comparison in ChartComparison.entries) {
      val bands = bands(comparison, baseline, effort = ComparisonResult.MORE, taskDuration = 1)
      assertTrue(bands.isNotEmpty(), "$comparison")
    }
  }
}

private val AUG_27 = day(2026, Calendar.AUGUST, 27)
private val SEP_3 = day(2026, Calendar.SEPTEMBER, 3)
private val CHART_START = day(2026, Calendar.AUGUST, 1)

private fun day(year: Int, month: Int, dayOfMonth: Int): Date = GregorianCalendar(year, month, dayOfMonth).time

private const val ROW = 1

/** The x of a bar starting on [date]: the stub chart has one offset of 20 pixels per day. */
private fun barLeftX(date: Date): Int = ((date.time - CHART_START.time) / (24 * 3600 * 1000L)).toInt() * 20

private fun Canvas.Shape.leftX(): Int = (this as Canvas.Polygon).leftX

private fun baseline(start: Date, duration: Int) =
  listOf(GanttPreviousStateTask(ROW, CalendarFactory.createGanttCalendar(start), duration, false, false))

/** The task always starts on 3 September and lasts five days unless told otherwise. */
private fun bands(
  comparison: ChartComparison, baseline: List<GanttPreviousStateTask>?,
  effort: ComparisonResult = ComparisonResult.NO_BAND,
  summary: Boolean = false, milestone: Boolean = false, taskDuration: Int = 5
): List<Canvas.Shape> {
  val calendar = AlwaysWorkingTimeCalendarImpl()
  val length = days(if (milestone) 1 else taskDuration)
  val task = BandTestTask(ROW, SEP_3, calendar.shiftDate(SEP_3, length), length, summary, milestone)
  val canvas = Canvas()
  GanttChartSceneBuilder(InputApiStub(calendar, task, baseline, comparison, effort), canvas).render()
  return collectBaselineBars(canvas)
}

private const val BAR_HEIGHT = 10

private class LabelScene(
  val bands: List<Canvas.Shape>, val downLabelTopY: Int, val rowHeight: Int, val dateViewRowHeight: Int
)

/** The scene of a task with a label below the bar, an effort above the original one and the given view. */
private fun labelScene(comparison: ChartComparison, baseline: List<GanttPreviousStateTask>?): LabelScene {
  val calendar = AlwaysWorkingTimeCalendarImpl()
  val length = days(5)
  val task = BandTestTask(ROW, SEP_3, calendar.shiftDate(SEP_3, length), length, false, false, label = "below")
  val canvas = Canvas()
  val builder = GanttChartSceneBuilder(InputApiStub(calendar, task, baseline, comparison, ComparisonResult.MORE), canvas)
  builder.getTaskLabelSceneApi().bottomLabelOption.setValue("name")
  builder.render()
  val downLabels = mutableListOf<Canvas.Text>()
  val collector = object : biz.ganttproject.core.chart.canvas.Painter {
    override fun prePaint() = Unit
    override fun paint(rectangle: Canvas.Rectangle) = Unit
    override fun paint(line: Canvas.Line) = Unit
    override fun paint(next: Canvas.Text) {
      // Only the label below the bar hangs from its top.
      if (next.vAlignment == Canvas.VAlignment.TOP) downLabels.add(next)
    }
    override fun paint(textGroup: Canvas.TextGroup) = Unit
    override fun paint(rhombus: Canvas.Rhombus) = Unit
  }
  canvas.paint(collector)
  canvas.layers.forEach { it.paint(collector) }
  val datesBuilder = GanttChartSceneBuilder(InputApiStub(calendar, task, baseline, ChartComparison.DATES), Canvas())
  datesBuilder.getTaskLabelSceneApi().bottomLabelOption.setValue("name")
  return LabelScene(
    collectBaselineBars(canvas), downLabels.single().bottomY,
    builder.myLabelsRenderer.calculateRowHeight(), datesBuilder.myLabelsRenderer.calculateRowHeight()
  )
}

private class BandTestTask(
  private val rowId: Int, startDate: Date, endDate: Date, private val length: TimeDuration,
  private val summary: Boolean, private val milestone: Boolean, private val label: String? = null
) : TestSceneTask() {
  override fun getProperty(propertyID: String?): String? = label
  private val ownActivities: List<TaskSceneTaskActivity> =
    listOf(TaskActivityDataImpl(true, true, 1f, this, startDate, endDate, length))
  override fun getRowId(): Int = rowId
  override val color: Color get() = Color.BLUE
  override val end: GanttCalendar = CalendarFactory.createGanttCalendar(endDate)
  override val activities: List<TaskSceneTaskActivity> get() = ownActivities
  override val expand: Boolean get() = true
  override val duration: TimeDuration get() = length
  override val hasNestedTasks: Boolean get() = summary
  override fun isMilestone(): Boolean = milestone
}
