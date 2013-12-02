/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;

import com.google.common.collect.Lists;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Panel to edit the text export parameters
 *
 * @author dbarashev@gmail.com Major rewrite.
 * @author athomas
 */
public class CSVSettingsPanel extends GeneralOptionPanel {

  private JComboBox cbTextSeparator;

//  private JCheckBox cbTaskID;
//
//  private JCheckBox cbTaskName;
//
//  private JCheckBox cbStartDate;
//
//  private JCheckBox cbEndDate;
//
//  private JCheckBox cbTaskPercent;
//
//  private JCheckBox cbTaskDuration;
//
//  private JCheckBox cbTaskWebLink;
//
//  private JCheckBox cbTaskResources;
//
//  private JCheckBox cbTaskNotes;

  private JCheckBox cbResID;

  private JCheckBox cbResName;

  private JCheckBox cbResMail;

  private JCheckBox cbResPhone;

  private JCheckBox cbResRole;

  private final CSVOptions myCsvOptions;

  private JComboBox myFieldSeparatorCombo;

  public CSVSettingsPanel(UIFacade uiFacade, CSVOptions csvOptions) {
    super(uiFacade, language.getCorrectedLabel("csvexport"), language.getText("settingsCSVExport"));
    myCsvOptions = csvOptions;

    vb.add(createSeparatorSettingsPanel());
    vb.add(Box.createVerticalStrut(15));
    vb.add(createTaskExportFieldsPanel());
    vb.add(Box.createVerticalStrut(15));
    vb.add(createResourceExportFieldsPanel());
    vb.add(Box.createVerticalGlue());

    applyComponentOrientation(language.getComponentOrientation());
  }

  JComponent createSeparatorSettingsPanel() {
    JPanel result = new JPanel(new SpringLayout());
    result.add(new JLabel(language.getText("textSeparator")));
    cbTextSeparator = new JComboBox(getCsvOptions().getSeparatedTextChars());
    result.add(cbTextSeparator);

    result.add(new JLabel(language.getText("separatedFields")));
    myFieldSeparatorCombo = new JComboBox(new String[] { language.getText("fixedWidth"), language.getText("doubledot"),
        language.getText("dotComa"), language.getText("coma") });
    myFieldSeparatorCombo.setEditable(false);
    result.add(myFieldSeparatorCombo);
    SpringUtilities.makeCompactGrid(result, 2, 2, 0, 0, 3, 3);
    return result;
  }

  JComponent createTaskExportFieldsPanel() {
    JXTable table = new JXTable(createTaskPropertiesTableModel());
    table.setTableHeader(null);
    table.setVisibleRowCount(10);
    JScrollPane scrollPane = new JScrollPane(table);

//    JPanel panel = new JPanel(new SpringLayout());
//    cbTaskID = new JCheckBox(language.getText("id"));
//    panel.add(cbTaskID);
//    cbTaskName = new JCheckBox(language.getText("name"));
//    panel.add(cbTaskName);
//    cbTaskDuration = new JCheckBox(language.getText("length"));
//    panel.add(cbTaskDuration);
//    cbStartDate = new JCheckBox(language.getText("dateOfBegining"));
//    panel.add(cbStartDate);
//    cbEndDate = new JCheckBox(language.getText("dateOfEnd"));
//    panel.add(cbEndDate);
//    cbTaskPercent = new JCheckBox(language.getText("advancement"));
//    panel.add(cbTaskPercent);
//    cbTaskWebLink = new JCheckBox(language.getText("webLink"));
//    panel.add(cbTaskWebLink);
//    cbTaskResources = new JCheckBox(language.getText("resources"));
//    panel.add(cbTaskResources);
//    cbTaskNotes = new JCheckBox(language.getText("notes"));
//    panel.add(cbTaskNotes);
//    SpringUtilities.makeCompactGrid(panel, 3, 3, 0, 0, 3, 3);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(BorderLayout.CENTER, scrollPane);
    UIUtil.createTitle(panel, language.getText("taskFields"));
    return panel;
  }

  private TableModel createTaskPropertiesTableModel() {
    final List<BooleanOption> taskOptions = Lists.newArrayList(getCsvOptions().getTaskOptions().values());
    return new AbstractTableModel() {
      @Override
      public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
          return String.class;
        case 1:
          return Boolean.class;
        }
        return null;
      }

      @Override
      public boolean isCellEditable(int row, int col) {
        return col == 1;
      }

      @Override
      public int getColumnCount() {
        return 2;
      }

      @Override
      public int getRowCount() {
        return taskOptions.size();
      }


      @Override
      public Object getValueAt(int row, int column) {
        if (row >= 0 && row < getRowCount()) {
          switch (column) {
          case 0:
            String id = taskOptions.get(row).getID();
            TaskDefaultColumn taskColumn = TaskDefaultColumn.find(id);
            return taskColumn == null ? GanttLanguage.getInstance().getText(id) : taskColumn.getName();
          case 1:
            return taskOptions.get(row).getValue();
          }
        }
        return null;
      }

