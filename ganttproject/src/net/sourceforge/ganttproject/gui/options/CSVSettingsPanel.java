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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.jdesktop.swingx.JXTable;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;

import com.google.common.collect.Lists;

/**
 * Panel to edit the text export parameters
 *
 * @author dbarashev@gmail.com Major rewrite.
 * @author athomas
 */
public class CSVSettingsPanel extends GeneralOptionPanel {

  private JComboBox cbTextSeparator;
  private final CSVOptions myCsvOptions;

  private JComboBox myFieldSeparatorCombo;

  public CSVSettingsPanel(UIFacade uiFacade, CSVOptions csvOptions) {
    super(uiFacade, language.getCorrectedLabel("csvexport"), language.getText("settingsCSVExport"));
    myCsvOptions = csvOptions;

    vb.add(createSeparatorSettingsPanel());
    vb.add(Box.createVerticalStrut(15));
    vb.add(createTaskExportFieldsPanel(createTaskPropertiesTableModel(), "taskFields"));
    vb.add(Box.createVerticalStrut(15));
    vb.add(createTaskExportFieldsPanel(createResourcePropertiesTableModel(), "resFields"));

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

  JComponent createTaskExportFieldsPanel(TableModel tableModel, String id) {
    JXTable table = new JXTable(tableModel);
    table.setTableHeader(null);
    table.setVisibleRowCount(10);
    JScrollPane scrollPane = new JScrollPane(table);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(BorderLayout.CENTER, scrollPane);
    UIUtil.createTitle(panel, language.getText(id));
    return panel;
  }

  private static abstract class ExportFieldsTableModel extends AbstractTableModel {
    private final List<BooleanOption> myOptions;

    ExportFieldsTableModel(List<BooleanOption> options) {
      myOptions = options;
    }
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
      return myOptions.size();
    }


    @Override
    public Object getValueAt(int row, int column) {
      if (row >= 0 && row < getRowCount()) {
        switch (column) {
        case 0:
          String id = myOptions.get(row).getID();
          return getOptionName(id);
        case 1:
          return myOptions.get(row).getValue();
        }
      }
      return null;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
      if (row >= 0 && row < getRowCount()) {
        myOptions.get(row).setValue((Boolean)aValue);
      }
    }

    protected abstract String getOptionName(String id);
  }

  private TableModel createTaskPropertiesTableModel() {
    final List<BooleanOption> taskOptions = Lists.newArrayList(getCsvOptions().getTaskOptions().values());
    return new ExportFieldsTableModel(taskOptions) {
      @Override
      protected String getOptionName(String id) {
        TaskDefaultColumn taskColumn = TaskDefaultColumn.find(id);
        return taskColumn == null ? GanttLanguage.getInstance().getText(id) : taskColumn.getName();
      }
    };
  }

  private TableModel createResourcePropertiesTableModel() {
    return new ExportFieldsTableModel(Lists.newArrayList(getCsvOptions().getResourceOptions().values())) {
      @Override
      protected String getOptionName(String id) {
        ResourceDefaultColumn column = ResourceDefaultColumn.find(id);
        return column == null ? GanttLanguage.getInstance().getText(id) : column.getName();
      }
    };
  }

  @Override
  public boolean applyChanges(boolean askForApply) {
    CSVOptions csvOptions = getCsvOptions();
    csvOptions.sSeparatedTextChar = getTextSeparat();
    csvOptions.sSeparatedChar = getSeparat();
    csvOptions.bFixedSize = getFixed();
    return true;
  }

  @Override
  public void initialize() {
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
