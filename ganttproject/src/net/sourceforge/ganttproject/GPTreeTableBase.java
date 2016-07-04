/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.table.NumberEditorExt;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public abstract class GPTreeTableBase extends JXTreeTable implements CustomPropertyListener {
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;
  private final TableHeaderUiFacadeImpl myTableHeaderFacade = new TableHeaderUiFacadeImpl();
  private final CustomPropertyManager myCustomPropertyManager;
  private final JScrollPane myScrollPane = new JScrollPane() {
    @Override
    public void applyComponentOrientation(ComponentOrientation o) {
      super.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
  };

  private GPAction myEditCellAction = new GPAction("tree.edit") {
    @Override
    public void actionPerformed(ActionEvent e) {
      JTable t = getTable();
      if (t.getSelectedRow() < 0) {
        return;
      }
      if (t.getSelectedColumn() < 0) {
        t.getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
      }
      editCellAt(t.getSelectedRow(), t.getSelectedColumn());
    }
  };
  private final Runnable myUpdateUiCommand = new Runnable() {
    @Override
    public void run() {
      updateUI();
    }
  };

  @Override
  public Component prepareEditor(TableCellEditor editor, int row, int column) {
    Component result = super.prepareEditor(editor, row, column);
    if (result instanceof JTextComponent) {
      final Runnable command = TreeTableCellEditorImpl.createSelectAllCommand((JTextComponent) result);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          command.run();
        }
      });
    }
    return result;
  }


  @Override
  public void editingCanceled(ChangeEvent e) {
    super.editingCanceled(e);
    SwingUtilities.invokeLater(myUpdateUiCommand);
  }

  @Override
  public void editingStopped(ChangeEvent arg0) {
    super.editingStopped(arg0);
    SwingUtilities.invokeLater(myUpdateUiCommand);
  }

  protected class TableHeaderUiFacadeImpl implements ColumnList {
    private final List<Column> myDefaultColumnStubs = new ArrayList<Column>();
    private final List<ColumnImpl> myColumns = new ArrayList<ColumnImpl>();

    TableHeaderUiFacadeImpl() {
      GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
        @Override
        public void languageChanged(Event event) {
          for (ColumnImpl column : myColumns) {
            column.setName(column.getName());
          }
        }
      });
    }

    private List<ColumnImpl> getColumns() {
      return Collections.unmodifiableList(myColumns);
    }

    @Override
    public int getSize() {
      return myColumns.size();
    }

    @Override
    public Column getField(int index) {
      return myColumns.get(index);
    }

    @Override
    public void clear() {
      clearUiColumns();
      myColumns.clear();
      for (int i = 0; i < myDefaultColumnStubs.size(); i++) {
        Column stub = myDefaultColumnStubs.get(i);
        ColumnImpl column = createColumn(i, stub);
        if (stub.isVisible()) {
          // keep some columns visible in the table when creating a new project
          // otherwise table appears without any columns at all and as a side effect,
          // chart timeline may change its height
          insertColumnIntoUi(column);
        }
      }
    }

    private void clearUiColumns() {
      List<TableColumn> columns = Collections.list(getTable().getColumnModel().getColumns());
      for (int i = 0; i < columns.size(); i++) {
        getTable().removeColumn(columns.get(i));
      }
    }

    @Override
    public void add(String id, int order, int width) {
      ColumnImpl column = findColumnByID(id);
      if (column == null) {
        CustomPropertyDefinition def = myCustomPropertyManager.getCustomPropertyDefinition(id);
        if (def == null) {
          return;
        }
        if (order == -1) {
          order = getTable().getColumnCount();
        }
        if (width == -1) {
          width = 75;
        }
        ColumnStub columnStub = new ColumnList.ColumnStub(id, def.getName(), true, order, width);
        column = createColumn(getSize(), columnStub);
      }
      if (column == null) {
        return;
      }
      insertColumnIntoUi(column);
    }

    private boolean importColumnList(ColumnList columns) {
      boolean anyVisible = false;
      for (int i = 0; i < columns.getSize(); i++) {
        Column foreign = columns.getField(i);
        ColumnImpl mine = findColumnByID(foreign.getID());
        if (mine == null) {
          int modelIndex = getModelIndex(foreign);
          if (modelIndex >= 0) {
            mine = createColumn(modelIndex, foreign);
          }
        } else {
          mine.getStub().setOrder(foreign.getOrder());
          mine.getStub().setVisible(foreign.isVisible());
          mine.getStub().setWidth(foreign.getWidth());
          anyVisible = foreign.isVisible();
        }
      }
      return anyVisible;
    }
    @Override
    public void importData(ColumnList source) {
      for (ColumnImpl column : myColumns) {
        column.getStub().setVisible(false);
      }
      if (!importColumnList(source)) {
        importColumnList(ColumnList.Immutable.fromList(myDefaultColumnStubs));
      }
      Collections.sort(myColumns, new Comparator<ColumnImpl>() {
        @Override
        public int compare(ColumnImpl left, ColumnImpl right) {
          int test1 = (left.getStub().isVisible() ? -1 : 0) + (right.getStub().isVisible() ? 1 : 0);
          if (test1 != 0) {
            return test1;
          }
          if (!left.getStub().isVisible() && !right.getStub().isVisible()) {
            return left.getName().compareTo(right.getName());
          }
          return left.getStub().getOrder() - right.getStub().getOrder();
        }
      });
      clearUiColumns();
      for (ColumnImpl column : myColumns) {
        if (column.getStub().isVisible()) {
          insertColumnIntoUi(column);
        }
      }
    }

    private int getModelIndex(Column c) {
      for (int i = 0; i < myDefaultColumnStubs.size(); i++) {
        if (c.getID().equals(myDefaultColumnStubs.get(i).getID())) {
          return i;
        }
      }
      List<CustomPropertyDefinition> definitions = myCustomPropertyManager.getDefinitions();
      for (int i = 0; i < definitions.size(); i++) {
        if (definitions.get(i).getID().equals(c.getID())) {
          return myDefaultColumnStubs.size() + i;
        }
      }
      return -1;
    }

    protected void createDefaultColumns(List<ColumnList.Column> stubs) {
      myDefaultColumnStubs.clear();
      for (Column stub : stubs) {
        myDefaultColumnStubs.add(new ColumnList.ColumnStub(stub.getID(), stub.getName(), stub.isVisible(),
            stub.getOrder(), stub.getWidth()));
      }
    }

    protected ColumnImpl createColumn(int modelIndex, ColumnList.Column stub) {
      TableColumnExt tableColumn = newTableColumnExt(modelIndex);
      tableColumn.setPreferredWidth(stub.getWidth());
      tableColumn.setIdentifier(stub.getID());
      ColumnImpl result = new ColumnImpl(getTreeTable(), tableColumn, stub);
      myColumns.add(result);
      return result;
    }

    protected void insertColumnIntoUi(ColumnImpl column) {
      getTable().addColumn(column.myTableColumn);
      column.setWidth(column.getStub().getWidth());
    }

    protected void renameColumn(CustomPropertyDefinition definition) {
      ColumnImpl c = findColumnByID(definition.getID());
      if (c == null) {
        return;
      }
      c.setName(definition.getName());
    }

    protected void updateType(CustomPropertyDefinition def) {
      ColumnImpl c = findColumnByID(def.getID());
      if (c == null) {
        return;
      }
      c.getTableColumnExt().setCellRenderer(createCellRenderer(def.getType()));
      c.getTableColumnExt().setCellEditor(createCellEditor(def.getType()));
    }

    protected void deleteColumn(CustomPropertyDefinition definition) {
      ColumnImpl c = findColumnByID(definition.getID());
      if (c == null) {
        return;
      }
      getTable().removeColumn(c.myTableColumn);
      myColumns.remove(c);
      for (ColumnImpl column : myColumns) {
        if (column.myTableColumn.getModelIndex() > c.myTableColumn.getModelIndex()) {
          column.myTableColumn.setModelIndex(column.myTableColumn.getModelIndex() - 1);
        }
      }
    }

    protected ColumnImpl findColumnByID(String id) {
      for (ColumnImpl c : myColumns) {
        if (c.getID().equals(id)) {
          return c;
        }
      }
      return null;
    }

    protected ColumnImpl findColumnByViewIndex(int index) {
      for (ColumnImpl c : myColumns) {
        if (c.getOrder() == index) {
          return c;
        }
      }
      return null;
    }

  }

  protected static class ColumnImpl implements ColumnList.Column {
    private final JXTreeTable myTable;
    private final TableColumnExt myTableColumn;
    private final Column myStub;

    protected ColumnImpl(JXTreeTable table, TableColumnExt tableColumn, ColumnList.Column stub) {
      myTable = table;
      myTableColumn = tableColumn;
      myStub = stub;
    }

    private TreeTableModel getTableModel() {
      return myTable.getTreeTableModel();
    }

    @Override
    public String getID() {
      return myStub.getID();
    }

    @Override
    public String getName() {
      return getTableModel().getColumnName(myTableColumn.getModelIndex());
    }

    private void setName(String name) {
      myTableColumn.setTitle(name);
    }

    @Override
    public int getOrder() {
      return myTable.convertColumnIndexToView(myTableColumn.getModelIndex());
    }

    @Override
    public int getWidth() {
      return myTableColumn.getWidth();
    }

    @Override
    public boolean isVisible() {
      return getOrder() >= 0;
    }

    @Override
    public void setVisible(boolean visible) {
      if (visible && !isVisible()) {
        myTable.addColumn(myTableColumn);
      } else if (!visible && isVisible()) {
        myTable.getColumnModel().removeColumn(myTableColumn);
      }
    }

    @Override
    public void setWidth(int width) {
      myTableColumn.setWidth(width);
      myTableColumn.setPreferredWidth(width);
    }

    Column getStub() {
      return myStub;
    }

    protected TableColumnExt getTableColumnExt() {
      return myTableColumn;
    }

    @Override
    public void setOrder(int order) {
    }

    Dimension getHeaderFitDimension() {
      return UIUtil.getHeaderDimension(myTable, myTableColumn);
    }

  }

  protected IGanttProject getProject() {
    return myProject;
  }

  @Override
  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
    if (e.isAltDown() || e.isControlDown()) {
      putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
    }
    boolean result = super.processKeyBinding(ks, e, condition, pressed);
    putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
    return result;
  }

  @Override
  public void applyComponentOrientation(ComponentOrientation o) {
    super.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    try {
      return super.getToolTipText(e);
    } catch (NullPointerException ex) {
      return null;
    }
  }

  protected GPTreeTableBase(IGanttProject project, UIFacade uiFacade, CustomPropertyManager customPropertyManager,
      DefaultTreeTableModel model) {
    super(model);
    setTableHeader(new JTableHeader(getColumnModel()) {
      @Override
      public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
      }
    });
    myCustomPropertyManager = customPropertyManager;
    myUiFacade = uiFacade;
    myProject = project;
    myProject.addProjectEventListener(new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        getTableHeaderUiFacade().clear();
      }

      @Override
      public void projectOpened() {
        onProjectOpened();
      }

      @Override
      public void projectCreated() {
        onProjectCreated();
      }
    });
