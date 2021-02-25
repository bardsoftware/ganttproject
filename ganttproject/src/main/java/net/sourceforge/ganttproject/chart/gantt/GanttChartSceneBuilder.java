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
package net.sourceforge.ganttproject.chart.gantt;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.scene.gantt.DependencySceneBuilder;
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder;
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.task.*;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class GanttChartSceneBuilder {
  public interface InputApi {
    int getHeaderHeight();
    int getWidth();
    int getLabelsFontSize();
    int getVerticalOffset();
    OffsetList getTasksUnitOffsets();
    TimeUnit getProgressBarTimeUnit();
    VerticalPartitioning getVerticalPartitioning();
    List<ITask> getVisibleTasks();
    List<ITaskSceneTask> getVisibleTaskSceneTasks();
    List<ITaskSceneTask> getTasksInDocumentOrder();
    List<GanttPreviousStateTask> getBaseline();
    TaskActivitySceneBuilder.ChartApi getChartApi(TaskLabelSceneBuilder<ITaskSceneTask> labelsRenderer);
    GPCalendarCalc getCalendar();
    Date getStartDate();
    TimeDuration createLength(TimeUnit timeUnit, Date startDate, Date endDate);
    TimeDuration createLength(int duration);
  }

  private final Canvas canvas;
  private final InputApi input;
  private final TaskLabelSceneBuilder.InputApi taskLabelSceneApi;

  private final TaskLabelSceneBuilder<ITaskSceneTask> myLabelsRenderer;

  private final TaskActivitySceneBuilder.TaskApi<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myTaskApi = new TaskActivitySceneTaskApi();

  private final TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myTaskActivityRenderer;
  private final TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> myBaselineActivityRenderer;

  private final Canvas myLabelsLayer;
  private final TaskActivitySceneBuilder.ChartApi myChartApi;
  private final TaskActivitySplitter mySplitter;

  public GanttChartSceneBuilder(InputApi input) {
    this(input, new Canvas());
  }

  public GanttChartSceneBuilder(InputApi input, Canvas canvas) {
    this.input = input;
    this.canvas = canvas;

    getPrimitiveContainer().setOffset(0, input.getHeaderHeight());
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    myLabelsLayer = getPrimitiveContainer().newLayer();

    List<String> taskProperties = Lists.newArrayList("", "id", "taskDates", "name", "length", "advancement", "coordinator", "resources", "predecessors");
    final DefaultEnumerationOption<String> topLabelOption = new DefaultEnumerationOption<String>("taskLabelUp", taskProperties);
    final DefaultEnumerationOption<String> bottomLabelOption = new DefaultEnumerationOption<String>("taskLabelDown", taskProperties);
    final DefaultEnumerationOption<String> leftLabelOption = new DefaultEnumerationOption<String>("taskLabelLeft", taskProperties);
    final DefaultEnumerationOption<String> rightLabelOption = new DefaultEnumerationOption<String>("taskLabelRight", taskProperties);
    taskLabelSceneApi = new TaskLabelSceneBuilder.InputApi() {
      @Override
      public EnumerationOption getTopLabelOption() {
        return topLabelOption;
      }

      @Override
      public EnumerationOption getBottomLabelOption() {
        return bottomLabelOption;
      }

      @Override
      public EnumerationOption getLeftLabelOption() {
        return leftLabelOption;
      }

      @Override
      public EnumerationOption getRightLabelOption() {
        return rightLabelOption;
      }

      @Override
      public int getFontSize() {
        return input.getLabelsFontSize();
      }
    };

    myLabelsRenderer = new TaskLabelSceneBuilder<>(new TaskLabelSceneTaskApi(), taskLabelSceneApi, myLabelsLayer);
    myChartApi = input.getChartApi(myLabelsRenderer);
    this.mySplitter = new TaskActivitySplitter<ITask>(
      input::getStartDate,
      myChartApi::getEndDate,
      input::createLength
    );
    myTaskActivityRenderer = createTaskActivitySceneBuilder(getPrimitiveContainer(), myChartApi,
        new TaskActivitySceneBuilder.Style(0));
    myBaselineActivityRenderer = createTaskActivitySceneBuilder(
        getPrimitiveContainer().getLayer(2), myChartApi,
        new TaskActivitySceneBuilder.Style(getRectangleHeight()));
  }

  public Canvas render() {
    getPrimitiveContainer().clear();
    getPrimitiveContainer().getLayer(0).clear();
    getPrimitiveContainer().getLayer(1).clear();
    getPrimitiveContainer().getLayer(2).clear();
    getPrimitiveContainer().setOffset(0, input.getHeaderHeight() - input.getVerticalOffset());
    getPrimitiveContainer().getLayer(2).setOffset(0, input.getHeaderHeight() - input.getVerticalOffset());

    VerticalPartitioning vp = input.getVerticalPartitioning();
    vp.build(input.getTasksInDocumentOrder());
    OffsetList defaultUnitOffsets = input.getTasksUnitOffsets();

    renderVisibleTasks(input.getVisibleTaskSceneTasks(), defaultUnitOffsets);
    renderTasksAboveAndBelowViewport(vp.getAboveViewport(), vp.getBelowViewport(), defaultUnitOffsets);
    renderDependencies();

    return getPrimitiveContainer();
  }

  public TaskLabelSceneBuilder.InputApi getTaskLabelSceneApi() {
    return taskLabelSceneApi;
  }

  private Canvas getPrimitiveContainer() {
    return canvas;
  }

  private void renderDependencies() {
    DependencySceneBuilder.ChartApi chartApi = new DependencySceneBuilder.ChartApi() {
      @Override
      public int getBarHeight() {
        return getRectangleHeight();
      }
    };
    var taskApi = new DependencySceneTaskApi(input.getVisibleTasks(), mySplitter);
    DependencySceneBuilder<ITask, BarChartConnectorImpl> dependencyRenderer = new DependencySceneBuilder<>(
        getPrimitiveContainer(), getPrimitiveContainer().getLayer(1), taskApi, chartApi);
    dependencyRenderer.build();
  }

  private void renderTasksAboveAndBelowViewport(List<ITaskSceneTask> tasksAboveViewport, List<ITaskSceneTask> tasksBelowViewport,
      OffsetList defaultUnitOffsets) {
    for (ITaskSceneTask nextAbove : tasksAboveViewport) {
      List<ITaskActivity<ITaskSceneTask>> activities = /*nextAbove.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextAbove)) : */nextAbove.getActivities();
      for (Canvas.Shape s : renderActivities(-1, nextAbove, activities, defaultUnitOffsets, false)) {
        s.setVisible(false);
      }
    }
    for (ITaskSceneTask nextBelow : tasksBelowViewport) {
      List<ITaskActivity<ITaskSceneTask>> activities = /*nextBelow.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextBelow)) : */nextBelow.getActivities();
      List<Polygon> rectangles = renderActivities(input.getVisibleTasks().size() + 1, nextBelow, activities,
          defaultUnitOffsets, false);
      for (Polygon nextRectangle : rectangles) {
        nextRectangle.setVisible(false);
      }
    }
  }

  private void renderVisibleTasks(List<ITaskSceneTask> visibleTasks, OffsetList defaultUnitOffsets) {
    List<Polygon> boundPolygons = Lists.newArrayList();
    int rowNum = 0;
    for (ITaskSceneTask t : visibleTasks) {
      boundPolygons.clear();
      List<ITaskActivity<ITaskSceneTask>> activities = t.getActivities();
      activities = mySplitter.split(activities);
      List<Polygon> rectangles = renderActivities(rowNum, t, activities, defaultUnitOffsets, true);
      for (Polygon p : rectangles) {
        if (p.getModelObject() != null) {
          boundPolygons.add(p);
        }
      }
      renderLabels(boundPolygons);
      renderBaseline(t, rowNum, defaultUnitOffsets);
      rowNum++;
      Canvas.Line nextLine = getPrimitiveContainer().createLine(0, rowNum * getRowHeight(),
          input.getWidth(), rowNum * getRowHeight());
      nextLine.setForegroundColor(Color.GRAY);
    }
  }

  public int getRowHeight() {
    return myChartApi.getRowHeight();
  }

  private void renderBaseline(ITaskSceneTask t, int rowNum, OffsetList defaultUnitOffsets) {
    TaskActivitiesSceneAlgorithm alg = new TaskActivitiesSceneAlgorithm(
      input.getCalendar(),
      (Date s, Date e) -> input.createLength(t.getDuration().getTimeUnit(), s, e)
    );
    List<GanttPreviousStateTask> baseline = input.getBaseline();
    if (baseline != null) {
      for (GanttPreviousStateTask taskBaseline : baseline) {
        if (taskBaseline.getId() == t.getRowId()) {
          Date startDate = taskBaseline.getStart().getTime();
          TimeDuration duration = input.createLength(taskBaseline.getDuration());
          Date endDate = input.getCalendar().shiftDate(startDate, duration);
          if (endDate.equals(t.getEnd().getTime())) {
            return;
          }
          List<String> styles = new ArrayList<String>();
          if (t.isMilestone()) {
            styles.add("milestone");
          }
          if (endDate.compareTo(t.getEnd().getTime()) < 0) {
            styles.add("later");
          } else {
            styles.add("earlier");
          }
          List<ITaskActivity<ITaskSceneTask>> baselineActivities = new ArrayList<ITaskActivity<ITaskSceneTask>>();
          if (t.isMilestone()) {
            baselineActivities.add(
              new TaskSceneMilestoneActivity(t, startDate, endDate, input.createLength(1))
            );
          } else {
            alg.recalculateActivities(t, baselineActivities, startDate, endDate);
          }
          List<Polygon> baselineRectangles = myBaselineActivityRenderer.renderActivities(rowNum, baselineActivities,
              defaultUnitOffsets);
          for (int i = 0; i < baselineRectangles.size(); i++) {
            Polygon r = baselineRectangles.get(i);
            r.setStyle("previousStateTask");
            for (String s : styles) {
              r.addStyle(s);
            }
            if (i == 0) {
              r.addStyle("start");
            }
            if (i == baselineRectangles.size() - 1) {
              r.addStyle("end");
            }
          }
          return;
        }
      }
    }
  }

  private static Predicate<Polygon> REMOVE_SUPERTASK_ENDINGS = new Predicate<Polygon>() {
    @Override
    public boolean apply(@Nullable Polygon shape) {
      return !shape.hasStyle("task.ending");
    }
  };
  private List<Polygon> renderActivities(final int rowNum, ITaskSceneTask t, List<ITaskActivity<ITaskSceneTask>> activities,
      OffsetList defaultUnitOffsets, boolean areVisible) {
    List<Polygon> rectangles = myTaskActivityRenderer.renderActivities(rowNum, activities, defaultUnitOffsets);
    if (areVisible && !myTaskApi.hasNestedTasks(t) && !t.isMilestone() && !t.isProjectTask()) {
      renderProgressBar(Lists.newArrayList(Iterables.filter(rectangles, REMOVE_SUPERTASK_ENDINGS)));
    }
    if (areVisible && myTaskApi.hasNotes(t)) {
      Rectangle notes = getPrimitiveContainer().createRectangle(input.getWidth() - 24, rowNum * getRowHeight() + getRowHeight()/2 - 8, 16, 16);
      notes.setStyle("task.notesMark");
      getPrimitiveContainer().bind(notes, t);
    }
    return rectangles;
  }

  private void renderLabels(List<Polygon> rectangles) {
    if (!rectangles.isEmpty()) {
      myLabelsRenderer.renderLabels(rectangles);
    }
  }

  private void renderProgressBar(List<Polygon> rectangles) {
    if (rectangles.isEmpty()) {
      return;
    }
    final Canvas container = getPrimitiveContainer().getLayer(0);
    final TimeUnit timeUnit = input.getProgressBarTimeUnit();
    final ITaskSceneTask task = ((ITaskActivity<ITaskSceneTask>) rectangles.get(0).getModelObject()).getOwner();
    float length = task.getDuration().getLength(timeUnit);
    float completed = task.getCompletionPercentage() * length / 100f;
    Polygon lastProgressRectangle = null;

    for (Polygon nextRectangle : rectangles) {
      final ITaskActivity<ITaskSceneTask> nextActivity = (ITaskActivity<ITaskSceneTask>) nextRectangle.getModelObject();
      final float nextLength = nextActivity.getDuration().getLength(timeUnit);

      final int nextProgressBarLength;
      if (completed > nextLength || nextActivity.getIntensity() == 0f) {
        nextProgressBarLength = nextRectangle.getWidth();
        if (nextActivity.getIntensity() > 0f) {
          completed -= nextLength;
        }
      } else {
        nextProgressBarLength = (int) (nextRectangle.getWidth() * (completed / nextLength));
        completed = 0f;
      }

      final Rectangle nextProgressBar = container.createRectangle(nextRectangle.getLeftX(),
          nextRectangle.getMiddleY() - 1, nextProgressBarLength, 3);
      nextProgressBar.setStyle(completed == 0f ? "task.progress.end" : "task.progress");
      getPrimitiveContainer().getLayer(0).bind(nextProgressBar, task);
      if (completed == 0) {
        lastProgressRectangle = nextRectangle;
        break;
      }
    }
    if (lastProgressRectangle == null) {
      lastProgressRectangle = rectangles.get(rectangles.size() - 1);
    }
    // createDownSideText(lastProgressRectangle);
  }

  private int getRectangleHeight() {
    return myLabelsRenderer.getFontHeight();
  }

  public Canvas getLabelLayer() {
    return myLabelsLayer;
  }

  private TaskActivitySceneBuilder<ITaskSceneTask, ITaskActivity<ITaskSceneTask>> createTaskActivitySceneBuilder(
      Canvas canvas, TaskActivitySceneBuilder.ChartApi chartApi, TaskActivitySceneBuilder.Style style) {
    return new TaskActivitySceneBuilder<>(myTaskApi, chartApi, canvas, myLabelsRenderer, style);
  }
}
