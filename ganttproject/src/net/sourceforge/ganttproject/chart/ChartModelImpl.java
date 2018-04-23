/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.scene.SceneBuilder;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeUnitStack;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskNotesChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Controls painting of the Gantt chart
 */
public class ChartModelImpl extends ChartModelBase {

  private List<Task> myVisibleTasks;

  private final TaskRendererImpl2 myTaskRendererImpl;

  private TaskManager taskManager;

  // private boolean isPreviousState = false;

  private int rowHeight = 20;

  private final ColorOption myTaskDefaultColorOption;

  private final GPOptionGroup myTaskDefaultsOptions;

  private Set<Task> myHiddenTasks;

  private List<GanttPreviousStateTask> myBaseline;

  public ChartModelImpl(TaskManager taskManager, TimeUnitStack timeUnitStack, final UIConfiguration projectConfig) {
    super(taskManager, timeUnitStack, projectConfig);
    this.taskManager = taskManager;
    myTaskRendererImpl = new TaskRendererImpl2(this);
    getRenderers().add(myTaskRendererImpl);

    myTaskDefaultColorOption = taskManager.getTaskDefaultColorOption();
    myTaskDefaultsOptions = new GPOptionGroup("ganttChartDefaults",
        new GPOption[] { taskManager.getTaskNamePrefixOption(), taskManager.getTaskCopyNamePrefixOption(), myTaskDefaultColorOption,
            getTaskManager().getDependencyHardnessOption() });
    myTaskDefaultsOptions.setI18Nkey(
        new OptionsPageBuilder.I18N().getCanonicalOptionLabelKey(getTaskManager().getDependencyHardnessOption()),
        "hardness");
    myTaskDefaultsOptions.setI18Nkey(OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Strong"),
        "hardness.strong");
    myTaskDefaultsOptions.setI18Nkey(OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Rubber"),
        "hardness.rubber");

  }

  @Override
  public void setVisibleTasks(List<Task> visibleTasks) {
    myVisibleTasks = visibleTasks;
  }

  public void setExplicitlyHiddenTasks(Set<Task> hiddenTasks) {
    myHiddenTasks = hiddenTasks;
  }

  @Override
  public ChartItem getChartItemWithCoordinates(int x, int y) {
    ChartItem result = findTaskProgressItem(x, y);
    if (result == null) {
      result = findTaskBoundaryItem(x, y);
    }
    if (result == null) {
      result = super.getChartItemWithCoordinates(x, y);
    }
    return result;
  }

  private ChartItem findTaskProgressItem(int x, int y) {
    ChartItem result = null;
    Canvas.Shape primitive = myTaskRendererImpl.getPrimitiveContainer().getLayer(0).getPrimitive(
        x, 4, y/* - getChartUIConfiguration().getHeaderHeight() */, 0);
    if (primitive instanceof Canvas.Rectangle) {
      Canvas.Rectangle rect = (Canvas.Rectangle) primitive;
      if ("task.progress.end".equals(primitive.getStyle()) && rect.getRightX() >= x - 4 && rect.getRightX() <= x + 4) {
        result = new TaskProgressChartItem((Task) primitive.getModelObject());
      }
    }
    return result;
  }

