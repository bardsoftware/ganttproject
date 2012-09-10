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
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivitiesAlgorithm;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.ActivityBinding;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.grid.OffsetList;
import biz.ganttproject.core.chart.render.AlphaRenderingOption;
import biz.ganttproject.core.chart.scene.BarChartActivity;
import biz.ganttproject.core.chart.scene.Polyline;
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder;
import biz.ganttproject.core.chart.scene.gantt.DependencySceneBuilder;
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Renders task rectangles, dependency lines and all task-related text strings
 * in the gantt chart
 */
public class TaskRendererImpl2 extends ChartRendererBase {
  private ChartModelImpl myModel;

  private GPOptionGroup myLabelOptions;

  private final TaskLabelSceneBuilder<Task> myLabelsRenderer;

  private TaskActivitySceneBuilder.TaskApi<Task, TaskActivity> myTaskApi = new TaskActivitySceneBuilder.TaskApi<Task, TaskActivity>() {
    @Override
    public boolean isFirst(TaskActivity activity) {
      return activity.isFirst();
    }
    @Override
    public boolean isLast(TaskActivity activity) {
      return activity.isLast();
    }
    @Override
    public boolean isVoid(TaskActivity activity) {
      return activity.getIntensity() == 0f;
    }
    @Override
    public boolean isCriticalTask(Task task) {
      return myModel.getChartUIConfiguration().isCriticalPathOn() && task.isCritical();
    }
    @Override
    public boolean isProjectTask(Task task) {
      return task.isProjectTask();
    }
    @Override
    public boolean isMilestone(Task task) {
      return task.isMilestone();
    }
    @Override
    public boolean hasNestedTasks(Task task) {
      return getChartModel().getTaskManager().getTaskHierarchy().hasNestedTasks(task);
    }
    @Override
    public Color getColor(Task task) {
      return task.getColor();
    }
    @Override
    public boolean hasNotes(Task task) {
      return !Strings.isNullOrEmpty(task.getNotes());
    }
  };

  class TaskActivityChartApi implements TaskActivitySceneBuilder.ChartApi {
    private final int myBarHeight;
    TaskActivityChartApi(int barHeight) {
      myBarHeight = barHeight;
    }
    @Override
    public Date getChartStartDate() {
      return myModel.getOffsetAnchorDate();
    }
    @Override
    public Date getEndDate() {
      return getChartModel().getEndDate();
    }
    @Override
    public OffsetList getBottomUnitOffsets() {
      return getChartModel().getBottomUnitOffsets();
    }
    @Override
    public int getRowHeight() {
      return calculateRowHeight();
    }
    @Override
    public int getBarHeight() {
      return myBarHeight;
    }
    @Override
    public int getViewportWidth() {
      return myModel.getBounds().width;
    }
    @Override
    public AlphaRenderingOption getWeekendOpacityOption() {
      return myModel.getChartUIConfiguration().getWeekendAlphaValue();
    }
  }

  private final TaskActivitySceneBuilder<Task, TaskActivity> myTaskActivityRenderer;
  private final TaskActivitySceneBuilder<Task, TaskActivity> myBaselineActivityRenderer;

  private final Canvas myLabelsLayer;