      @Override
      public void setValueAt(Object aValue, int row, int column) {
        if (row >= 0 && row < getRowCount()) {
          taskOptions.get(row).setValue((Boolean)aValue);
        }
      }
    };
  }

  JComponent createResourceExportFieldsPanel() {
    JPanel panel = new JPanel(new SpringLayout());
    cbResID = new JCheckBox(language.getText("id"));
    panel.add(cbResID);
    cbResName = new JCheckBox(language.getText("colName"));
    panel.add(cbResName);
    cbResMail = new JCheckBox(language.getText("colMail"));
    panel.add(cbResMail);
    cbResPhone = new JCheckBox(language.getText("colPhone"));
    panel.add(cbResPhone);
    cbResRole = new JCheckBox(language.getText("colRole"));
    panel.add(cbResRole);
    panel.add(new JPanel());

    SpringUtilities.makeCompactGrid(panel, 3, 2, 0, 0, 3, 3);
    UIUtil.createTitle(panel, language.getText("resFields"));

    Box result = Box.createHorizontalBox();
    result.add(panel);
    result.add(Box.createHorizontalGlue());
    return result;

  }

  @Override
  public boolean applyChanges(boolean askForApply) {
    CSVOptions csvOptions = getCsvOptions();
    csvOptions.sSeparatedTextChar = getTextSeparat();
    csvOptions.sSeparatedChar = getSeparat();
    csvOptions.bFixedSize = getFixed();
//    csvOptions.bExportTaskID = getTaskID();
//    csvOptions.bExportTaskName = getTaskName();
//    csvOptions.bExportTaskStartDate = getTaskSD();
//    csvOptions.bExportTaskEndDate = getTaskED();
//    csvOptions.bExportTaskDuration = getTaskDuration();
//    csvOptions.bExportTaskPercent = getTaskPercent();
//    csvOptions.bExportTaskWebLink = getTaskWebLink();
//    csvOptions.bExportTaskResources = getTaskResources();
//    csvOptions.bExportTaskNotes = getTaskNotes();
    csvOptions.bExportResourceID = getResourceID();
    csvOptions.bExportResourceName = getResourceName();
    csvOptions.bExportResourceMail = getResourceMail();
    csvOptions.bExportResourcePhone = getResourcePhone();
    csvOptions.bExportResourceRole = getResourceRole();
    return true;
  }

  @Override
  public void initialize() {
//    cbTaskID.setSelected(getCsvOptions().bExportTaskID);
//    cbTaskName.setSelected(getCsvOptions().bExportTaskName);
//    cbStartDate.setSelected(getCsvOptions().bExportTaskStartDate);
//    cbEndDate.setSelected(getCsvOptions().bExportTaskEndDate);
//    cbTaskPercent.setSelected(getCsvOptions().bExportTaskPercent);
//    cbTaskDuration.setSelected(getCsvOptions().bExportTaskDuration);
//    cbTaskWebLink.setSelected(getCsvOptions().bExportTaskWebLink);
//    cbTaskResources.setSelected(getCsvOptions().bExportTaskResources);
//    cbTaskNotes.setSelected(getCsvOptions().bExportTaskNotes);

    cbResID.setSelected(getCsvOptions().bExportResourceID);
    cbResName.setSelected(getCsvOptions().bExportResourceName);
    cbResMail.setSelected(getCsvOptions().bExportResourceMail);
    cbResPhone.setSelected(getCsvOptions().bExportResourcePhone);
    cbResRole.setSelected(getCsvOptions().bExportResourceRole);

    String selectedSeparator;
    if (getCsvOptions().bFixedSize) {
      selectedSeparator = language.getText("fixedWidth");
    } else {
      String sSeparatedChar = getCsvOptions().sSeparatedChar;

      if (",".equals(sSeparatedChar)) {
        selectedSeparator = language.getText("coma");
      } else if (";".equals(sSeparatedChar)) {
        selectedSeparator = language.getText("dotComa");
      } else if (":".equals(sSeparatedChar)) {
        selectedSeparator = language.getText("doubledot");
      } else {
        selectedSeparator = sSeparatedChar;
      }
    }
    myFieldSeparatorCombo.setSelectedItem(selectedSeparator);
    if ("\"".equals(getCsvOptions().sSeparatedTextChar)) {
      cbTextSeparator.setSelectedIndex(1);
    }
  }

  private boolean getFixed() {
    return language.getText("fixedWidth").equals(myFieldSeparatorCombo.getSelectedItem());
  }

//  private boolean getTaskID() {
//    return cbTaskID.isSelected();
//  }
//
//  private boolean getTaskName() {
//    return cbTaskName.isSelected();
//  }
//
//  private boolean getTaskSD() {
//    return cbStartDate.isSelected();
//  }
//
//  private boolean getTaskED() {
//    return cbEndDate.isSelected();
//  }
//
//  private boolean getTaskPercent() {
//    return cbTaskPercent.isSelected();
//  }
//
//  private boolean getTaskDuration() {
//    return cbTaskDuration.isSelected();
//  }
//
//  private boolean getTaskWebLink() {
//    return cbTaskWebLink.isSelected();
//  }
//
//  private boolean getTaskResources() {
//    return cbTaskResources.isSelected();
//  }
//
//  private boolean getTaskNotes() {
//    return cbTaskNotes.isSelected();
//  }

  private boolean getResourceID() {
    return cbResID.isSelected();
  }

  private boolean getResourceName() {
    return cbResName.isSelected();
  }

  private boolean getResourcePhone() {
    return cbResPhone.isSelected();
  }

  private boolean getResourceMail() {
    return cbResMail.isSelected();
  }

  private boolean getResourceRole() {
    return cbResRole.isSelected();
  }

  private String getTextSeparat() {
    if (cbTextSeparator.getSelectedIndex() == 0) {
      return "\'";
    }
    return "\"";
  }

  private String getSeparat() {
    String selectedSeparator = (String) myFieldSeparatorCombo.getSelectedItem();
    if (selectedSeparator.equals(language.getText("doubledot"))) {
      return ":";
    }
    if (selectedSeparator.equals(language.getText("coma"))) {
      return ",";
    }
    if (selectedSeparator.equals(language.getText("dotComa"))) {
      return ";";
    }
    return selectedSeparator;
  }

  private CSVOptions getCsvOptions() {
    return myCsvOptions;
  }
}
