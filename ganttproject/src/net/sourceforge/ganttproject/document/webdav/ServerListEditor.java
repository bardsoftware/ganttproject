package net.sourceforge.ganttproject.document.webdav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

class ServerListEditor {
  private Action myPopupAction = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      onPopupAction();
    }
  };

  private Action myDoneAction = new GPAction("done") {
    public void actionPerformed(ActionEvent arg0) {
      onDoneAction();
    }
  };

  private Action myPlusAction = new GPAction("add", IconSize.SMALL) {
    public void actionPerformed(ActionEvent arg0) {
      myTableModel.addRow(new String[] {"", ""});
      myTable.editCellAt(myTableModel.getRowCount() - 1, 0);
    }
  };

  private Action myMinusAction = new GPAction("delete", IconSize.SMALL) {
    public void actionPerformed(ActionEvent e) {
      myTableModel.removeRow(myTable.getSelectedRow());
    }
  };

  private JButton myButton = new TestGanttRolloverButton(myPopupAction);
  private JPanel myWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
  private ListOption myOption;
  private DefaultTableModel myTableModel;
  private JXTable myTable;

  private JScrollPane myScrollPane;

  private JPanel myResult;

  private JButton myDoneButton;

  private ListSelectionListener mySelectionListener = new ListSelectionListener() {
    public void valueChanged(ListSelectionEvent arg0) {
      int selectedRow = myTable.getSelectedRow();
      if (selectedRow == -1) {
        return;
      }
      myOption.setValue(buildValue(selectedRow));
      myButton.setText(String.valueOf(myTable.getValueAt(selectedRow, 0)));
    }
  };

  private JPopupMenu myPopup;

  public ServerListEditor(ListOption option) {
    myOption = option;
    myButton.setText(option.getValue().split("\\t")[0]);
    myButton.setIcon(new ImageIcon(getClass().getResource("/icons/dropdown_16.png")));
    myButton.setHorizontalTextPosition(SwingConstants.LEADING);
    myButton.setVerticalTextPosition(SwingConstants.CENTER);

    myWrapper.add(myButton);
    myTableModel = new DefaultTableModel(new Vector<String>(Arrays.asList("Name", "URL")), 0);
    for (String value : option.getAvailableValues()) {
      String[] parts = value.split("\\t");
      if (parts.length == 2) {
        myTableModel.addRow(parts);
      }
    }
    myTable = new JXTable(myTableModel);
    myTable.setVisibleRowCount(10);
    myTable.setFillsViewportHeight(true);
    myTable.setPreferredSize(new Dimension(600, 200));
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
      public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
        // TODO Auto-generated method stub
        TableColumn column = myTable.getColumn(0);
        Dimension dimension = UIUtil.autoFitColumnWidth(myTable, column);
        System.err.println("autofit="+dimension);
        column.setWidth(dimension.width);
        column.setPreferredWidth(dimension.width);
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
        applyValues();
      }

      public void popupMenuCanceled(PopupMenuEvent arg0) {
        applyValues();
      }
    });
    myPopup.show(myWrapper, myButton.getLocation().x, myButton.getHeight());
  }

  private void applyValues() {
    List<String> values = Lists.newArrayList();
    for (int i = 0; i < myTableModel.getRowCount(); i++) {
      values.add(buildValue(i));
    }
    myOption.setValues(values);
    mySelectionListener.valueChanged(null);
  }

  private String buildValue(int i) {
    return myTable.getValueAt(i, 0) + "\t" + myTable.getValueAt(i, 1);
  }
}