//    JXTree.DelegatingRenderer treeCellRenderer = (DelegatingRenderer) getTreeCellRenderer();
//    treeCellRenderer.setDelegateRenderer(new DefaultTreeCellRenderer() {
//      @Override
//      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
//          boolean leaf, int row, boolean hasFocus) {
//        JLabel result = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
//        result.setIcon(new ImageIcon(getClass().getResource("/icons/alert1_16.gif")));
//        return result;
//      }
//    });
  }

  @Override
  protected void createDefaultEditors() {
    super.createDefaultEditors();

    defaultEditorsByColumnClass.put(Object.class, new GenericEditor(){
      @Override
      public boolean stopCellEditing() {
        try {
          return super.stopCellEditing();
        } catch (ValidationException e) {
          getComponent().setBackground(UIUtil.INVALID_VALUE_BACKGROUND);
          return false;
        }
      }
    });
    defaultEditorsByColumnClass.put(Number.class, new NumberEditorExt(true) {
      @Override
      public boolean stopCellEditing() {
        try {
          return super.stopCellEditing();
        } catch (ValidationException e) {
          getComponent().setBackground(UIUtil.INVALID_VALUE_BACKGROUND);
          return false;
        }
      }
    });

  }

  protected void onProjectOpened() {
  }

  protected void onProjectCreated() {
    getTableHeaderUiFacade().createDefaultColumns(getDefaultColumns());
    getTableHeaderUiFacade().importData(ColumnList.Immutable.fromList(getDefaultColumns()));
  }

  protected void initTreeTable() {
    doInit();
  }

  protected void doInit() {
    setRootVisible(false);
    myCustomPropertyManager.addListener(this);

    getTable().getTableHeader().addMouseListener(new HeaderMouseListener(myCustomPropertyManager));
    getTable().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      @Override
      public void columnMoved(TableColumnModelEvent e) {
        if (e.getFromIndex() != e.getToIndex()) {
          myProject.setModified();
        }
      }

      @Override
      public void columnAdded(TableColumnModelEvent e) {
        myProject.setModified();
      }

      @Override
      public void columnRemoved(TableColumnModelEvent e) {
        myProject.setModified();
      }

      @Override
      public void columnMarginChanged(ChangeEvent e) {
        myProject.setModified();
      }

      @Override
      public void columnSelectionChanged(ListSelectionEvent e) {
      }
    });
    getTable().setAutoCreateColumnsFromModel(false);
    getTable().setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/simple_task.gif"));
    setOpenIcon(icon);
    setClosedIcon(icon);
    setCollapsedIcon(new ImageIcon(getClass().getResource("/icons/plus.gif")));
    setExpandedIcon(new ImageIcon(getClass().getResource("/icons/minus.gif")));
    setLeafIcon(icon);
    addActionWithAccelleratorKey(myEditCellAction);

    setHighlighters(UIUtil.ZEBRA_HIGHLIGHTER);

    getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onCellSelectionChanged();
      }
    });
    getTable().getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onCellSelectionChanged();
      }
    });
    getTree().addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent arg0) {
        getChart().reset();
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent arg0) {
        getChart().reset();
      }
    });
    getTableHeaderUiFacade().importData(ColumnList.Immutable.fromList(getDefaultColumns()));

    // getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    getTable().setFillsViewportHeight(true);
  }

  protected void onCellSelectionChanged() {
    if (!getTable().isEditing()) {
      int row = getTable().getSelectedRow();
      int col = getTable().getSelectedColumn();
      Rectangle rect = getTable().getCellRect(row, col, true);
      getScrollPane().scrollRectToVisible(rect);
    }
  }

  private void addNewCustomColumn(CustomColumn customColumn) {
    ColumnList.Column stub = new ColumnList.ColumnStub(customColumn.getId(), customColumn.getName(),
        false, getTable().getColumnCount(), 100);
    getTableHeaderUiFacade().createColumn(getTable().getModel().getColumnCount() - 1, stub);
  }

  private void deleteCustomColumn(CustomColumn column) {
    getTableHeaderUiFacade().deleteColumn(column);
  }

  @Override
  public void customPropertyChange(CustomPropertyEvent event) {
    switch (event.getType()) {
    case CustomPropertyEvent.EVENT_ADD:
      addNewCustomColumn((CustomColumn) event.getDefinition());
      break;
    case CustomPropertyEvent.EVENT_REMOVE:
      deleteCustomColumn((CustomColumn) event.getDefinition());
      break;
    case CustomPropertyEvent.EVENT_NAME_CHANGE:
      getTableHeaderUiFacade().renameColumn(event.getDefinition());
      getTable().getTableHeader().repaint();
      break;
    case CustomPropertyEvent.EVENT_TYPE_CHANGE:
      getTableHeaderUiFacade().updateType(event.getDefinition());
      getTable().repaint();
    }
  }

  public ColumnList getVisibleFields() {
    return getTableHeaderUiFacade();
  }

  protected TableHeaderUiFacadeImpl getTableHeaderUiFacade() {
    return myTableHeaderFacade;
  }

  protected List<ColumnList.Column> getDefaultColumns() {
    return Collections.emptyList();
  }

  protected abstract Chart getChart();

  protected TableColumnExt newTableColumnExt(int modelIndex) {
    TableColumnExt result = new TableColumnExt(modelIndex);
    Class<?> columnClass = getTreeTableModel().getColumnClass(modelIndex);
    TableCellRenderer renderer = createCellRenderer(columnClass);
    if (renderer != null) {
      result.setCellRenderer(renderer);
    }
    TableCellEditor editor = createCellEditor(columnClass);
    if (editor != null) {
      result.setCellEditor(editor);
    } else {
      System.err.println("no editor for column=" + modelIndex + " class=" + columnClass);
    }
    return result;
  }

  TableCellRenderer createCellRenderer(Class<?> columnClass) {

    // TODO(dbarashev): make sure that icon and boolean values render fine
    // if (Icon.class.equals(columnClass) || Boolean.class.equals(columnClass))
    // {
    // renderer = TableCellRenderers.getNewDefaultRenderer(columnClass);
    //
    // }
    return getTreeTable().getDefaultRenderer(columnClass);
  }

  TableCellEditor createCellEditor(Class<?> columnClass) {
    TableCellEditor editor = columnClass.equals(GregorianCalendar.class) ? UIUtil.newDateCellEditor(myProject, false)
        : getTreeTable().getDefaultEditor(columnClass);
    return editor == null ? null : wrapEditor(editor);
  }

  private TableCellEditor wrapEditor(TableCellEditor editor) {
    return new TreeTableCellEditorImpl((DefaultCellEditor) editor, getTable());
  }

  public JXTreeTable getTree() {
    return this;
  }

  public JXTreeTable getTreeTable() {
    return this;
  }

  public JTable getTable() {
    return this;
  }

  JScrollBar getVerticalScrollBar() {
    return getScrollPane().getVerticalScrollBar();
  }

  JScrollBar getHorizontalScrollBar() {
    return getScrollPane().createHorizontalScrollBar();
  }

  protected JScrollPane getScrollPane() {
    return myScrollPane;
  }

  @Override
  public void addMouseListener(MouseListener mouseListener) {
    super.addMouseListener(mouseListener);
    // this.getTreeTable().getParent().addMouseListener(mouseListener);
  }

  @Override
  public void addKeyListener(KeyListener keyListener) {
    super.addKeyListener(keyListener);
    // getTable().addKeyListener(keyListener);
    // getTree().addKeyListener(keyListener);
  }

  protected class VscrollAdjustmentListener implements AdjustmentListener, TimelineChart.VScrollController {
    private final boolean isMod;
    private final TimelineChart myChart;

    protected VscrollAdjustmentListener(TimelineChart chart, boolean calculateMod) {
      isMod = calculateMod;
      myChart = chart;
    }

    protected TimelineChart getChart() {
      return myChart;
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      if (getChart() == null) {
        return;
      }
      if (isMod) {
        getChart().getModel().setVerticalOffset(e.getValue() % getTreeTable().getRowHeight());
      } else {
        getChart().getModel().setVerticalOffset(e.getValue());
      }
      getChart().reset();
    }

    @Override
    public boolean isScrollable() {
      return getVerticalScrollBar().getMaximum() - getVerticalScrollBar().getMinimum() > 0;
    }

    @Override
    public void scrollBy(int pixels) {
      getVerticalScrollBar().setValue(getVerticalScrollBar().getValue() + pixels);
    }
  }

  void insertWithLeftyScrollBar(JComponent container) {
    getScrollPane().getViewport().add(getTable());
    container.add(getScrollPane(), BorderLayout.CENTER);

  }

  /** Adds keyStroke to the given action (if action is null nothing happens) */
  void addAction(Action action, KeyStroke keyStroke) {
    if (action != null) {
      InputMap inputMap = getInputMap();
      inputMap.put(keyStroke, action.getValue(Action.NAME));
      getActionMap().put(action.getValue(Action.NAME), action);
    }
  }

  /** Adds an action to the object and makes it active */
  public void addActionWithAccelleratorKey(GPAction action) {
    if (action != null) {
      for (KeyStroke ks : GPAction.getAllKeyStrokes(action.getID())) {
        addAction(action, ks);
      }
    }
  }

  void setupActionMaps(GPAction... actions) {
    for (GPAction action : actions) {
      addActionWithAccelleratorKey(action);
    }
  }

  private class HeaderMouseListener extends MouseAdapter {
    private final CustomPropertyManager myCustomPropertyManager;
    private final LinkedList<Column> myRecentlyHiddenColumns = new LinkedList<Column>();

    public HeaderMouseListener(CustomPropertyManager customPropertyManager) {
      super();
      myCustomPropertyManager = customPropertyManager;
    }

    /**
     * @inheritDoc Shows the popupMenu to hide/show columns and to add custom
     *             columns.
     */
    @Override
    public void mousePressed(MouseEvent e) {
      handlePopupTrigger(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      handlePopupTrigger(e);
    }

    private void handlePopupTrigger(MouseEvent e) {
      if (e.isPopupTrigger()) {
        Collection<Action> actions = createPopupActions(e);
        myUiFacade.showPopupMenu(e.getComponent(), actions, e.getX(), e.getY());
      }
    }

    private Collection<Action> createPopupActions(final MouseEvent mouseEvent) {
      List<Action> result = new ArrayList<Action>();
      final int columnAtPoint = getTable().columnAtPoint(mouseEvent.getPoint());
      final ColumnImpl column = getTableHeaderUiFacade().findColumnByViewIndex(columnAtPoint);

      {
        result.add(new GPAction("columns.manage.label") {
          @Override
          public void actionPerformed(ActionEvent e) {
            ShowHideColumnsDialog dialog = new ShowHideColumnsDialog(myUiFacade, myTableHeaderFacade,
                myCustomPropertyManager);
            dialog.show();
          }
        });
      }
      {
        GPAction fitAction = new GPAction("columns.fit.label") {
          @Override
          public void actionPerformed(ActionEvent e) {
            autoFitColumnWidth(column);
          }
        };
        result.add(fitAction);
        fitAction.putValue(Action.NAME, GanttLanguage.getInstance().formatText("columns.fit.label", column.getName()));

      }
      result.add(null);
      {
        GPAction hideAction = new GPAction("columns.hide.label") {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            assert column.isVisible() : "how come it is at mouse click point?";
            column.setVisible(false);
            myRecentlyHiddenColumns.add(column);
          }
        };
        if (columnAtPoint == -1) {
          hideAction.setEnabled(false);
        } else {
          hideAction.putValue(Action.NAME,
              GanttLanguage.getInstance().formatText("columns.hide.label", column.getName()));
        }
        result.add(hideAction);
      }
      if (!myRecentlyHiddenColumns.isEmpty()) {
        List<GPAction> showActions = new ArrayList<GPAction>();
        for (ListIterator<Column> it = myRecentlyHiddenColumns.listIterator(myRecentlyHiddenColumns.size()); it.hasPrevious();) {
          final Column hidden = it.previous();
          GPAction action = new GPAction("columns.show.label") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
              hidden.setVisible(true);
              myRecentlyHiddenColumns.remove(hidden);
            }
          };
          action.putValue(Action.NAME, GanttLanguage.getInstance().formatText("columns.show.label", hidden.getName()));
          showActions.add(action);
          if (showActions.size() == 5) {
            break;
          }
        }
        result.addAll(showActions);
      }
      return result;
    }
  }

  public void autoFitColumns() {
    int visibleWidth = 0;
    int headerHeight = 0;
    for (ColumnImpl column : getTableHeaderUiFacade().getColumns()) {
      if (column.isVisible()) {
        Dimension columnDimension = autoFitColumnWidth(column);
        visibleWidth += columnDimension.width;
        headerHeight = Math.max(headerHeight, column.getHeaderFitDimension().height);
      }
    }
    // {
    // Rectangle bounds = getBounds();
    // setBounds(bounds.x, bounds.y, visibleWidth, bounds.height);
    // }
    if (headerHeight > 0) {
      Rectangle bounds = getTable().getTableHeader().getBounds();
      getTable().getTableHeader().setBounds(bounds.x, bounds.y, visibleWidth, headerHeight);
    }
    {
      Rectangle bounds = getTable().getBounds();
      getTable().setBounds(bounds.x, bounds.y, visibleWidth, getTable().getRowCount() * getTable().getRowHeight());
    }
  }

  private Dimension autoFitColumnWidth(ColumnImpl column) {
    Dimension dimension = UIUtil.autoFitColumnWidth(getTable(), column.getTableColumnExt());
    column.getTableColumnExt().setWidth(dimension.width);
    column.getTableColumnExt().setPreferredWidth(dimension.width);
    return dimension;

  }
}
