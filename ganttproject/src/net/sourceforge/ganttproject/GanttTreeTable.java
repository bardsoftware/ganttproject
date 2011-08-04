package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskNode;

import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.HierarchicalColumnHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;

/**
 * TreeTable used to displayed tabular data and hierarchical data.
 *
 * @author bbaranne
 * @version 1.0 (20050301) (yyyymmdd)
 */
public class GanttTreeTable extends GPTreeTableBase implements CustomPropertyListener {
    private final GanttTreeTableModel ttModel;

    private final UIFacade myUIfacade;

    GanttTreeTable(IGanttProject project, UIFacade uifacade, GanttTreeTableModel model) {
        super(project, uifacade, model);
        this.ttModel = model;
        myUIfacade = uifacade;
        initTreeTable();
    }

    private UIFacade getUiFacade() {
        return myUIfacade;
    }

    private static enum DefaultColumn {
        TYPE(new TableHeaderUIFacade.ColumnStub("tpd0", null, false, -1, -1)),
        PRIORITY(new TableHeaderUIFacade.ColumnStub("tpd1", null, false, -1, 50)),
        INFO(new TableHeaderUIFacade.ColumnStub("tpd2", null, false, -1, -1)),
        NAME(new TableHeaderUIFacade.ColumnStub("tpd3", null, true, 0, 200)),
        BEGIN_DATE(new TableHeaderUIFacade.ColumnStub("tpd4", null, true, 1, 75)),
        END_DATE(new TableHeaderUIFacade.ColumnStub("tpd5", null, true, 0, 75)),
        DURATION(new TableHeaderUIFacade.ColumnStub("tpd6", null, false, -1, 50)),
        COMPLETION(new TableHeaderUIFacade.ColumnStub("tpd7", null, false, -1, 50)),
        COORDINATOR(new TableHeaderUIFacade.ColumnStub("tpd8", null, false, -1, 200)),
        PREDECESSORS(new TableHeaderUIFacade.ColumnStub("tpd9", null, false, -1, 200)),
        ID(new TableHeaderUIFacade.ColumnStub("tpd10", null, false, -1, 20)),
        ;

        private final Column myDelegate;
        private DefaultColumn(TableHeaderUIFacade.Column delegate) {
            myDelegate = delegate;
        }

        Column getStub() {
            return myDelegate;
        }

        static List<Column> getColumnStubs() {
            List<Column> result = new ArrayList<Column>();
            for (DefaultColumn dc : values()) {
                result.add(dc.myDelegate);
            }
            return result;
        }
    }

    void reloadColumns() {
        getTableHeaderUiFacade().clearColumns();
        getTableHeaderUiFacade().createDefaultColumns(DefaultColumn.getColumnStubs());
    }

    void initTreeTable() {
        getProject().getTaskCustomColumnManager().addListener(this);
        getTreeTableModel().addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent arg0) {
            }

            public void treeNodesInserted(TreeModelEvent arg0) {
            }

            public void treeNodesRemoved(TreeModelEvent arg0) {

            }

