/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document.webdav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.GPAction.IconSize;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.ListOption;

/**
 * Editor for WebDAV server list in WebDAV open/save dialog
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ServerListEditor {
  private Action myPopupAction = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      onPopupAction();
    }
  };

  private Action myDoneAction = new GPAction("done") {
    @Override
    public void actionPerformed(ActionEvent arg0) {
      onDoneAction();
    }
  };

  private Action myPlusAction = new GPAction("add", IconSize.SMALL) {
    @Override
    public void actionPerformed(ActionEvent arg0) {
      myOption.addValue(new WebDavServerDescriptor("", "", ""));
      myTableModel.addRow(new String[] {"", ""});
      myTable.editCellAt(myTableModel.getRowCount() - 1, 0);
    }
  };

  private Action myMinusAction = new GPAction("delete", IconSize.SMALL) {
    @Override
    public void actionPerformed(ActionEvent e) {
      myOption.removeValueIndex(myTable.getSelectedRow());
      myTableModel.removeRow(myTable.getSelectedRow());
    }
  };

  private JButton myButton = new TestGanttRolloverButton(myPopupAction);
  private JPanel myWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
  private ListOption<WebDavServerDescriptor> myOption;
  private DefaultTableModel myTableModel;
  private JXTable myTable;

  private JScrollPane myScrollPane;

  private JPanel myResult;

  private JButton myDoneButton;

  private ListSelectionListener mySelectionListener = new ListSelectionListener() {
    @Override
    public void valueChanged(ListSelectionEvent arg0) {
      int selectedRow = myTable.getSelectedRow();
      if (selectedRow == -1) {
        return;
      }
      myOption.setValueIndex(selectedRow);
      myButton.setText(String.valueOf(myTable.getValueAt(selectedRow, 0)));
    }
  };

  private JPopupMenu myPopup;

  public ServerListEditor(ListOption<WebDavServerDescriptor> option) {
    myOption = option;
    myButton.setText(option.getValue().name);
    myButton.setIcon(new ImageIcon(getClass().getResource("/icons/dropdown_16.png")));
    myButton.setHorizontalTextPosition(SwingConstants.LEADING);
    myButton.setVerticalTextPosition(SwingConstants.CENTER);

    myWrapper.add(myButton);

    myTableModel = new DefaultTableModel(new Vector<String>(Arrays.asList("Name", "URL")), 0) {
      @Override
      public void setValueAt(Object aValue, int row, int column) {
        super.setValueAt(aValue, row, column);

        String editValue = String.valueOf(aValue);
        List<WebDavServerDescriptor> values = Lists.newArrayList(myOption.getValues());
        WebDavServerDescriptor descr = values.get(row);
        switch (column) {
        case 0:
          descr.name = editValue;
          break;
        case 1:
          descr.rootUrl = editValue;
          break;
        default:
          assert false : "should not be here";
        }
      }

    };
    for (WebDavServerDescriptor value : option.getValues()) {
      myTableModel.addRow(new String[] {value.name, value.rootUrl});
    }
    myTable = new JXTable(myTableModel);
    myTable.setVisibleRowCount(10);
    myTable.setFillsViewportHeight(true);
    myTable.setPreferredSize(new Dimension(600, 200));
    myTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          myDoneAction.actionPerformed(null);
        }
      }
    });
    myScrollPane = new JScrollPane(myTable);
    myDoneButton = new JButton(myDoneAction);
    myResult = new JPanel(new BorderLayout());
    myResult.add(myScrollPane, BorderLayout.CENTER);

    JButton plusButton = new JButton(myPlusAction);
    plusButton.setText("");

    JButton minusButton = new JButton(myMinusAction);
    minusButton.setText("");

    JButton[] leftButtons = new JButton[] {plusButton, minusButton};
    JButton[] rightButtons = new JButton[] {myDoneButton};

    JComponent buttonBar = UIUtil.createButtonBar(leftButtons, rightButtons);
    buttonBar.setBorder(BorderFactory.createEmptyBorder(5, 3, 3, 3));
    myResult.add(buttonBar, BorderLayout.SOUTH);
    myResult.setPreferredSize(new Dimension(600, 250));

    myTable.getSelectionModel().addListSelectionListener(mySelectionListener);
    DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField()) {
      @Override
      public boolean isCellEditable(EventObject evt) {
        if (evt instanceof MouseEvent && ((MouseEvent)evt).getClickCount() >= 2) {
          return false;
        }
        return super.isCellEditable(evt);
      }

    };
    myTable.getColumn(0).setCellEditor(cellEditor);
    myTable.getColumn(1).setCellEditor(cellEditor);
  }

  JComponent getComponent() {
    return myWrapper;
  }


  protected void onDoneAction() {
    myPopup.setVisible(false);
    applyValues();
  }

  protected void onPopupAction() {
    myPopup = new JPopupMenu();
    myPopup.add(myResult);
    myPopup.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
        TableColumn column = myTable.getColumn(0);
        Dimension dimension = UIUtil.autoFitColumnWidth(myTable, column);
        column.setWidth(dimension.width);
        column.setPreferredWidth(dimension.width);
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
        applyValues();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent arg0) {
        applyValues();
      }
    });
    myPopup.show(myWrapper, myButton.getLocation().x, myButton.getHeight());
  }

  private void applyValues() {
    mySelectionListener.valueChanged(null);
  }

  private String buildValue(int i) {
    return myTable.getValueAt(i, 0) + "\t" + myTable.getValueAt(i, 1);
  }
}