  public TaskRendererImpl2(ChartModelImpl model) {
    super(model);
    this.myModel = model;
    getPrimitiveContainer().setOffset(0, model.getChartUIConfiguration().getHeaderHeight());
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    getPrimitiveContainer().newLayer();
    myLabelsLayer = getPrimitiveContainer().newLayer();

    List<String> taskProperties = Lists.newArrayList("", "id", "taskDates", "name", "length", "advancement", "coordinator", "resources", "predecessors");
    final DefaultEnumerationOption<String> topLabelOption = new DefaultEnumerationOption<String>("taskLabelUp", taskProperties);
    final DefaultEnumerationOption<String> bottomLabelOption = new DefaultEnumerationOption<String>("taskLabelDown", taskProperties);
    final DefaultEnumerationOption<String> leftLabelOption = new DefaultEnumerationOption<String>("taskLabelLeft", taskProperties);
    final DefaultEnumerationOption<String> rightLabelOption = new DefaultEnumerationOption<String>("taskLabelRight", taskProperties);

    myLabelsRenderer = new TaskLabelSceneBuilder<Task>(new TaskLabelSceneBuilder.TaskApi<Task>() {
      TaskProperties myLabelFormatter = new TaskProperties(getChartModel().getTimeUnitStack());

      @Override
      public Object getProperty(Task task, String propertyID) {
        return myLabelFormatter.getProperty(task, propertyID);
      }
    }, new TaskLabelSceneBuilder.InputApi() {
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
        return getChartModel().getChartUIConfiguration().getChartFont().getSize();
      }
    }, myLabelsLayer);
    myLabelOptions = new ChartOptionGroup("ganttChartDetails",
        new GPOption[] {topLabelOption, bottomLabelOption, leftLabelOption, rightLabelOption},
        model.getOptionEventDispatcher());

    myTaskActivityRenderer = createTaskActivitySceneBuilder(getPrimitiveContainer(), new TaskActivityChartApi(getRectangleHeight()),
        new TaskActivitySceneBuilder.Style(0));
    myBaselineActivityRenderer = createTaskActivitySceneBuilder(
        getPrimitiveContainer().getLayer(2), new TaskActivityChartApi(getRectangleHeight()/2),
        new TaskActivitySceneBuilder.Style(getRectangleHeight()));
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
    OffsetList defaultUnitOffsets = getChartModel().getDefaultUnitOffsets();