  public Canvas.Shape getGraphicPrimitive(Object modelObject) {
    for (SceneBuilder renderer : getRenderers()) {
      Canvas.Shape result = renderer.getCanvas().getPrimitive(modelObject);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private ChartItem findTaskBoundaryItem(int x, int y) {
    ChartItem result = null;
    Canvas.Shape primitive = myTaskRendererImpl.getPrimitiveContainer().getPrimitive(x, y);
    if (primitive == null) {
      primitive = myTaskRendererImpl.getPrimitiveContainer().getLayer(1).getPrimitive(x, y);
    }
    if (primitive instanceof Canvas.Polygon) {
      Canvas.Polygon rect = (Canvas.Polygon) primitive;
      if ("task.notesMark".equals(rect.getStyle())) {
        return new TaskNotesChartItem((Task)primitive.getModelObject());
      }
      TaskActivity activity = (TaskActivity) primitive.getModelObject();
      if (activity != null) {
        if (activity.isFirst() && rect.getLeftX() - 2 <= x && rect.getLeftX() + 2 >= x) {
          result = new TaskBoundaryChartItem(activity.getOwner(), true);
        }
        if (result == null && activity.isLast() && rect.getRightX() - 2 <= x
            && rect.getRightX() + 2 >= x) {
          result = new TaskBoundaryChartItem(activity.getOwner(), false);
        }
        if (result == null) {
          result = new TaskRegularAreaChartItem(activity.getOwner());
        }
      }
    }
    return result;
  }

  // public java.awt.Rectangle getBoundingRectangle(Task task) {
  // java.awt.Rectangle result = null;
  // TaskActivity[] activities = task.getActivities();
  // for (int i = 0; i < activities.length; i++) {
  // GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
  // .getPrimitive(activities[i]);
  // if (nextRectangle != null) {
  // java.awt.Rectangle nextAwtRectangle = new java.awt.Rectangle(
  // nextRectangle.myLeftX, nextRectangle.myTopY,
  // nextRectangle.myWidth, nextRectangle.myHeight);
  // if (result == null) {
  // result = nextAwtRectangle;
  // } else {
  // result = result.union(nextAwtRectangle);
  // }
  // }
  // }
  // return result;
  // }

  // GraphicPrimitiveContainer.Rectangle[] getTaskActivityRectangles(Task task)
  // {
  // List<Rectangle> result = new ArrayList<Rectangle>();
  // TaskActivity[] activities = task.getActivities();
  // for (int i = 0; i < activities.length; i++) {
  // GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
  // .getPrimitive(activities[i]);
  // if (nextRectangle!=null) {
  // result.add(nextRectangle);
  // }
  // }
  // return result.toArray(new GraphicPrimitiveContainer.Rectangle[0]);
  // }

  List<Task> getVisibleTasks() {
    return myVisibleTasks == null ? Collections.<Task> emptyList() : myVisibleTasks;
  }

  TaskContainmentHierarchyFacade getTaskContainment() {
    return myTaskManager.getTaskHierarchy();
  }

  @Override
  public int calculateRowHeight() {
    rowHeight = myTaskRendererImpl.calculateRowHeight();
    return rowHeight;
  }

  // @Override
  // protected int getRowCount() {
  // return getTaskManager().getTaskCount();
  // }

  @Override
  public TaskManager getTaskManager() {
    return taskManager;
  }

  @Override
  public GPOptionGroup[] getChartOptionGroups() {
    GPOptionGroup[] superGroups = super.getChartOptionGroups();
    List<GPOptionGroup> result = Lists.newArrayList();
    result.add(myTaskDefaultsOptions);
    result.addAll(Arrays.asList(superGroups));
    result.add(myTaskRendererImpl.getLabelOptions());
    return result.toArray(new GPOptionGroup[result.size()]);
  }

  public ColorOption getTaskDefaultColorOption() {
    return myTaskDefaultColorOption;
  }

  public GPOptionGroup getTaskLabelOptions() {
    return myTaskRendererImpl.getLabelOptions();
  }

  public int setBaseline(List<GanttPreviousStateTask> tasks) {
    myBaseline = tasks;
    return (calculateRowHeight());
  }

  List<GanttPreviousStateTask> getBaseline() {
    return myBaseline;
  }

  @Override
  public ChartModelBase createCopy() {
    ChartModelImpl result = new ChartModelImpl(getTaskManager(), getTimeUnitStack(), getProjectConfig());
    super.setupCopy(result);
    result.setVisibleTasks(getVisibleTasks());
    result.setBaseline(getBaseline());
    return result;
  }

  public boolean isExplicitlyHidden(Task task) {
    return myHiddenTasks == null ? false : myHiddenTasks.contains(task);
  }

  public EnumerationOption getDependencyHardnessOption() {
    return getTaskManager().getDependencyHardnessOption();
  }
}
