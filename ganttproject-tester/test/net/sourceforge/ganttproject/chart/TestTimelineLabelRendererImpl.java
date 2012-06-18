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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import junit.framework.TestCase;

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
      return null;
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

    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), SUNDAY.getTime(), MONDAY.getTime(), 50,
        DayType.WEEKEND));
    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), MONDAY.getTime(), TUESDAY.getTime(), 100,
        DayType.WORKING));
    testApi.myOffsets.add(new Offset(GPTimeUnitStack.DAY, SUNDAY.getTime(), TUESDAY.getTime(), WEDNESDAY.getTime(),
        150, DayType.WORKING));

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
}
