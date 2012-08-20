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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.chart.canvas.GraphicPrimitiveContainer;
import biz.ganttproject.core.chart.canvas.GraphicPrimitiveContainer.Rectangle;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;

import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivitiesAlgorithm;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class TaskRendererImpl2 extends ChartRendererBase {
  private ChartModelImpl myModel;

  private GPOptionGroup[] myOptionGroups;

  private final TaskLabelsRendererImpl myLabelsRenderer;

  private final GraphicPrimitiveContainer myLabelsLayer;

  public TaskRendererImpl2(ChartModelImpl model) {
    super(model);
    this.myModel = model;
    getPrimitiveContainer().setOffset(0, model.getChartUIConfiguration().getHeaderHeight());
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    myLabelsLayer = getPrimitiveContainer().newLayer();
    myLabelsRenderer = new TaskLabelsRendererImpl(model, myLabelsLayer);
    myOptionGroups = new GPOptionGroup[] { myLabelsRenderer.getOptionGroup() };
  }

  private List<Task> getVisibleTasks() {
    return ((ChartModelImpl) getChartModel()).getVisibleTasks();
  }

  @Override
  public void render() {
    getPrimitiveContainer().clear();
    getPrimitiveContainer().getLayer(0).clear();
    getPrimitiveContainer().getLayer(1).clear();
    getPrimitiveContainer().getLayer(2).clear();
    getPrimitiveContainer().setOffset(0,
        myModel.getChartUIConfiguration().getHeaderHeight() - myModel.getVerticalOffset());
    getPrimitiveContainer().getLayer(2).setOffset(0,
        myModel.getChartUIConfiguration().getHeaderHeight() - myModel.getVerticalOffset());
    List<Task> tasksAboveViewport = new ArrayList<Task>();
    List<Task> tasksBelowViewport = new ArrayList<Task>();

    collectTasksAboveAndBelowViewport(getVisibleTasks(), tasksAboveViewport, tasksBelowViewport);
    List<Offset> defaultUnitOffsets = getChartModel().getDefaultUnitOffsets();

    renderVisibleTasks(getVisibleTasks(), defaultUnitOffsets);
    renderTasksAboveAndBelowViewport(tasksAboveViewport, tasksBelowViewport, defaultUnitOffsets);
    renderDependencies();
  }

  private void renderDependencies() {
    TaskDependencyRenderer dependencyRenderer = new TaskDependencyRenderer(myModel.getVisibleTasks(),
        getPrimitiveContainer(), getPrimitiveContainer().getLayer(1));
    dependencyRenderer.createDependencyLines();
  }

  private void renderTasksAboveAndBelowViewport(List<Task> tasksAboveViewport, List<Task> tasksBelowViewport,
      List<Offset> defaultUnitOffsets) {
    for (Task nextAbove : tasksAboveViewport) {
      List<TaskActivity> activities = nextAbove.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextAbove)) : Arrays.asList(nextAbove.getActivities());
      List<Rectangle> rectangles = renderActivities(-1, nextAbove, activities, defaultUnitOffsets);
      for (Rectangle nextRectangle : rectangles) {
        nextRectangle.setVisible(false);
      }
    }
    for (Task nextBelow : tasksBelowViewport) {
      List<TaskActivity> activities = nextBelow.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          nextBelow)) : Arrays.asList(nextBelow.getActivities());
      List<Rectangle> rectangles = renderActivities(getVisibleTasks().size() + 1, nextBelow, activities,
          defaultUnitOffsets);
      for (Rectangle nextRectangle : rectangles) {
        nextRectangle.setVisible(false);
      }
    }
  }

  private void renderVisibleTasks(List<Task> visibleTasks, List<Offset> defaultUnitOffsets) {
    int rowNum = 0;
    for (Task t : visibleTasks) {
      List<TaskActivity> activities = t.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          t)) : Arrays.asList(t.getActivities());
      List<Rectangle> rectangles = renderActivities(rowNum, t, activities, defaultUnitOffsets);
      renderLabels(rectangles);
      renderBaseline(t, rowNum, defaultUnitOffsets);
      rowNum++;
      GraphicPrimitiveContainer.Line nextLine = getPrimitiveContainer().createLine(0, rowNum * getRowHeight(),
          (int) getChartModel().getBounds().getWidth(), rowNum * getRowHeight());
      nextLine.setForegroundColor(Color.GRAY);
    }
  }

  private int getRowHeight() {
    return myModel.getRowHeight();
  }

  private void renderBaseline(Task t, int rowNum, List<Offset> defaultUnitOffsets) {
    TaskActivitiesAlgorithm alg = new TaskActivitiesAlgorithm(getCalendar());
    List<GanttPreviousStateTask> baseline = myModel.getBaseline();
    if (baseline != null) {
      for (GanttPreviousStateTask taskBaseline : baseline) {
        if (taskBaseline.getId() == t.getTaskID()) {
          Date startDate = taskBaseline.getStart().getTime();
          TimeDuration duration = getChartModel().getTaskManager().createLength(taskBaseline.getDuration());
          Date endDate = getCalendar().shiftDate(startDate, duration);
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
          List<TaskActivity> baselineActivities = new ArrayList<TaskActivity>();
          if (t.isMilestone()) {
            baselineActivities.add(new MilestoneTaskFakeActivity(t, startDate, endDate));
          } else {
            alg.recalculateActivities(t, baselineActivities, startDate, endDate);
          }
          TaskActivityRenderer activityRenderer = new TaskActivityRenderer(myModel,
              getPrimitiveContainer().getLayer(2), myLabelsRenderer, new TaskActivityRenderer.Style(
                  getRectangleHeight(), getRectangleHeight() / 2));
          List<Rectangle> baselineRectangles = activityRenderer.renderActivities(rowNum, baselineActivities,
              defaultUnitOffsets);
          for (int i = 0; i < baselineRectangles.size(); i++) {
            Rectangle r = baselineRectangles.get(i);
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

  private List<Rectangle> renderActivities(final int rowNum, Task t, List<TaskActivity> activities,
      List<Offset> defaultUnitOffsets) {
    TaskActivityRenderer activityRenderer = new TaskActivityRenderer(myModel, getPrimitiveContainer(),
        myLabelsRenderer, new TaskActivityRenderer.Style(0, getRectangleHeight()));
    List<Rectangle> rectangles = activityRenderer.renderActivities(rowNum, activities, defaultUnitOffsets);
    if (!getChartModel().getTaskManager().getTaskHierarchy().hasNestedTasks(t) && !t.isMilestone()) {
      renderProgressBar(rectangles);
    }
    return rectangles;
  }

  private void renderLabels(List<Rectangle> rectangles) {
    if (!rectangles.isEmpty()) {
      Rectangle lastRectangle = rectangles.get(rectangles.size() - 1);

      if (lastRectangle.isVisible()) {
        myLabelsRenderer.createRightSideText(lastRectangle);
        myLabelsRenderer.createDownSideText(lastRectangle);
        myLabelsRenderer.createUpSideText(lastRectangle);
      }
      Rectangle firstRectangle = rectangles.get(0);
      if (firstRectangle.isVisible()) {
        myLabelsRenderer.createLeftSideText(firstRectangle);
      }
    }
  }

  private void renderProgressBar(List<Rectangle> rectangles) {
    if (rectangles.isEmpty()) {
      return;
    }
    final GraphicPrimitiveContainer container = getPrimitiveContainer().getLayer(0);
    final TimeUnit timeUnit = getChartModel().getTimeUnitStack().getDefaultTimeUnit();
    final Task task = ((TaskActivity) rectangles.get(0).getModelObject()).getTask();
    float length = task.getDuration().getLength(timeUnit);
    float completed = task.getCompletionPercentage() * length / 100f;
    Rectangle lastProgressRectangle = null;

    for (Rectangle nextRectangle : rectangles) {
      final TaskActivity nextActivity = (TaskActivity) nextRectangle.getModelObject();
      final float nextLength = nextActivity.getDuration().getLength(timeUnit);

      final int nextProgressBarLength;
      if (completed > nextLength || nextActivity.getIntensity() == 0f) {
        nextProgressBarLength = nextRectangle.myWidth;
        if (nextActivity.getIntensity() > 0f) {
          completed -= nextLength;
        }
      } else {
        nextProgressBarLength = (int) (nextRectangle.myWidth * (completed / nextLength));
        completed = 0f;
      }
      final Rectangle nextProgressBar = container.createRectangle(nextRectangle.myLeftX,
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

  private void collectTasksAboveAndBelowViewport(List<Task> visibleTasks, List<Task> tasksAboveViewport,
      List<Task> tasksBelowViewport) {
    TaskContainmentHierarchyFacade containment = getChartModel().getTaskManager().getTaskHierarchy();
    List<Task> tasksInDocumentOrder = containment.getTasksInDocumentOrder();
    final Task firstVisible = visibleTasks.isEmpty() ? null : visibleTasks.get(0);
    final Task lastVisible = visibleTasks.isEmpty() ? null : visibleTasks.get(visibleTasks.size() - 1);
    List<Task> addTo = tasksAboveViewport;
    for (Task nextTask : tasksInDocumentOrder) {
      if (addTo == null) {
        if (nextTask.equals(lastVisible)) {
          addTo = tasksBelowViewport;
        }
        continue;
      }
      if (!nextTask.equals(firstVisible)) {
        addTo.add(nextTask);
      } else {
        addTo = null;
      }
    }
  }

  public GPOptionGroup[] getOptionGroups() {
    return myOptionGroups;
  }

  int calculateRowHeight() {
    return myLabelsRenderer.calculateRowHeight();
  }

  private int getRectangleHeight() {
    return myLabelsRenderer.getFontHeight();
  }

  GraphicPrimitiveContainer getLabelLayer() {
    return myLabelsLayer;
  }
}
