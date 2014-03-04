/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.BorderLayout;
import java.awt.Component;
import java.math.BigDecimal;

import javax.swing.JPanel;
import javax.swing.JTable;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;

import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultDoubleOption;
import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

/**
 * UI component in a task properties dialog: a table with resources assigned to
 * a task.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskAllocationsPanel {
  private ResourcesTableModel myModel;
  private final HumanResourceManager myHRManager;
  private final RoleManager myRoleManager;
  private final Task myTask;
  private final DefaultBooleanOption myCostIsCalculated = new DefaultBooleanOption("taskProperties.cost.calculated");
  private final DefaultDoubleOption myCostValue = new DefaultDoubleOption("taskProperties.cost.value") {

    @Override
    public void setValue(Double value) {
      // TODO Auto-generated method stub
      super.setValue(value);
    }

  };
  private final GPOptionGroup myCostGroup = new GPOptionGroup("task.cost", myCostIsCalculated, myCostValue);

  private JTable myTable;

  public TaskAllocationsPanel(Task task, HumanResourceManager hrManager, RoleManager roleMgr) {
    myHRManager = hrManager;
    myRoleManager = roleMgr;
    myTask = task;
  }

  private JTable getTable() {
    return myTable;
  }

  public JPanel getComponent() {
    myModel = new ResourcesTableModel(myTask.getAssignmentCollection());
    myTable = new JTable(myModel);
    UIUtil.setupTableUI(getTable());
    CommonPanel.setupComboBoxEditor(getTable().getColumnModel().getColumn(1), myHRManager.getResources().toArray());
    CommonPanel.setupComboBoxEditor(getTable().getColumnModel().getColumn(4), myRoleManager.getEnabledRoles());

    AbstractTableAndActionsComponent<TaskDependency> tableAndActions = new AbstractTableAndActionsComponent<TaskDependency>(
        getTable()) {
      @Override
      protected void onAddEvent() {
        getTable().editCellAt(myModel.getRowCount() - 1, 1);
      }

      @Override
      protected void onDeleteEvent() {
        myModel.delete(getTable().getSelectedRows());
      }

      @Override
      protected void onSelectionChanged() {
      }
    };
    JPanel tablePanel = CommonPanel.createTableAndActions(myTable, tableAndActions.getActionsComponent());
    String layoutDef = "(ROW weight=1.0 (LEAF name=resources weight=0.5) (LEAF name=cost weight=0.5))";

    JXMultiSplitPane result = new JXMultiSplitPane();
    result.setDividerSize(0);

    MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
    result.getMultiSplitLayout().setModel(modelRoot);
    result.add(tablePanel, "resources");
    result.add(createCostPanel(), "cost");
    return result;
  }

  private Component createCostPanel() {
    myCostIsCalculated.setValue(myTask.getCost().isCalculated());
    myCostValue.setValue(myTask.getCost().getValue().doubleValue());
    OptionsPageBuilder builder = new OptionsPageBuilder();
    return builder.createGroupComponent(myCostGroup);
  }

  public void commit() {
    if (myTable.isEditing()) {
      myTable.getCellEditor().stopCellEditing();
    }
    myModel.commit();
    Task.Cost cost = myTask.getCost();
    cost.setCalculated(myCostIsCalculated.getValue());
    if (!cost.isCalculated()) {
      cost.setValue(BigDecimal.valueOf(myCostValue.getValue()));
    }
  }
}
