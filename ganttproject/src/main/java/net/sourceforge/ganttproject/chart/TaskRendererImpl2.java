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
import com.google.common.collect.ImmutableList;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.chart.gantt.*;
import net.sourceforge.ganttproject.task.*;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.sourceforge.ganttproject.chart.gantt.TaskActivitySceneApiAdapterKt.mapTaskSceneTask2Task;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class TaskRendererImpl2 extends ChartRendererBase {
  private final GanttChartSceneBuilder chartRenderer;

  private final ChartModelImpl myModel;

  private final GPOptionGroup myLabelOptions;

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
      TaskContainmentHierarchyFacade containment = myModel.getTaskManager().getTaskHierarchy();
      Map<Task, ITask> tasks2itasks = DependencySceneApiAdapterKt.tasks2itasks(containment.getTasksInDocumentOrder());
      return myModel.getVisibleTasks().stream().map(tasks2itasks::get).collect(Collectors.toList());
    }

    @Override
    public List<ITaskSceneTask> getVisibleTaskSceneTasks() {
      return ImmutableList.copyOf(
        mapTaskSceneTask2Task(TaskRendererImpl2.this.getVisibleTasks(), myModel).keySet()
      );
    }

    @Override
    public List<ITaskSceneTask> getTasksInDocumentOrder() {
      TaskContainmentHierarchyFacade containment = myModel.getTaskManager().getTaskHierarchy();
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

  @Override
  public void render() {
    chartRenderer.render();
  }

  public GPOptionGroup getLabelOptions() {
    return myLabelOptions;
  }

  public int calculateRowHeight() {
    return chartRenderer.getRowHeight();
  }

  Canvas getLabelLayer() {
    return chartRenderer.getLabelLayer();
  }

  public static List<Rectangle> getTaskRectangles(Task t, ChartModelImpl chartModel) {
    List<Rectangle> result = new ArrayList<Rectangle>();
    ITaskSceneTask task = new ITaskSceneTaskImpl(t, chartModel);
    List<ITaskActivity<ITaskSceneTask>> originalActivities = task.getActivities();
    TaskActivitySplitter<ITaskSceneTask> splitter = new TaskActivitySplitter<ITaskSceneTask>(
      chartModel::getStartDate,
      chartModel::getEndDate,
      (u, s, e) -> chartModel.getTaskManager().createLength(u, s, e)
    );
    List<ITaskActivity<ITaskSceneTask>> splitOnBounds = splitter.split(originalActivities);
    for (ITaskActivity<ITaskSceneTask> activity : splitOnBounds) {
      assert activity != null : "Got null activity in task="+t;
      Canvas.Shape graphicPrimitive = chartModel.getGraphicPrimitive(activity);
      assert graphicPrimitive != null : "Got null for activity="+activity;
      assert graphicPrimitive instanceof Rectangle;
      result.add((Rectangle) graphicPrimitive);
    }
    return result;

  }
}
