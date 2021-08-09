/*
GanttProject is an opensource project management tool. License: GPL3
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

import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.ganttview.TaskTableActionConnector;
import biz.ganttproject.ganttview.TaskTableChartConnector;
import com.google.common.base.Supplier;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.ChartImplementation;
import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.TaskChartModelFacade;
import net.sourceforge.ganttproject.chart.TaskRendererImpl2;
import net.sourceforge.ganttproject.chart.export.ChartImageBuilder;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskEndInteraction;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskProgressInteraction;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskStartInteraction;
import net.sourceforge.ganttproject.chart.mouse.DrawDependencyInteraction;
import net.sourceforge.ganttproject.chart.mouse.MoveTaskInteractions;
import net.sourceforge.ganttproject.chart.mouse.TimelineFacadeImpl;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

public class GanttChartController extends AbstractChartImplementation implements ChartImplementation {
  private final TaskManager myTaskManager;
  private final ChartModelImpl myChartModel;
  private final ChartViewState myChartViewState;
  private final MouseListenerImpl myMouseListener;
  private final MouseMotionListenerImpl myMouseMotionListener;
  private final TaskTableChartConnector myTaskTableConnector;
  protected CustomBalloonTip myTooltip;
  private final TaskSelectionManager mySelectionManager;

  public GanttChartController(IGanttProject project, UIFacade uiFacade, ChartModelImpl chartModel,
                              ChartComponentBase chartComponent, ChartViewState chartViewState,
                              TaskTableChartConnector taskTableConnector,
                              Supplier<TaskTableActionConnector> taskTableActionFacade) {
    super(project, uiFacade, chartModel, chartComponent);
    myChartViewState = chartViewState;
    myTaskManager = project.getTaskManager();
    myChartModel = chartModel;
    myMouseListener = new MouseListenerImpl(this, uiFacade, chartComponent, taskTableActionFacade);
    myMouseMotionListener = new MouseMotionListenerImpl(this, chartModel, uiFacade, chartComponent);
    mySelectionManager = uiFacade.getTaskSelectionManager();
    mySelection = new GanttChartSelection(myTaskManager, mySelectionManager);
    myTaskTableConnector = taskTableConnector;
    myTaskTableConnector.getVisibleTasks().addListener(
        (ListChangeListener<Task>) c -> SwingUtilities.invokeLater(() -> reset())
    );
    myTaskTableConnector.getTableScrollOffset().addListener(
        (ChangeListener<? super Number>) (wtf, old, newValue) -> {
          SwingUtilities.invokeLater(() -> {
            getChartModel().setVerticalOffset(newValue.intValue());
            reset();
          });
        }
    );
    setVScrollController(new VScrollController() {
      @Override
      public boolean isScrollable() {
        return myTaskTableConnector.isTableScrollable();
      }

      @Override
      public void scrollBy(int pixels) {
        myTaskTableConnector.getChartScrollOffset().setValue(pixels);
      }
    });
  }

  private TaskManager getTaskManager() {
    return myTaskManager;
  }

  private ChartViewState getViewState() {
    return myChartViewState;
  }

  @Override
  public void beginChangeTaskEndInteraction(MouseEvent initiatingEvent, TaskBoundaryChartItem taskBoundary) {
    setActiveInteraction(new ChangeTaskEndInteraction(taskBoundary, new TimelineFacadeImpl(super.getChartModel(),
        getTaskManager()), getUIFacade(),
        getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
    setCursor(GanttGraphicArea.E_RESIZE_CURSOR);
  }

  @Override
  public void beginChangeTaskStartInteraction(MouseEvent e, TaskBoundaryChartItem taskBoundary) {
    setActiveInteraction(new ChangeTaskStartInteraction(e, taskBoundary, new TimelineFacadeImpl(getChartModel(),
        getTaskManager()), getUIFacade(),
        getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
    setCursor(GanttGraphicArea.W_RESIZE_CURSOR);
  }

  @Override
  public void beginChangeTaskProgressInteraction(MouseEvent e, TaskProgressChartItem taskProgress) {
    setActiveInteraction(new ChangeTaskProgressInteraction(e, taskProgress, new TimelineFacadeImpl(getChartModel(),
        getTaskManager()), new TaskChartModelFacade() {
      @Override
      public List<Rectangle> getTaskRectangles(Task t) {
        return TaskRendererImpl2.getTaskRectangles(t, myChartModel);
      }
    }, getUIFacade()));
    setCursor(GanttGraphicArea.CHANGE_PROGRESS_CURSOR);
  }

  @Override
  public void beginDrawDependencyInteraction(MouseEvent initiatingEvent, TaskRegularAreaChartItem taskArea) {
    setActiveInteraction(new DrawDependencyInteraction(initiatingEvent, taskArea, new TimelineFacadeImpl(
        getChartModel(), getTaskManager()), new DrawDependencyInteraction.ChartModelFacade() {
      @Override
      public Task findTaskUnderMousePointer(int xpos, int ypos) {
        ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
        return chartItem == null ? null : chartItem.getTask();
      }

      @Override
      public Hardness getDefaultHardness() {
        return TaskDependency.Hardness.parse(getTaskManager().getDependencyHardnessOption().getValue());
      }
    }, getUIFacade(), getTaskManager().getDependencyCollection()));

  }

  @Override
  public void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks) {
    setActiveInteraction(new MoveTaskInteractions(e, tasks, new TimelineFacadeImpl(getChartModel(), getTaskManager()),
        getUIFacade(), getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
  }

  @Override
  public void paintChart(Graphics g) {
    synchronized (ChartModelBase.STATIC_MUTEX) {
      // GanttGraphicArea.super.paintComponent(g);
      ChartModel model = myChartModel;
      model.setBottomUnitWidth(getViewState().getBottomUnitWidth());
      myTaskTableConnector.getRowHeight().setValue(model.calculateRowHeight());
      model.setRowHeight(myTaskTableConnector.getRowHeight().getValue().intValue());
      model.setTopTimeUnit(getViewState().getTopTimeUnit());
      model.setBottomTimeUnit(getViewState().getBottomTimeUnit());
      List<Task> visibleTasks = myTaskTableConnector.getVisibleTasks();
      model.setVisibleTasks(visibleTasks);
      myChartModel.setTimelineTasks(getUIFacade().getCurrentTaskView().getTimelineTasks());
      model.paint(g);
      if (getActiveInteraction() != null) {
        getActiveInteraction().paint(g);
      }
    }
  }

  @Override
  public MouseListener getMouseListener() {
    return myMouseListener;
  }

  @Override
  public MouseMotionListener getMouseMotionListener() {
    return myMouseMotionListener;
  }

  @Override
  public IStatus canPaste(ChartSelection selection) {
    return Status.OK_STATUS;
  }

  private GanttChartSelection mySelection;

  @Override
  public ChartSelection getSelection() {
    return mySelection;
  }

  @Override
  public void paste(ChartSelection selection) {
    if (mySelectionManager.getSelectedTasks().size() != 1) {
      return;
    }
    mySelection.paste(mySelectionManager.getSelectedTasks().get(0));

  }

  public Task findTaskUnderPointer(int xpos, int ypos) {
    ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
    return chartItem == null ? null : chartItem.getTask();
  }

  public ChartItem getChartItemUnderMousePoint(int xpos, int ypos) {
    ChartItem result = myChartModel.getChartItemWithCoordinates(xpos, ypos);
    return result;
  }

  @Override
  public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
//    final TaskTreeUIFacade taskTree = getUIFacade().getTaskTree();
//    List<Task> visibleTasks = Lists.newArrayList();
//    for (Task t : getTaskManager().getTaskHierarchy().getDeepNestedTasks(getTaskManager().getRootTask())) {
//      if (taskTree.isVisible(t)) {
//        visibleTasks.add(t);
//      }
//    }
    settings.setVisibleTasks(myTaskTableConnector.getVisibleTasks());
    super.buildImage(settings, imageVisitor);
  }

  protected ChartImageBuilder createChartImageBuilder(GanttExportSettings settings, ChartModelBase modelCopy, GPTreeTableBase treeTable) {
    return new ChartImageBuilder(settings, modelCopy, myTaskTableConnector.getExportTreeTableApi().invoke());
  }
  void showTooltip(final int x, final int y, final String text) {
    if (myTooltip == null) {
      scheduleTask(() -> {
        if (myTooltip == null) {
          java.awt.Rectangle offset = new java.awt.Rectangle(x - 30, y, 0, 0);
          myTooltip = new CustomBalloonTip(
            getChartComponent(),
            new JLabel(text), offset,
            new ToolTipBalloonStyle(Color.YELLOW, Color.YELLOW.darker()),
            BalloonTip.Orientation.LEFT_ABOVE,
            BalloonTip.AttachLocation.ALIGNED,
            20, 20, false);
          myTooltip.setVisible(true);
        }
      });
    }
  }

  public void hideTooltip() {
    if (myTooltip != null) {
      SwingUtilities.invokeLater(() -> {
        if (myTooltip != null) {
          myTooltip.setVisible(false);
          myTooltip = null;
        }
      });
    }
  }
}
