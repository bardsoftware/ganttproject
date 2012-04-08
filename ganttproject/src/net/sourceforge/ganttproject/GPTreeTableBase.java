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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Rectangle;
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
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

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

public abstract class GPTreeTableBase extends JXTreeTable implements CustomPropertyListener {
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;
  private final TableHeaderUiFacadeImpl myTableHeaderFacade = new TableHeaderUiFacadeImpl();
  private final CustomPropertyManager myCustomPropertyManager;
  private boolean isInitialized;
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
      TreeTableCellEditorImpl cellEditor = (TreeTableCellEditorImpl) getTable().getCellEditor(
          t.getSelectedRow(), t.getSelectedColumn());
      t.editCellAt(t.getSelectedRow(), t.getSelectedColumn());
      cellEditor.requestFocus();
    }
  };

  protected class TableHeaderUiFacadeImpl implements TableHeaderUIFacade {
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
        myDefaultColumnStubs.get(i).setVisible(false);
        createColumn(i, myDefaultColumnStubs.get(i));
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
        ColumnStub columnStub = new TableHeaderUIFacade.ColumnStub(id, def.getName(), true, order, width);
        column = createColumn(getSize(), columnStub);
      }
      if (column == null) {
        return;
      }
      insertColumnIntoUi(column);
    }

    @Override
    public void importData(TableHeaderUIFacade source) {
      for (int i = 0; i < source.getSize(); i++) {
        Column foreign = source.getField(i);
        ColumnImpl mine = findColumnByID(foreign.getID());
        if (mine == null) {
          mine = createColumn(getModelIndex(foreign), foreign);
        } else {
          mine.getStub().setOrder(foreign.getOrder());
          mine.getStub().setVisible(foreign.isVisible());
          mine.getStub().setWidth(foreign.getWidth());
        }
      }
      Collections.sort(myColumns, new Comparator<ColumnImpl>() {
        @Override
        public int compare(ColumnImpl left, ColumnImpl right) {
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
      assert false : "Column=" + c + " was not defined";
      return -1;
    }

    protected void createDefaultColumns(List<TableHeaderUIFacade.Column> stubs) {
      myDefaultColumnStubs.clear();
      for (Column stub : stubs) {
        myDefaultColumnStubs.add(new TableHeaderUIFacade.ColumnStub(stub.getID(), stub.getName(), stub.isVisible(),
            stub.getOrder(), stub.getWidth()));
      }
    }

    protected ColumnImpl createColumn(int modelIndex, TableHeaderUIFacade.Column stub) {
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

  protected static class ColumnImpl implements TableHeaderUIFacade.Column {
    private final JXTreeTable myTable;
    private final TableColumnExt myTableColumn;
    private final Column myStub;

    protected ColumnImpl(JXTreeTable table, TableColumnExt tableColumn, TableHeaderUIFacade.Column stub) {
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
      TableCellRenderer renderer = myTableColumn.getHeaderRenderer();
      if (renderer == null) {
        renderer = myTable.getTableHeader().getDefaultRenderer();
      }
      Component comp = renderer.getTableCellRendererComponent(myTable, myTableColumn.getHeaderValue(), false, false, 0,
          0);
      return comp.getPreferredSize();
    }

  }

  protected IGanttProject getProject() {
    return myProject;
  }

  // TODO(dbarashev): make sure that we don't need these hacks anymore
  @SuppressWarnings("unused")
  private static JXTreeTable createTable(DefaultTreeTableModel model) {
    JXTreeTable result = new JXTreeTable(model) {
      {
        setTableHeader(new JTableHeader(getColumnModel()) {
          @Override
          public void applyComponentOrientation(ComponentOrientation o) {
            super.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
          }
        });
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
    };
    return result;
  }

  protected GPTreeTableBase(IGanttProject project, UIFacade uiFacade, CustomPropertyManager customPropertyManager,
      DefaultTreeTableModel model) {
    super(model);
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
  }

  protected void onProjectOpened() {
  }

  protected void onProjectCreated() {
    getTableHeaderUiFacade().createDefaultColumns(getDefaultColumns());
    getTableHeaderUiFacade().importData(TableHeaderUIFacade.Immutable.fromList(getDefaultColumns()));
  }

  protected void initTreeTable() {
    doInit();
    isInitialized = true;
  }

  protected void doInit() {
//    TODO(dbarashev): make sure that scroll bars are located properly
//    scrollPane = new JScrollPane() {
//      @Override
//      public void applyComponentOrientation(ComponentOrientation o) {
//        super.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
//      }
//    };
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
    setShowHorizontalLines(true);
    // TODO(dbarashev): make sure that everything is fine with the column control
    //setHasColumnControl(true);

    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/simple_task.gif"));
    setOpenIcon(icon);
    setClosedIcon(icon);
    setCollapsedIcon(new ImageIcon(getClass().getResource("/icons/plus.gif")));
    setExpandedIcon(new ImageIcon(getClass().getResource("/icons/minus.gif")));
    setLeafIcon(icon);
    getTreeTable().getParent().setBackground(Color.WHITE);

    InputMap inputMap = getInputMap();
    inputMap.setParent(getTreeTable().getInputMap(JComponent.WHEN_FOCUSED));
    getTreeTable().setInputMap(JComponent.WHEN_FOCUSED, inputMap);
    ActionMap actionMap = getActionMap();
    actionMap.setParent(getTreeTable().getActionMap());
    getTreeTable().setActionMap(actionMap);
    addActionWithAccelleratorKey(myEditCellAction);

//  TODO(dbarashev): restore highlighters
//    setHighlighters(new HighlighterPipeline(new Highlighter[] { AlternateRowHighlighter.quickSilver,
//        new HierarchicalColumnHighlighter() }));

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
    getTableHeaderUiFacade().importData(TableHeaderUIFacade.Immutable.fromList(getDefaultColumns()));

    getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
    TableHeaderUIFacade.Column stub = new TableHeaderUIFacade.ColumnStub(customColumn.getId(), customColumn.getName(),
        true, getTable().getColumnCount(), 100);
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

  public TableHeaderUIFacade getVisibleFields() {
    return getTableHeaderUiFacade();
  }

  protected TableHeaderUiFacadeImpl getTableHeaderUiFacade() {
    return myTableHeaderFacade;
  }

  protected List<TableHeaderUIFacade.Column> getDefaultColumns() {
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
      result.setCellEditor(new TreeTableCellEditorImpl(editor));
    }
    return result;
  }

  TableCellRenderer createCellRenderer(Class<?> columnClass) {
    TableCellRenderer renderer = null;
//  TODO(dbarashev): make sure that icon and boolean values render fine
//    if (Icon.class.equals(columnClass) || Boolean.class.equals(columnClass)) {
//      renderer = TableCellRenderers.getNewDefaultRenderer(columnClass);
//
//    }
    if (renderer == null) {
      renderer = getTreeTable().getDefaultRenderer(columnClass);
    }
    return renderer;
  }

  TableCellEditor createCellEditor(Class<?> columnClass) {
    TableCellEditor editor = columnClass.equals(GregorianCalendar.class) ? newDateCellEditor()
        : getTreeTable().getDefaultEditor(columnClass);
    return wrapEditor(editor);
  }

  private static TableCellEditor wrapEditor(TableCellEditor editor) {
    return new TreeTableCellEditorImpl(editor);
  }

  protected TableCellEditor newDateCellEditor() {
    return new DateCellEditor();
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
    return getEnclosingScrollPane();
  }

  @Override
  public void addMouseListener(MouseListener mouseListener) {
    super.addMouseListener(mouseListener);
    getTable().addMouseListener(mouseListener);
    getTree().addMouseListener(mouseListener);
    this.getTreeTable().getParent().addMouseListener(mouseListener);
  }

  @Override
  public void addKeyListener(KeyListener keyListener) {
    super.addKeyListener(keyListener);
    getTable().addKeyListener(keyListener);
    getTree().addKeyListener(keyListener);
  }

  private static class DateCellEditor extends DefaultCellEditor {
    // normal textfield background color
    private final Color colorNormal = null;

    // error textfield background color (when the date isn't correct
    private final Color colorError = new Color(255, 125, 125);

    private Date myDate;

    public DateCellEditor() {
      super(new JTextField());
    }

    @Override
    public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
      JTextField result = (JTextField) super.getTableCellEditorComponent(arg0, arg1, arg2, arg3, arg4);
      result.selectAll();
      return result;
    }

    @Override
    public Object getCellEditorValue() {
      return new GanttCalendar(myDate == null ? new Date() : myDate);
    }

    @Override
    public boolean stopCellEditing() {
      final String dateString = ((JTextComponent) getComponent()).getText();
      Date parsedDate = GanttLanguage.getInstance().parseDate(dateString);
      if (parsedDate == null) {
        getComponent().setBackground(colorError);
        return false;
      }
      myDate = parsedDate;
      getComponent().setBackground(colorNormal);
      super.fireEditingStopped();
      return true;
    }
  }

  protected abstract class VscrollAdjustmentListener implements AdjustmentListener {
    private final boolean isMod;

    protected VscrollAdjustmentListener(boolean calculateMod) {
      isMod = calculateMod;
    }

    protected abstract TimelineChart getChart();

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      if (!isInitialized) {
        return;
      }
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
  }

  void insertWithLeftyScrollBar(JComponent container) {
    container.add(getScrollPane(), BorderLayout.CENTER);
    getScrollPane().getViewport().add(getTable());

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
    final int margin = 5;
    final JTable table = getTable();
    final TableColumnExt tableColumn = column.myTableColumn;

    Dimension headerFit = column.getHeaderFitDimension();
    int width = headerFit.width;
    int height = 0;

    // Get maximum width of column data
    for (int r = 0; r < getTable().getRowCount(); r++) {
      TableCellRenderer renderer = table.getCellRenderer(r, column.getOrder());
      Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, column.getOrder()), false,
          false, r, column.getOrder());
      width = Math.max(width, comp.getPreferredSize().width);
      height += comp.getPreferredSize().height;
    }
    // Add margin
    width += 2 * margin;
    // Set the width
    tableColumn.setWidth(width);
    tableColumn.setPreferredWidth(width);
    return new Dimension(width, height);
  }
}
