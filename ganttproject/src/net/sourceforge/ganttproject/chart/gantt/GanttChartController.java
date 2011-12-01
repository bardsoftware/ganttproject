/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.ChartImplementation;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.TaskChartModelFacade;
import net.sourceforge.ganttproject.chart.VisibleNodesFilter;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.GraphicPrimitive;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
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
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class GanttChartController extends AbstractChartImplementation implements ChartImplementation {
    private final TaskManager myTaskManager;
    private final ChartModelImpl myChartModel;
    private final ChartViewState myChartViewState;
    private final GanttTree2 myTree;
    private final MouseListenerImpl myMouseListener;
    private final MouseMotionListenerImpl myMouseMotionListener;

    public GanttChartController(
            IGanttProject project, UIFacade uiFacade, ChartModelImpl chartModel, ChartComponentBase chartComponent,
            GanttTree2 tree, ChartViewState chartViewState) {
        super(project, uiFacade, chartModel, chartComponent);
        myTree = tree;
        myChartViewState = chartViewState;
        myTaskManager = project.getTaskManager();
        myChartModel = chartModel;
        myMouseListener = new MouseListenerImpl(this, myChartModel, uiFacade, chartComponent, tree);
        myMouseMotionListener = new MouseMotionListenerImpl(this, chartModel, uiFacade, chartComponent);
    }

    private TaskManager getTaskManager() {
        return myTaskManager;
    }

    private ChartViewState getViewState() {
        return myChartViewState;
    }

    @Override
    public void beginChangeTaskEndInteraction(MouseEvent initiatingEvent,
            TaskBoundaryChartItem taskBoundary) {
        setActiveInteraction(new ChangeTaskEndInteraction(
            initiatingEvent, taskBoundary,
            new TimelineFacadeImpl(super.getChartModel(), getTaskManager()),
            getUIFacade(),
            getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
        setCursor(GanttGraphicArea.E_RESIZE_CURSOR);
    }

    @Override
    public void beginChangeTaskStartInteraction(MouseEvent e,
            TaskBoundaryChartItem taskBoundary) {
        setActiveInteraction(new ChangeTaskStartInteraction(e, taskBoundary,
            new TimelineFacadeImpl(getChartModel(), getTaskManager()),
            getUIFacade(),
            getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
        setCursor(GanttGraphicArea.W_RESIZE_CURSOR);
    }

    @Override
    public void beginChangeTaskProgressInteraction(MouseEvent e,
            TaskProgressChartItem taskProgress) {
        setActiveInteraction(new ChangeTaskProgressInteraction(e, taskProgress,
            new TimelineFacadeImpl(getChartModel(), getTaskManager()),
            new TaskChartModelFacade() {
                @Override
                public List<Rectangle> getTaskRectangles(Task t) {
                    List<Rectangle> result = new ArrayList<Rectangle>();
                    for (TaskActivity activity : t.getActivities()) {
                        GraphicPrimitive graphicPrimitive = myChartModel.getGraphicPrimitive(activity);
                        assert graphicPrimitive != null;
                        assert graphicPrimitive instanceof Rectangle;
                        result.add((Rectangle)graphicPrimitive);
                    }
                    return result;
                }
            },
            getUIFacade()));
        setCursor(GanttGraphicArea.CHANGE_PROGRESS_CURSOR);
    }

    @Override
    public void beginDrawDependencyInteraction(
            MouseEvent initiatingEvent, TaskRegularAreaChartItem taskArea) {
        setActiveInteraction(new DrawDependencyInteraction(initiatingEvent, taskArea,
            new TimelineFacadeImpl(getChartModel(), getTaskManager()),
            new DrawDependencyInteraction.ChartModelFacade() {
                @Override
                public Task findTaskUnderMousePointer(int xpos, int ypos) {
                    ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
                    return chartItem == null ? null : chartItem.getTask();
                }

                @Override
                public Hardness getDefaultHardness() {
                    return TaskDependency.Hardness.parse(myChartModel.getDependencyHardnessOption().getValue());
                }
            },
            getUIFacade(),
            getTaskManager().getDependencyCollection()));

    }

    @Override
    public void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks) {
        setActiveInteraction(new MoveTaskInteractions(e, tasks,
            new TimelineFacadeImpl(getChartModel(), getTaskManager()),
            getUIFacade(),
            getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
    }

    @Override
    public void paintChart(Graphics g) {
        synchronized(ChartModelBase.STATIC_MUTEX) {
            //GanttGraphicArea.super.paintComponent(g);
            ChartModel model = myChartModel;
            model.setBottomUnitWidth(getViewState().getBottomUnitWidth());
            model.setRowHeight(myTree.getRowHeight());
            model.setTopTimeUnit(getViewState().getTopTimeUnit());
            model.setBottomTimeUnit(getViewState().getBottomTimeUnit());
            VisibleNodesFilter visibleNodesFilter = new VisibleNodesFilter();
            List<Task> visibleTasks = myTree.getVisibleNodes(visibleNodesFilter);
            model.setVisibleTasks(visibleTasks);
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

    @Override
    public ChartSelection getSelection() {
        ChartSelectionImpl result = new ChartSelectionImpl() {
            @Override
            public boolean isEmpty() {
                return false;
            }
            @Override
            public void startCopyClipboardTransaction() {
                super.startCopyClipboardTransaction();
                myTree.copySelectedNode();
            }
            @Override
            public void startMoveClipboardTransaction() {
                super.startMoveClipboardTransaction();
                myTree.cutSelectedNode();
            }
        };
        return result;
    }

    @Override
    public void paste(ChartSelection selection) {
        myTree.pasteNode();
    }

    public Task findTaskUnderPointer(int xpos, int ypos) {
        ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
        return chartItem == null ? null : chartItem.getTask();
    }

    public ChartItem getChartItemUnderMousePoint(int xpos, int ypos) {
        ChartItem result = myChartModel.getChartItemWithCoordinates(xpos, ypos);
        return result;
    }
}