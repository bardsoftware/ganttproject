package net.sourceforge.ganttproject;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskNode;

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
        super(project, uifacade, project.getTaskCustomColumnManager(), model);
        this.ttModel = model;
        myUIfacade = uifacade;
        getTableHeaderUiFacade().createDefaultColumns(DefaultColumn.getColumnStubs());
        init();
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

    protected List<Column> getDefaultColumns() {
        return DefaultColumn.getColumnStubs();
    }

    protected void init() {
        super.init();
        getTable().getColumnModel().addColumnModelListener((TableColumnModelListener) this.getTreeTableModel());
        getTable().getModel().addTableModelListener(new ModelListener());
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(true) {
            @Override
            protected TimelineChart getChart() {
                return myUIfacade.getGanttChart();
            }
        });
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

    void setDelay(TaskNode taskNode, Delay delay) {
        try {
            int indexInfo = getTable().getColumnModel().getColumnIndex(GanttTreeTableModel.strColInfo);
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
