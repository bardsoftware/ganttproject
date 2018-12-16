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

import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Tests for TimelineLabelRendererImpl
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestTimelineLabelRendererImpl extends TestCase {
  private class TestChartModelApi implements TimelineLabelRendererImpl.ChartModelApi {
    private final TaskManager myTaskManager = TestSetupHelper.newTaskManagerBuilder().build();
    final List<Offset> myOffsets = Lists.newArrayList();

    @Override
    public int getTimelineTopLineHeight() {
      return 20;
    }

    @Override
    public List<Offset> getDefaultUnitOffsets() {
      return myOffsets;
    }

    @Override
    public Date getStartDate() {
      return myTaskManager.getProjectStart();
    }

    @Override
    public Collection<Task> getTimelineTasks() {
      List<Task> result = Lists.newArrayList();
      for (Task t : myTaskManager.getTasks()) {
        if (t.isMilestone()) {
          result.add(t);
        }
      }
      return result;
    }

    TaskManager getTaskManager() {
      return myTaskManager;
    }
  }

  private static final GanttCalendar SUNDAY = TestSetupHelper.newSunday();
  private static final GanttCalendar MONDAY = TestSetupHelper.newMonday();
  private static final GanttCalendar TUESDAY = TestSetupHelper.newTuesday();
  private static final GanttCalendar WEDNESDAY = TestSetupHelper.newWendesday();

  private TestChartModelApi testApi;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testApi = new TestChartModelApi();
  }

  private void testHasTimelineLabel(Task task, boolean condition) {
    testApi.getTaskManager().getTaskHierarchy().move(task, testApi.getTaskManager().getRootTask());

    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), SUNDAY.getTime(), MONDAY.getTime(), 0, 50,
        DayMask.WEEKEND));
    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), MONDAY.getTime(), TUESDAY.getTime(), 50, 100,
        DayMask.WORKING));
    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), TUESDAY.getTime(), WEDNESDAY.getTime(),
        100, 150, DayMask.WORKING));

    TimelineLabelRendererImpl renderer = new TimelineLabelRendererImpl(testApi);
    renderer.render();
    renderer.getLabelLayer().paint(new TestPainter(new TestTextLengthCalculator(10)));
    if (condition) {
      assertTrue(renderer.getLabelLayer().getPrimitive(60, 15) instanceof Text);
    } else {
      assertNull(renderer.getLabelLayer().getPrimitive(60, 15));
    }
  }

  public void testMilestoneLabels() {
    Task task = testApi.getTaskManager().createTask();
    task.setName("foo");
    task.setMilestone(true);
    task.setStart(MONDAY);
    task.setEnd(TUESDAY);
    testHasTimelineLabel(task, true);
  }

  public void testNoLabelsForMereMortals() {
    TestChartModelApi testApi = new TestChartModelApi();
    Task task = testApi.getTaskManager().createTask();
    task.setName("foo");
    task.setMilestone(false);
    task.setStart(MONDAY);
    task.setEnd(TUESDAY);
    testHasTimelineLabel(task, false);
  }

  public void testShortLabelsAreDisplayedFully() {
    Task task = testApi.getTaskManager().createTask();
    task.setName("foo");
    TimelineLabelRendererImpl renderer = new TimelineLabelRendererImpl(testApi);
    TimelineLabelRendererImpl.LabelTextSelector textSelector = new TimelineLabelRendererImpl.LabelTextSelector(task, renderer.getLabelLayer().createText(0, 0, ""));
    TestTextLengthCalculator lengthCalculator = new TestTextLengthCalculator(10);
    Canvas.Label[] labels = textSelector.getLabels(lengthCalculator);
    assertEquals(1, labels.length);
    assertEquals("foo", labels[0].text);
  }

  public void testLongLabelsAreDisplayedTruncated() {
    Task task = testApi.getTaskManager().createTask();
    task.setName("123456789012345678901");
    TimelineLabelRendererImpl renderer = new TimelineLabelRendererImpl(testApi);
    TimelineLabelRendererImpl.LabelTextSelector textSelector = new TimelineLabelRendererImpl.LabelTextSelector(task, renderer.getLabelLayer().createText(0, 0, ""));
    TestTextLengthCalculator lengthCalculator = new TestTextLengthCalculator(10);
    Canvas.Label[] labels = textSelector.getLabels(lengthCalculator);
    assertEquals(1, labels.length);
    assertEquals("123456789012345678...", labels[0].text);
  }
}
