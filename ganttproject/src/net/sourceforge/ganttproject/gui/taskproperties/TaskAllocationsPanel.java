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

import biz.ganttproject.core.option.*;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.BooleanOptionRadioUi;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

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
  private final DefaultBooleanOption myCost2IsCalculated = new DefaultBooleanOption("taskProperties.cost2.calculated");
  private final DefaultDoubleOption myCost2Value = new DefaultDoubleOption("taskProperties.cost2.value") {

    @Override
    public void setValue(Double value) {
      // TODO Auto-generated method stub
      super.setValue(value);
    }

  };
  private final GPOptionGroup myCostGroup = new GPOptionGroup("task.cost", myCostIsCalculated, myCostValue);
  private final GPOptionGroup myCost2Group = new GPOptionGroup("task.cost2", myCost2IsCalculated, myCost2Value);

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

    JPanel tablePanel = CommonPanel.createTableAndActions(myTable, myModel);
    String layoutDef = "(ROW weight=1.0 (LEAF name=resources weight=0.5) (LEAF name=cost weight=0.5))";

    JXMultiSplitPane result = new JXMultiSplitPane();
    result.setDividerSize(0);

    MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
    result.getMultiSplitLayout().setModel(modelRoot);
    result.add(tablePanel, "resources");
    result.add(UIUtil.border(createCostPanel(), 10, UIUtil.LEFT), "cost");
    return result;
  }

  private JComponent createCostPanel() {
    myCostIsCalculated.setValue(myTask.getCost().isCalculated());
    myCost2IsCalculated.setValue(myTask.getCost2().isCalculated());
    myCostIsCalculated.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        myCostValue.setWritable(!myCostIsCalculated.isChecked());
        myCostValue.setValue(myTask.getCost().getValue().doubleValue());
      }
    });
    myCost2IsCalculated.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        myCost2Value.setWritable(!myCost2IsCalculated.isChecked());
        myCost2Value.setValue(myTask.getCost2().getValue().doubleValue());
      }
    });
    myCostValue.setValue(myTask.getCost().getValue().doubleValue());
    myCost2Value.setValue(myTask.getCost2().getValue().doubleValue());
    myCostValue.setWritable(!myCostIsCalculated.isChecked());
    myCost2Value.setWritable(!myCost2IsCalculated.isChecked());

    OptionsPageBuilder builder = new OptionsPageBuilder();
    BooleanOptionRadioUi radioUi = OptionsPageBuilder.createBooleanOptionRadioUi(myCostIsCalculated);
    BooleanOptionRadioUi radioUi2 = OptionsPageBuilder.createBooleanOptionRadioUi(myCost2IsCalculated);

    JPanel optionsPanel = new JPanel();
    optionsPanel.add(radioUi.getYesButton());
    optionsPanel.add(new JLabel(myTask.getCost().getCalculatedValue().toPlainString()));
    optionsPanel.add(radioUi.getNoButton());
    optionsPanel.add(builder.createOptionComponent(myCostGroup, myCostValue));
    optionsPanel.add(radioUi2.getYesButton());
    optionsPanel.add(new JLabel(myTask.getCost2().getCalculatedValue().toPlainString()));
    optionsPanel.add(radioUi2.getNoButton());
    optionsPanel.add(builder.createOptionComponent(myCost2Group, myCost2Value));
    OptionsPageBuilder.TWO_COLUMN_LAYOUT.layout(optionsPanel, 4);

    final String yesLabelKey = builder.getI18N().getCanonicalOptionLabelKey(myCostIsCalculated) + ".yes";
    radioUi.getYesButton().setText(GanttLanguage.getInstance().getText(yesLabelKey));
    radioUi.getNoButton().setText(GanttLanguage.getInstance().getText(builder.getI18N().getCanonicalOptionLabelKey(myCostIsCalculated) + ".no"));
    radioUi2.getYesButton().setText(GanttLanguage.getInstance().getText(yesLabelKey));
    radioUi2.getNoButton().setText(GanttLanguage.getInstance().getText(builder.getI18N().getCanonicalOptionLabelKey(myCost2IsCalculated) + ".no"));
    UIUtil.createTitle(optionsPanel, builder.getI18N().getOptionGroupLabel(myCostGroup));
    UIUtil.createTitle(optionsPanel, builder.getI18N().getOptionGroupLabel(myCost2Group));

    JPanel result = new JPanel(new BorderLayout());
    result.add(optionsPanel, BorderLayout.NORTH);
    return result;
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
    Task.Cost cost2 = myTask.getCost2();
    cost2.setCalculated(myCost2IsCalculated.getValue());
    if (!cost2.isCalculated()) {
      cost2.setValue(BigDecimal.valueOf(myCost2Value.getValue()));
    }
  }
}