            public void treeStructureChanged(TreeModelEvent arg0) {
            }

        });
        getTable().setAutoCreateColumnsFromModel(false);
        getTable().setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        setShowHorizontalLines(true);

        setOpenIcon(null);
        setClosedIcon(null);
        setCollapsedIcon(new ImageIcon(getClass()
                .getResource("/icons/plus.gif")));
        setExpandedIcon(new ImageIcon(getClass()
                .getResource("/icons/minus.gif")));
        setLeafIcon(null);


        this.setHasColumnControl(false);
        this.getTreeTable().getParent().setBackground(Color.WHITE);
        {

            InputMap inputMap = getInputMap();
            inputMap.setParent(getTreeTable().getInputMap(JComponent.WHEN_FOCUSED));
            getTreeTable().setInputMap(JComponent.WHEN_FOCUSED, inputMap);
            ActionMap actionMap= getActionMap();
            actionMap.setParent(getTreeTable().getActionMap());
            getTreeTable().setActionMap(actionMap);

        }
        {
            getTable().getColumnModel().addColumnModelListener(
                    (TableColumnModelListener) this.getTreeTableModel());
            getTable().getModel().addTableModelListener(new ModelListener());
            // The following is used to store the new index of a moved column in
            // order
            // to restore it properly.

        }
        setHighlighters(new HighlighterPipeline(new Highlighter[] {
                AlternateRowHighlighter.quickSilver,
                new HierarchicalColumnHighlighter() }));

        getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                onCellSelectionChanged();
            }
        });
        getTable().getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                onCellSelectionChanged();
            }
        });

        reloadColumns();
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(true) {
            @Override
            protected TimelineChart getChart() {
                return myUIfacade.getGanttChart();
            }
        });
    }

    private void onCellSelectionChanged() {
        if (!getTable().isEditing()) {
            int row = getTable().getSelectedRow();
            int col = getTable().getSelectedColumn();
            Rectangle rect = getTable().getCellRect(row, col, true);
            scrollPane.scrollRectToVisible(rect);
        }
    }


    /**
     * Adds a new custom column. The custom column will affect all tasks and
     * future tasks. Several types are available for the custom columns (string,
     * date, integer, double, boolean). A default value is also set.
     */
    private void addNewCustomColumn(CustomColumn customColumn) {
        TaskContainmentHierarchyFacade tchf = getProject().getTaskManager().getTaskHierarchy();

        TableHeaderUIFacade.Column stub = new TableHeaderUIFacade.ColumnStub(
            customColumn.getId(), customColumn.getName(), true, getTable().getColumnCount(), 100);
        ColumnImpl columnImpl = getTableHeaderUiFacade().createColumn(getTable().getModel().getColumnCount() - 1, stub);
        getTableHeaderUiFacade().insertColumnIntoUi(columnImpl);
    }

    /**
     * Delete permanently a custom column.
     *
     * @param name
     *            Name of the column to delete.
     */
    private void deleteCustomColumn(CustomColumn column) {
        getTableHeaderUiFacade().deleteColumn(column);
    }



    /**
     * @return The JTree used in the treetable.
     */
    JTree getTree() {
        return this.getTreeTable().getTree();
    }

    void centerViewOnSelectedCell() {
        int row = getTable().getSelectedRow();
        int col = getTable().getEditingColumn();
        if (col == -1) {
            col = getTable().getSelectedColumn();
        }
        Rectangle rect = getTable().getCellRect(row, col, true);
        scrollPane.getHorizontalScrollBar().scrollRectToVisible(rect);
        scrollPane.getViewport().scrollRectToVisible(rect);
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

    void setDelay(TaskNode taskNode, Delay delay) {
        try {
            int indexInfo = getTable().getColumnModel().getColumnIndex(
                    GanttTreeTableModel.strColInfo);
            indexInfo = getTable().convertColumnIndexToModel(indexInfo);
            ttModel.setValueAt(delay, taskNode, indexInfo);
        } catch (IllegalArgumentException e) {
        }
    }


    /**
     * This class repaints the GraphicArea and the table every time the table
     * model has been modified.
     * TODO Add the refresh functionality when available.
     *
     * @author Benoit Baranne
     */
    private class ModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            getUiFacade().getGanttChart().reset();
        }
    }

    void editSelectedTask() {

        TreePath selectedPath = getTree().getSelectionPath();
        Column column = getTableHeaderUiFacade().findColumnByID(DefaultColumn.NAME.getStub().getID());
        TreeTableCellEditorImpl cellEditor = (TreeTableCellEditorImpl) getTable().getCellEditor(-1, column.getOrder());
        getTable().editCellAt(getTree().getRowForPath(selectedPath), column.getOrder());
        cellEditor.requestFocus();
    }


    public void customPropertyChange(CustomPropertyEvent event) {
        switch(event.getType()) {
        case CustomPropertyEvent.EVENT_ADD:
            addNewCustomColumn((CustomColumn) event.getDefinition());
            break;
        case CustomPropertyEvent.EVENT_REMOVE:
            deleteCustomColumn((CustomColumn) event.getDefinition());
            break;
        case CustomPropertyEvent.EVENT_PROPERTY_CHANGE:
            getTableHeaderUiFacade().renameColumn(event.getDefinition());
            getTable().getTableHeader().repaint();
            break;
        }
    }

    /*
    public TableHeaderUIFacade getVisibleFields() {
        return myVisibleFields;
    }

    private class VisibleFieldsImpl implements TableHeaderUIFacade {
        public void add(String name, int order, int width) {
            DisplayedColumn newColumn = new DisplayedColumn(name);
            newColumn.setOrder(order);
            if (width>=0) {
                newColumn.setWidth(width);
            }
            newColumn.setDisplayed(true);
            DisplayedColumnsList clone = (DisplayedColumnsList) getDisplayColumns().clone();
            clone.add(newColumn);
            setDisplayedColumns(clone);
        }

        public void clear() {
            setDisplayedColumns(new DisplayedColumnsList());
        }

        public Column getField(int index) {
            return (Column) getDisplayColumns().get(index);
        }


        public int getSize() {
            return getDisplayColumns().size();
        }
        public void importData(TableHeaderUIFacade source) {
            clear();
            DisplayedColumnsList clone = (DisplayedColumnsList) getDisplayColumns().clone();
            clone.clear();
            for (int i=0; i<source.getSize(); i++) {
                Column nextField = source.getField(i);
                DisplayedColumn newColumn = new DisplayedColumn(nextField.getName());
                newColumn.setID(nextField.getID());
                newColumn.setOrder(nextField.getOrder());
                if (nextField.getWidth()>=0) {
                    newColumn.setWidth(nextField.getWidth());
                }
                newColumn.setDisplayed(true);
                clone.add(newColumn);
            }
            setDisplayedColumns(clone);
        }
    }
    */
}