    renderVisibleTasks(getVisibleTasks(), defaultUnitOffsets);
    renderTasksAboveAndBelowViewport(tasksAboveViewport, tasksBelowViewport, defaultUnitOffsets);
    renderDependencies();
  }

  private void renderDependencies() {
    DependencySceneBuilder.ChartApi chartApi = new DependencySceneBuilder.ChartApi() {
      @Override
      public int getBarHeight() {
        return getRectangleHeight();
      }
    };
    DependencySceneBuilder.TaskApi<Task, TaskDependency> taskApi = new DependencySceneBuilder.TaskApi<Task, TaskDependency>() {
      @Override
      public boolean isMilestone(Task task) {
        return task.isMilestone();
      }

      @Override
      public Dimension getUnitVector(BarChartActivity<Task> activity, TaskDependency dependency) {
        ActivityBinding activityBinding = dependency.getActivityBinding();
        TaskDependencyConstraint.Type type = dependency.getConstraint().getType();
        if (activity == activityBinding.getDependeeActivity()) {
          if (type == TaskDependencyConstraint.Type.finishfinish || type == TaskDependencyConstraint.Type.finishstart) {
            return Polyline.Vector.EAST;
          }
          return Polyline.Vector.WEST;
        } else if (activity == activityBinding.getDependantActivity()) {
          if (type == TaskDependencyConstraint.Type.finishfinish || type == TaskDependencyConstraint.Type.startfinish) {
            return Polyline.Vector.EAST;
          }
          return Polyline.Vector.WEST;
        } else {
          assert false : "Should not be here";
          return null;
        }
      }

      @Override
      public String getStyle(TaskDependency dependency) {
        return dependency.getHardness() == TaskDependency.Hardness.STRONG ? "dependency.line.hard" : "dependency.line.rubber";
      }

      @Override
      public Iterable<TaskDependency> getConnectors(Task task) {
        return Arrays.asList(task.getDependencies().toArray());
      }

      @Override
      public List<Task> getTasks() {
        return myModel.getVisibleTasks();
      }
    };
    DependencySceneBuilder<Task, TaskDependency> dependencyRenderer = new DependencySceneBuilder<Task, TaskDependency>(
        getPrimitiveContainer(), getPrimitiveContainer().getLayer(1), taskApi, chartApi);
    dependencyRenderer.build();
  }

  private void renderTasksAboveAndBelowViewport(List<Task> tasksAboveViewport, List<Task> tasksBelowViewport,
      OffsetList defaultUnitOffsets) {
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

  private void renderVisibleTasks(List<Task> visibleTasks, OffsetList defaultUnitOffsets) {
    int rowNum = 0;
    for (Task t : visibleTasks) {
      List<TaskActivity> activities = t.isMilestone() ? Collections.<TaskActivity> singletonList(new MilestoneTaskFakeActivity(
          t)) : Arrays.asList(t.getActivities());
      List<Rectangle> rectangles = renderActivities(rowNum, t, activities, defaultUnitOffsets);
      renderLabels(rectangles);
      renderBaseline(t, rowNum, defaultUnitOffsets);
      rowNum++;
      Canvas.Line nextLine = getPrimitiveContainer().createLine(0, rowNum * getRowHeight(),
          (int) getChartModel().getBounds().getWidth(), rowNum * getRowHeight());
      nextLine.setForegroundColor(Color.GRAY);
    }
  }

  private int getRowHeight() {
    return calculateRowHeight();
  }

  private void renderBaseline(Task t, int rowNum, OffsetList defaultUnitOffsets) {
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
          List<Rectangle> baselineRectangles = myBaselineActivityRenderer.renderActivities(rowNum, baselineActivities,
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
      OffsetList defaultUnitOffsets) {
    List<Rectangle> rectangles = myTaskActivityRenderer.renderActivities(rowNum, activities, defaultUnitOffsets);
    if (!getChartModel().getTaskManager().getTaskHierarchy().hasNestedTasks(t) && !t.isMilestone()) {
      renderProgressBar(rectangles);
    }
    if (myTaskApi.hasNotes(t)) {
      Rectangle notes = getPrimitiveContainer().createRectangle(myModel.getBounds().width - 24, rowNum * getRowHeight() + getRowHeight()/2 - 8, 16, 16);
      notes.setStyle("task.notesMark");
      getPrimitiveContainer().bind(notes, t);
    }
    return rectangles;
  }

  private void renderLabels(List<Rectangle> rectangles) {
    if (!rectangles.isEmpty()) {
      myLabelsRenderer.renderLabels(rectangles);
    }
  }

  private void renderProgressBar(List<Rectangle> rectangles) {
    if (rectangles.isEmpty()) {
      return;
    }
    final Canvas container = getPrimitiveContainer().getLayer(0);
    final TimeUnit timeUnit = getChartModel().getTimeUnitStack().getDefaultTimeUnit();
    final Task task = ((TaskActivity) rectangles.get(0).getModelObject()).getOwner();
    float length = task.getDuration().getLength(timeUnit);
    float completed = task.getCompletionPercentage() * length / 100f;
    Rectangle lastProgressRectangle = null;

    for (Rectangle nextRectangle : rectangles) {
      final TaskActivity nextActivity = (TaskActivity) nextRectangle.getModelObject();
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

  public GPOptionGroup getLabelOptions() {
    return myLabelOptions;
  }

  int calculateRowHeight() {
    int rowHeight = myLabelsRenderer.calculateRowHeight();
    if (myModel.getBaseline() != null) {
      rowHeight = rowHeight + 8;
    }
    return rowHeight;
  }

  private int getRectangleHeight() {
    return myLabelsRenderer.getFontHeight();
  }

  Canvas getLabelLayer() {
    return myLabelsLayer;
  }

  private TaskActivitySceneBuilder<Task, TaskActivity> createTaskActivitySceneBuilder(
      Canvas canvas, TaskActivitySceneBuilder.ChartApi chartApi, TaskActivitySceneBuilder.Style style) {
    return new TaskActivitySceneBuilder<Task, TaskActivity>(myTaskApi, chartApi, canvas, myLabelsRenderer, style);
  }
}
