/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.gantt;

import biz.ganttproject.app.MenuBuilderAsList;
import biz.ganttproject.ganttview.TaskTableActionConnector;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.mouse.MouseListenerBase;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm;
import net.sourceforge.ganttproject.util.MouseUtil;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Supplier;

class MouseListenerImpl extends MouseListenerBase {
  private static final Function<Task, Task> getParentTask = new Function<Task, Task>() {
    @Override
    public Task apply(Task task) {
      return task.getManager().getTaskHierarchy().getContainer(task);
    }
  };
  private static final RetainRootsAlgorithm<Task> ourRetainRootsAlgorithm = new RetainRootsAlgorithm<Task>();

  private final GanttChartController myChartImplementation;
  private final UIFacade myUiFacade;
  private final ChartComponentBase myChartComponent;
  private final Supplier<TaskTableActionConnector> myTaskTableActionFacade;

  public MouseListenerImpl(GanttChartController chartImplementation, UIFacade uiFacade, ChartComponentBase chartComponent,
                           Supplier<TaskTableActionConnector> taskTableActionFacade) {
    super(uiFacade, chartComponent, chartImplementation);
    myUiFacade = uiFacade;
    myChartImplementation = chartImplementation;
    myChartComponent = chartComponent;
    myTaskTableActionFacade = taskTableActionFacade;
  }

  private TaskSelectionManager getTaskSelectionManager() {
    return myUiFacade.getTaskSelectionManager();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      Task taskUnderPointer = myChartImplementation.findTaskUnderPointer(e.getX(), e.getY());
      if (taskUnderPointer == null) {
        getTaskSelectionManager().clear();
      }
    }
    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
      myTaskTableActionFacade.get().getTaskPropertiesAction().invoke().actionPerformed(null);
    }
  }

  @Override
  protected Action[] getPopupMenuActions(MouseEvent e) {
    List<Action> result = Lists.newArrayList();
    var menuBuilder = new MenuBuilderAsList();
    myTaskTableActionFacade.get().getContextMenuActions().invoke(menuBuilder);
    menuBuilder.actions().forEach(gpAction -> result.add(gpAction));
    result.add(GPAction.SEPARATOR);

    Arrays.asList(myChartComponent.getPopupMenuActions(e)).forEach(it -> result.add(it));
    return result.toArray(new Action[result.size()]);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    myTaskTableActionFacade.get().getCommitEdit().invoke();
    String text = MouseUtil.toString(e);
    super.mousePressed(e);

    // If there is no task under the mouse pointer, we consider dragging the
    // chart
    Task taskUnderPointer = myChartImplementation.findTaskUnderPointer(e.getX(), e.getY());
    if (taskUnderPointer == null) {
      if (text.equals(GPAction.getKeyStrokeText("mouse.drag.chart"))) {
        startScrollView(e);
      }
      return;
    }

    // Process selection change before doing other interactions
    if (text.equals(GPAction.getKeyStrokeText("mouse.select.single"))) {
      getTaskSelectionManager().clear();
    }
    if (text.equals(GPAction.getKeyStrokeText("mouse.select.single"))) {
      getTaskSelectionManager().setSelectedTasks(Collections.singletonList(taskUnderPointer), myChartImplementation);
    }
    if (text.equals(GPAction.getKeyStrokeText("mouse.select.multiple"))) {
      var newSelection = new LinkedHashSet<>(getTaskSelectionManager().getSelectedTasks());
      newSelection.add(taskUnderPointer);
      getTaskSelectionManager().setSelectedTasks(new ArrayList<>(newSelection), myChartImplementation);
    }

    // Now examine what exactly is under the pointer
    ChartItem itemUnderPoint = myChartImplementation.getChartItemUnderMousePoint(e.getX(), e.getY());
    if (itemUnderPoint instanceof TaskRegularAreaChartItem) {
      // If it is a plain task area then either drag the task or create a
      // dependency,
      // depending on the settings.
      if (text.equals(GPAction.getKeyStrokeText("mouse.drag.task"))) {
        startDragTasks(e, taskUnderPointer);
        return;
      }
      if (text.equals(GPAction.getKeyStrokeText("mouse.dependency"))) {
        startDrawDependency(e, itemUnderPoint);
        return;
      }
    } else {
      // Otherwise process boundary change or progress change
      handleEvent(itemUnderPoint, e);
    }
  }

  private void startDrawDependency(MouseEvent e, ChartItem itemUnderPoint) {
    myChartImplementation.beginDrawDependencyInteraction(e, (TaskRegularAreaChartItem) itemUnderPoint);
  }

  private void startDragTasks(MouseEvent e, Task taskUnderPointer) {
    if (!getTaskSelectionManager().isTaskSelected(taskUnderPointer)) {
      getTaskSelectionManager().clear();
      getTaskSelectionManager().setSelectedTasks(Collections.singletonList(taskUnderPointer), myChartImplementation);
    }
    List<Task> roots = Lists.newArrayList();
    ourRetainRootsAlgorithm.run(getTaskSelectionManager().getSelectedTasks().toArray(new Task[0]), getParentTask, roots);
    myChartImplementation.beginMoveTaskInteractions(e, roots);
  }

  private void handleEvent(ChartItem itemUnderPoint, MouseEvent e) {
    if (itemUnderPoint instanceof TaskBoundaryChartItem) {
      TaskBoundaryChartItem taskBoundary = (TaskBoundaryChartItem) itemUnderPoint;
      if (taskBoundary.isStartBoundary()) {
        myChartImplementation.beginChangeTaskStartInteraction(e, taskBoundary);
      } else {
        myChartImplementation.beginChangeTaskEndInteraction(e, taskBoundary);
      }
    } else if (itemUnderPoint instanceof TaskProgressChartItem) {
      myChartImplementation.beginChangeTaskProgressInteraction(e, (TaskProgressChartItem) itemUnderPoint);
    }
  }
}
