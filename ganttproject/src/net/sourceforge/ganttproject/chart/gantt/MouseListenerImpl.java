/*
GanttProject is an opensource project management tool.
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

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Action;

import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.mouse.MouseListenerBase;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

class MouseListenerImpl extends MouseListenerBase {
    private final GanttTree2 myTree;
    private final GanttChartController myChartImplementation;
    private final UIFacade myUiFacade;
    private final ChartComponentBase myChartComponent;

    public MouseListenerImpl(
            GanttChartController chartImplementation, ChartModelImpl chartModel, UIFacade uiFacade, ChartComponentBase chartComponent, GanttTree2 tree) {
        super(uiFacade, chartComponent, chartImplementation);
        myUiFacade = uiFacade;
        myTree = tree;
        myChartImplementation = chartImplementation;
        myChartComponent = chartComponent;
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
            myTree.getTaskPropertiesAction().actionPerformed(null);
        }
    }

    @Override
    protected Action[] getPopupMenuActions() {
        Action[] treeActions = myTree.getPopupMenuActions();
        int sep = 0;
        if (treeActions.length != 0) {
            sep = 1;
        }

        Action[] chartActions = myChartComponent.getPopupMenuActions();
        Action[] result = new Action[treeActions.length + sep
                + chartActions.length];
        System.arraycopy(treeActions, 0, result, 0, treeActions.length);
        System.arraycopy(chartActions, 0, result, treeActions.length
                + sep, chartActions.length);
        return result;
    }

    @Override
    protected void processLeftButton(MouseEvent e) {
        boolean isMineEvent = true;
        ChartItem itemUnderPoint = myChartImplementation.getChartItemUnderMousePoint(e.getX(), e.getY());
        if (itemUnderPoint instanceof TaskBoundaryChartItem) {
            TaskBoundaryChartItem taskBoundary = (TaskBoundaryChartItem) itemUnderPoint;
            if (taskBoundary.isStartBoundary()) {
                myChartImplementation.beginChangeTaskStartInteraction(e, taskBoundary);
            }
            else {
                myChartImplementation.beginChangeTaskEndInteraction(e, taskBoundary);
            }
        }
        else if (itemUnderPoint instanceof TaskProgressChartItem) {
            myChartImplementation.beginChangeTaskProgressInteraction(
                    e, (TaskProgressChartItem) itemUnderPoint);
        }
        else if (itemUnderPoint instanceof TaskRegularAreaChartItem) {
            myChartImplementation.beginDrawDependencyInteraction(
                    e, (TaskRegularAreaChartItem) itemUnderPoint);
        }
        else {
            isMineEvent = false;
            super.processLeftButton(e);
        }
        if (isMineEvent) {
            myUiFacade.refresh();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        myTree.stopEditing();
        super.mousePressed(e);
        Task taskUnderPointer = myChartImplementation.findTaskUnderPointer(e.getX(), e.getY());
        if (taskUnderPointer == null) {
            return;
        }
        if (taskUnderPointer!=null && !getTaskSelectionManager().isTaskSelected(taskUnderPointer)) {
            boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
            if (!ctrl) {
                getTaskSelectionManager().clear();
            }
            getTaskSelectionManager().addTask(taskUnderPointer);
        }
        if (e.getButton() == MouseEvent.BUTTON2) {
            if (!getTaskSelectionManager().isTaskSelected(taskUnderPointer)) {
                getTaskSelectionManager().clear();
                getTaskSelectionManager().addTask(taskUnderPointer);
            }
            List<Task> l = getTaskSelectionManager().getSelectedTasks();
            myChartImplementation.beginMoveTaskInteractions(e, l);
        }
    }
}