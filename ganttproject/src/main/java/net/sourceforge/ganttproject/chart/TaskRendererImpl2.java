/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder;
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.chart.gantt.*;
import net.sourceforge.ganttproject.task.*;

import java.util.*;
import java.util.List;

import static net.sourceforge.ganttproject.chart.gantt.TaskActivitySceneApiAdapterKt.mapTaskSceneTask2Task;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class TaskRendererImpl2 extends ChartRendererBase {
  private GanttChartSceneBuilder chartRenderer;

  private ChartModelImpl myModel;

  private GPOptionGroup myLabelOptions;

  class GanttChartSceneApi implements GanttChartSceneBuilder.InputApi {
    @Override
    public int getHeaderHeight() {
      return myModel.getChartUIConfiguration().getHeaderHeight();
    }

    @Override
    public int getWidth() {
      return (int) getChartModel().getBounds().getWidth();
    }

    @Override
    public int getLabelsFontSize() {
      return getChartModel().getChartUIConfiguration().getBaseFontSize();
    }

    @Override
    public int getAppFontSize() {
      return myModel.getProjectConfig().getAppFontSize().get();
    }

    @Override
    public int getVerticalOffset() {
      return myModel.getVerticalOffset();
    }

    @Override
    public OffsetList getTasksUnitOffsets() {
      return getChartModel().getDefaultUnitOffsets();
    }

    @Override
    public TimeUnit getProgressBarTimeUnit() {
      return getChartModel().getTimeUnitStack().getDefaultTimeUnit();
    }

    @Override
    public net.sourceforge.ganttproject.chart.gantt.VerticalPartitioning getVerticalPartitioning() {
      TaskContainmentHierarchyFacade containment = myModel.getTaskManager().getTaskHierarchy();
      Map<ITaskSceneTask, Task> tasksMap = mapTaskSceneTask2Task(containment.getTasksInDocumentOrder(), myModel);
      return new net.sourceforge.ganttproject.chart.gantt.VerticalPartitioning(
        getVisibleTaskSceneTasks(),
        (ITaskSceneTask t1, ITaskSceneTask t2) -> containment.areUnrelated(tasksMap.get(t1), tasksMap.get(t2))
      );
    }

    @Override
    public List<ITask> getVisibleTasks() {
      return ImmutableList.copyOf(
        DependencySceneApiAdapterKt.tasks2itasks(myModel.getVisibleTasks()).values()
      );
    }

    @Override
    public List<ITaskSceneTask> getVisibleTaskSceneTasks() {
      return ImmutableList.copyOf(
        mapTaskSceneTask2Task(TaskRendererImpl2.this.getVisibleTasks(), myModel).keySet()
      );
    }

    @Override
    public List<ITaskSceneTask> getTasksInDocumentOrder() {
      TaskContainmentHierarchyFacade containment = myModel.getTaskManager().getTaskHierarchy();;
      return ImmutableList.copyOf(
        mapTaskSceneTask2Task(containment.getTasksInDocumentOrder(), myModel).keySet()
      );
    }

    @Override
    public List<GanttPreviousStateTask> getBaseline() {
      return myModel.getBaseline();
    }

    @Override
    public TaskActivitySceneBuilder.ChartApi getChartApi(TaskLabelSceneBuilder<ITaskSceneTask> labelsRenderer) {
      return new TaskActivitySceneChartApi(myModel) {
        @Override
        public int getRowHeight() {
          int rowHeight = labelsRenderer.calculateRowHeight();
          if (myModel.getBaseline() != null) {
            rowHeight = rowHeight + 8;
          }
          int appFontSize = myModel.getProjectConfig().getAppFontSize().get();
          return Math.max(appFontSize, rowHeight);
        }
        @Override
        public int getBarHeight() {
          return labelsRenderer.getFontHeight();
        }
      };
    }

    @Override
    public GPCalendarCalc getCalendar() {
      return TaskRendererImpl2.this.getCalendar();
    }

    @Override
    public Date getStartDate() {
      return myModel.getStartDate();
    }

    @Override
    public TimeDuration createLength(TimeUnit timeUnit, Date startDate, Date endDate) {
      return myModel.getTaskManager().createLength(timeUnit, startDate, endDate);
    }

    @Override
    public TimeDuration createLength(int duration) {
      return getChartModel().getTaskManager().createLength(duration);
    }
  }

  public TaskRendererImpl2(ChartModelImpl model) {
    super(model);
    myModel = model;
    chartRenderer = new GanttChartSceneBuilder(new GanttChartSceneApi(), getPrimitiveContainer());
    TaskLabelSceneBuilder.InputApi taskLabelSceneApi = chartRenderer.getTaskLabelSceneApi();
    myLabelOptions = new ChartOptionGroup("ganttChartDetails",
        new GPOption[] {
          taskLabelSceneApi.getTopLabelOption(), taskLabelSceneApi.getBottomLabelOption(),
          taskLabelSceneApi.getLeftLabelOption(), taskLabelSceneApi.getRightLabelOption()
        },
        model.getOptionEventDispatcher()
    );
  }

  private List<Task> getVisibleTasks() {
    return ((ChartModelImpl) getChartModel()).getVisibleTasks();
  }

  /**
    This class splits all tasks into 4 groups. One group is pure virtual: it contains
    tasks which are hidden under some collapsed parent and hence are just filtered out.
    The remaining groups are: tasks which are shown in the chart viewport, tasks above the viewport
    and tasks below the viewport. We need tasks outside the viewport because we want to show
    dependency lines which may connect them with tasks inside the viewport.
   */
  static class VerticalPartitioning {
    final List<Task> aboveViewport = Lists.newArrayList();
    final List<Task> belowViewport = Lists.newArrayList();
    final List<Task> insideViewport;

    /**
     * @param tasksInsideViewport partition with tasks inside viewport, with hidden tasks already filtered.
     *        Tasks must be ordered in their document order.
     */
    VerticalPartitioning(List<Task> tasksInsideViewport) {
      insideViewport = tasksInsideViewport;
    }

    /**
     * Builds the remaining partitions.
     *
     * In this method we iterate through *all* the tasks in their document order. If we find some
     * collapsed task then we filter out its children. Until we reach the first task in the vieport
     * partition,  we're above the viewport, then we skip the viewport partition and proceed to
     * below viewport
     */
    void build(TaskContainmentHierarchyFacade containment) {
      List<Task> tasksInDocumentOrder = containment.getTasksInDocumentOrder();
      final Task firstVisible = insideViewport.isEmpty() ? null : insideViewport.get(0);
      final Task lastVisible = insideViewport.isEmpty() ? null : insideViewport.get(insideViewport.size() - 1);
      List<Task> addTo = aboveViewport;

      Task collapsedRoot = null;
      for (Task nextTask : tasksInDocumentOrder) {
        if (addTo == null) {
          if (nextTask.equals(lastVisible)) {
            addTo = belowViewport;
          }
          continue;
        }
        if (nextTask.equals(firstVisible)) {
          addTo = null;
          continue;
        }

        if (collapsedRoot != null) {
          if (containment.areUnrelated(nextTask, collapsedRoot)) {
            collapsedRoot = null;
          } else {
            continue;
          }
        }
        addTo.add(nextTask);

        if (!nextTask.getExpand()) {
          assert collapsedRoot == null : "All tasks processed prior to this one must be expanded";
          collapsedRoot = nextTask;
        }
      }
    }
  }

  @Override
  public void render() {
    chartRenderer.render();
  }

  /**
   * This method scans the list of activities and splits activities crossing the borders
   * of the given frame into parts "before" and "after" the border date. Activities which
   * do not cross frame borders are left as is, and the relative order of activities is preserved.
   *
   * Normally no more than two activities from the input list are partitioned.
   *
   * @return input activities with those crossing frame borders partitioned into left and right parts
   */
  static List<TaskActivity> splitOnBounds(List<TaskActivity> activities, Date frameStartDate, Date frameEndDate) {
    Preconditions.checkArgument(frameEndDate.compareTo(frameStartDate) >= 0,
        String.format("Invalid frame: start=%s end=%s", frameStartDate, frameEndDate));
    List<TaskActivity> result = Lists.newArrayList();
    Deque<TaskActivity> queue = new LinkedList<>(activities);
    while (!queue.isEmpty()) {
      TaskActivity head = queue.pollFirst();
      if (head.getStart().compareTo(frameStartDate) < 0
          && head.getEnd().compareTo(frameStartDate) > 0) {

        // Okay, this activity crosses frame start. Lets add its left part to the result
        // and push back its right part
        TaskActivity beforeViewport = new TaskActivityPart(head, head.getStart(), frameStartDate);
        TaskActivity remaining = new TaskActivityPart(head, frameStartDate, head.getEnd());
        result.add(beforeViewport);
        queue.addFirst(remaining);
        continue;
      }
      if (head.getStart().compareTo(frameEndDate) < 0
          && head.getEnd().compareTo(frameEndDate) > 0) {
        // This activity crosses frame end date. Again, lets add its left part to the result
        // and push back the remainder.
        TaskActivity insideViewport = new TaskActivityPart(head, head.getStart(), frameEndDate);
        TaskActivity remaining = new TaskActivityPart(head, frameEndDate, head.getEnd());
        result.add(insideViewport);
        queue.addFirst(remaining);
        continue;
      }
      result.add(head);
    }
    return result;
  }

  public GPOptionGroup getLabelOptions() {
    return myLabelOptions;
  }

  public int calculateRowHeight() {
    return chartRenderer.calculateRowHeight();
  }

  Canvas getLabelLayer() {
    return chartRenderer.getLabelLayer();
  }

  public static List<Rectangle> getTaskRectangles(Task t, ChartModelImpl chartModel) {
    List<Rectangle> result = new ArrayList<Rectangle>();
    List<TaskActivity> originalActivities = t.getActivities();
    List<TaskActivity> splitOnBounds = TaskRendererImpl2.splitOnBounds(originalActivities, chartModel.getStartDate(), chartModel.getEndDate());
    for (TaskActivity activity : splitOnBounds) {
      assert activity != null : "Got null activity in task="+t;
      Canvas.Shape graphicPrimitive = chartModel.getGraphicPrimitive(activity);
      assert graphicPrimitive != null : "Got null for activity="+activity;
      assert graphicPrimitive instanceof Rectangle;
      result.add((Rectangle) graphicPrimitive);
    }
    return result;

  }
}
