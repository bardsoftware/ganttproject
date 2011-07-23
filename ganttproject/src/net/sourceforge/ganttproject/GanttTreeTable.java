package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.gui.GanttDialogCustomColumn;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.language.GanttLanguage.Listener;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskNode;

import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.HierarchicalColumnHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;
import org.jdesktop.swing.table.TableColumnExt;

/**
 * TreeTable used to displayed tabular data and hierarchical data.
 *
 * @author bbaranne
 * @version 1.0 (20050301) (yyyymmdd)
 */
public class GanttTreeTable extends GPTreeTableBase implements CustomPropertyListener {
    /**
     * Unique instance of GanttLanguage.
     */
    private static GanttLanguage language = GanttLanguage.getInstance();

    /**
     * PopupMenu showed on right click (window, linux) on the table header.
     */
    private JPopupMenu popupMenu;

    /**
     * Point where the user has just right clicked on the table header.
     */
    private Point clickPoint = null;

    /**
     * model of the treetable.
     */
    private final GanttTreeTableModel ttModel;

    /**
     * stores the tableColum associated with there ColumnKeeper. it is used to
     * restore the column at the same index it has been removed.
     */
    private final Map<TableColumn, ColumnKeeper> mapTableColumnColumnKeeper = new LinkedHashMap<TableColumn, ColumnKeeper>();

    /**
     * Menu item to delete columns.
     */
    private JMenuItem jmiDeleteColumn;

    private DisplayedColumnsList listDisplayedColumns = null;

    private Listener myLanguageListener;

    private final UIFacade myUIfacade;

    private final TableHeaderUIFacade myVisibleFields = new VisibleFieldsImpl();

    /**
     * Creates an instance of GanttTreeTable with the given TreeTableModel.
     * @param project
     *
     * @param model
     *            TreeTableModel.
     */
    GanttTreeTable(IGanttProject project, UIFacade uifacade, GanttTreeTableModel model) {
        super(project, uifacade, model);
        this.ttModel = model;
        myUIfacade = uifacade;
        initTreeTable();
    }

    private UIFacade getUiFacade() {
        return myUIfacade;
    }

    private void updateDisplayedColumnsOrder() {
//        Iterator<DisplayedColumn> it = this.listDisplayedColumns.iterator();
//        while (it.hasNext()) {
//            DisplayedColumn dc = it.next();
//            if (dc.isDisplayed()) {
//                String id = dc.getID();
//                String name = getNameForId(id);
//                int viewIndex = getTable().convertColumnIndexToView(
//                        getColumn(name).getModelIndex());
//                dc.setOrder(viewIndex);
//                dc.setWidth(getColumn(name).getPreferredWidth());
//            }
//        }
    }

    public DisplayedColumnsList getDisplayColumns() {
        updateDisplayedColumnsOrder();
        return this.listDisplayedColumns;
    }

    private void setDisplayedColumns(DisplayedColumnsList displayedColumns) {
        DisplayedColumnsList l = (DisplayedColumnsList) displayedColumns
                .clone();
        displayAllColumns();
        hideAllColumns();
        this.listDisplayedColumns = l;
        Collections.sort(this.listDisplayedColumns);
        Iterator<DisplayedColumn> it = this.listDisplayedColumns.iterator();
        while (it.hasNext()) {
            DisplayedColumn dc = it.next();
            String id = dc.getID();
            String name = getNameForId(id);

            if (dc.displayed) {
                displayColumn(name);
            } else {
                hideColumn(name);
            }
        }
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

    void _reloadColumns() {
        List<TableColumn> columns = Collections.list(getTable().getColumnModel().getColumns());
        for (int i = 0; i < columns.size(); i++) {
            getTable().removeColumn(columns.get(i));
        }
        if (myLanguageListener != null) {
            GanttLanguage.getInstance().removeListener(myLanguageListener);
        }

        // TODO Put in an Array in order to use loops to manage the TableColumns.
        final TableColumnExt tce1 = newTableColumnExt(0);
        final TableColumnExt tce2 = newTableColumnExt(1);
        final TableColumnExt tce3 = newTableColumnExt(2);
        final TableColumnExt tce4 = newTableColumnExt(3);
        final TableColumnExt tce5 = newTableColumnExt(4);
        final TableColumnExt tce6 = newTableColumnExt(5);
        final TableColumnExt tce7 = newTableColumnExt(6);
        final TableColumnExt tce8 = newTableColumnExt(7);
        final TableColumnExt tce9 = newTableColumnExt(8);
        final TableColumnExt tce10 = newTableColumnExt(9);
        final TableColumnExt tce11 = newTableColumnExt(10);
        myLanguageListener = new GanttLanguage.Listener() {
            public void languageChanged(Event event) {
                GanttTreeTable.this.ttModel.languageChanged(event);
                tce1.setTitle(GanttTreeTableModel.strColType);
                tce2.setTitle(GanttTreeTableModel.strColPriority);
                tce3.setTitle(GanttTreeTableModel.strColInfo);
                tce4.setTitle(GanttTreeTableModel.strColName);
                tce5.setTitle(GanttTreeTableModel.strColBegDate);
                tce6.setTitle(GanttTreeTableModel.strColEndDate);
                tce7.setTitle(GanttTreeTableModel.strColDuration);
                tce8.setTitle(GanttTreeTableModel.strColCompletion);
                tce9.setTitle(GanttTreeTableModel.strColCoordinator);
                tce10.setTitle(GanttTreeTableModel.strColPredecessors);
                tce11.setTitle(GanttTreeTableModel.strColID);
            }
        };

        GanttLanguage.getInstance().addListener(myLanguageListener);
        this.addColumn(tce1);
        this.addColumn(tce2);
        this.addColumn(tce3);
        this.addColumn(tce4);
        this.addColumn(tce5);
        this.addColumn(tce6);
        this.addColumn(tce7);
        this.addColumn(tce8);
        this.addColumn(tce9);
        this.addColumn(tce10);
        this.addColumn(tce11);
        {
            listDisplayedColumns = new DisplayedColumnsList();
            // Type
            DisplayedColumn dc1 = new DisplayedColumn(getIdForName(tce1.getTitle()));
            dc1.setDisplayed(false);
            dc1.setOrder(this.getTable().convertColumnIndexToView(
                    tce1.getModelIndex()));
            dc1.setWidth(tce1.getPreferredWidth());
            listDisplayedColumns.add(dc1);
            // Priority
            DisplayedColumn dc2 = new DisplayedColumn(getIdForName(tce2.getTitle()));
            dc2.setDisplayed(false);
            dc2.setOrder(this.getTable().convertColumnIndexToView(
                    tce2.getModelIndex()));
            dc2.setWidth(tce2.getPreferredWidth());
            listDisplayedColumns.add(dc2);
            // Info
            DisplayedColumn dc3 = new DisplayedColumn(getIdForName(tce3.getTitle()));
            dc3.setDisplayed(false);
            dc3.setOrder(this.getTable().convertColumnIndexToView(
                    tce3.getModelIndex()));
            dc3.setWidth(tce3.getPreferredWidth());
            listDisplayedColumns.add(dc3);
            // Name
            DisplayedColumn dc4 = new DisplayedColumn(getIdForName(tce4.getTitle()));
            dc4.setDisplayed(true);
            dc4.setOrder(this.getTable().convertColumnIndexToView(
                    tce4.getModelIndex()));
            dc4.setWidth(tce4.getPreferredWidth());
            listDisplayedColumns.add(dc4);
            // Begin date
            DisplayedColumn dc5 = new DisplayedColumn(getIdForName(tce5.getTitle()));
            dc5.setDisplayed(true);
            dc5.setOrder(this.getTable().convertColumnIndexToView(
                    tce5.getModelIndex()));
            dc5.setWidth(tce5.getPreferredWidth());
            listDisplayedColumns.add(dc5);
            // End date
            DisplayedColumn dc6 = new DisplayedColumn(getIdForName(tce6.getTitle()));
            dc6.setDisplayed(true);
            dc6.setOrder(this.getTable().convertColumnIndexToView(
                    tce6.getModelIndex()));
            dc6.setWidth(tce6.getPreferredWidth());
            listDisplayedColumns.add(dc6);
            // Duration
            DisplayedColumn dc7 = new DisplayedColumn(getIdForName(tce7.getTitle()));
            dc7.setDisplayed(false);
            dc7.setOrder(this.getTable().convertColumnIndexToView(
                    tce7.getModelIndex()));
            dc7.setWidth(tce7.getPreferredWidth());
            listDisplayedColumns.add(dc7);
            // Completion
            DisplayedColumn dc8 = new DisplayedColumn(getIdForName(tce8.getTitle()));
            dc8.setDisplayed(false);
            dc8.setOrder(this.getTable().convertColumnIndexToView(
                    tce8.getModelIndex()));
            dc8.setWidth(tce8.getPreferredWidth());
            listDisplayedColumns.add(dc8);
            // Coordinator
            DisplayedColumn dc9 = new DisplayedColumn(getIdForName(tce9.getTitle()));
            dc9.setDisplayed(false);
            dc9.setOrder(this.getTable().convertColumnIndexToView(
                    tce9.getModelIndex()));
            dc9.setWidth(tce9.getPreferredWidth());
            listDisplayedColumns.add(dc9);

            // Predecessors
            DisplayedColumn dc10 = new DisplayedColumn(getIdForName(tce10
                    .getTitle()));
            dc10.setDisplayed(false);
            dc10.setOrder(this.getTable().convertColumnIndexToView(
                    tce10.getModelIndex()));
            dc10.setWidth(tce10.getPreferredWidth());
            listDisplayedColumns.add(dc10);

            // ID
            DisplayedColumn dc11 = new DisplayedColumn(getIdForName(tce11
                    .getTitle()));
            dc11.setDisplayed(false);
            dc11.setOrder(this.getTable().convertColumnIndexToView(
                    tce11.getModelIndex()));
            dc11.setWidth(tce11.getPreferredWidth());
            listDisplayedColumns.add(dc11);

            //this.setDisplayedColumns(listDisplayedColumns);
        }
        {
            this.mapTableColumnColumnKeeper.clear();
            this.mapTableColumnColumnKeeper.put(tce1, new ColumnKeeper(tce1));
            this.mapTableColumnColumnKeeper.put(tce2, new ColumnKeeper(tce2));
            this.mapTableColumnColumnKeeper.put(tce3, new ColumnKeeper(tce3));
            this.mapTableColumnColumnKeeper.put(tce4, new ColumnKeeper(tce4));
            this.mapTableColumnColumnKeeper.put(tce5, new ColumnKeeper(tce5));
            this.mapTableColumnColumnKeeper.put(tce6, new ColumnKeeper(tce6));
            this.mapTableColumnColumnKeeper.put(tce7, new ColumnKeeper(tce7));
            this.mapTableColumnColumnKeeper.put(tce8, new ColumnKeeper(tce8));
            this.mapTableColumnColumnKeeper.put(tce9, new ColumnKeeper(tce9));
            this.mapTableColumnColumnKeeper.put(tce10, new ColumnKeeper(tce10));
            this.mapTableColumnColumnKeeper.put(tce11, new ColumnKeeper(tce11));
        }
        initColumnsAlignements();
        getTable().getColumnExt(GanttTreeTableModel.strColBegDate)
                .setCellEditor(newDateCellEditor());
        getTable().getColumnExt(GanttTreeTableModel.strColEndDate)
                .setCellEditor(newDateCellEditor());
//        getTable().getColumnExt(GanttTreeTableModel.strColName).setCellEditor(
//                new NameCellEditor());
        //createPopupMenu();
        if (listDisplayedColumns != null) {
            this.setDisplayedColumns(listDisplayedColumns);
        }
        else {
            this.displayAllColumns();
        }
        Runnable t = new Runnable() {
            public void run() {
                calculateWidth();
                revalidate();
            }
        };
        SwingUtilities.invokeLater(t);
    }
    /**
     * Initialize the treetable. Addition of various listeners, tree's icons,
     */
    void initTreeTable() {
        clickPoint = null;
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

    private void initColumnsAlignements() {
        // set the columns horizontal aligment. It also associate an
        // appropiate renderer according to the column class.
        setColumnHorizontalAlignment(GanttTreeTableModel.strColType,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColPriority,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColInfo,
                SwingConstants.CENTER);
        // setColumnHorizontalAlignment(GanttTreeTableModel.strColName,
        // SwingConstants.LEFT);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColBegDate,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColEndDate,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColDuration,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColCompletion,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColCoordinator,
                SwingConstants.CENTER);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColPredecessors,
                SwingConstants.RIGHT);
        setColumnHorizontalAlignment(GanttTreeTableModel.strColID,
                SwingConstants.CENTER);

        // Set the columns widths
        getTable().getColumnExt(GanttTreeTableModel.strColID)
                .setPreferredWidth(32);
        getTable().getColumnExt(GanttTreeTableModel.strColType)
                .setPreferredWidth(32);
        getTable().getColumnExt(GanttTreeTableModel.strColPriority)
                .setPreferredWidth(42);
        getTable().getColumnExt(GanttTreeTableModel.strColInfo)
                .setPreferredWidth(32);
        getTable().getColumnExt(GanttTreeTableModel.strColName)
                .setPreferredWidth(120);
        getTable().getColumnExt(GanttTreeTableModel.strColDuration)
                .setPreferredWidth(50);

        // getTable().getColumnExt(GanttTreeTableModel.strColType).setMaxWidth(32);
        // getTable().getColumnExt(GanttTreeTableModel.strColPriority).setMaxWidth(42);
        // getTable().getColumnExt(GanttTreeTableModel.strColInfo).setMaxWidth(32);
        // getTable().getColumnExt(GanttTreeTableModel.strColName).setMaxWidth(800);
        // getTable().getColumnExt(GanttTreeTableModel.strColBegDate).setMaxWidth(300);
        // getTable().getColumnExt(GanttTreeTableModel.strColEndDate).setMaxWidth(300);
        // getTable().getColumnExt(GanttTreeTableModel.strColDuration).setMaxWidth(300);
        // getTable().getColumnExt(GanttTreeTableModel.strColCompletion).setMaxWidth(300);
        // getTable().getColumnExt(GanttTreeTableModel.strColCoordinator).setMaxWidth(300);
    }

    private void calculateWidth() {
        int width = 0;

        int nbCol = getTable().getColumnCount();

        for (int i = 0; i < nbCol; i++) {
            TableColumnExt tce = getTable().getColumnExt(i);
            if (tce.isVisible())
                width += tce.getPreferredWidth();
        }

        getTable().setPreferredScrollableViewportSize(new Dimension(width, 0));
    }

    /**
     * Creates the popupMenu used to hide/show columns and to add customs
     * columns. It will associate each tablecolumn with an ColumnKeeper in
     * charge of adding and removing the tablecolumn.
     */
    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        for (Iterator<Entry<TableColumn, ColumnKeeper>> entries = mapTableColumnColumnKeeper
                .entrySet().iterator(); entries.hasNext();) {
            Map.Entry<TableColumn, ColumnKeeper> nextEntry = entries.next();
            TableColumn column = nextEntry.getKey();
            JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem(column
                    .getHeaderValue().toString());

            ColumnKeeper ck = nextEntry.getValue();
            assert ck != null;
            jcbmi.setSelected(ck.isShown);

            jcbmi.addActionListener(ck);
            popupMenu.add(jcbmi);
        }
        popupMenu.addSeparator();

        JMenuItem jmiAddColumn = new JMenuItem(language
                .getText("addCustomColumn"));
        jmiAddColumn.setIcon(new ImageIcon(getClass().getResource(
                "/icons/addCol_16.gif")));
        jmiAddColumn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myUIfacade.getUndoManager()
                        .undoableEdit("PopUpNewColumn", new Runnable() {
                            public void run() {
                                new GanttDialogCustomColumn(
                                        myUIfacade, getProject().getTaskCustomColumnManager()).setVisible(true);
                            }
                        });
            }
        });

        JMenuItem jmiDisplayAll = new JMenuItem(language.getText("displayAll"));
        jmiDisplayAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Mediator.getGanttProjectSingleton().getUndoManager()
                        .undoableEdit("displayAllColumns", new Runnable() {
                            public void run() {
                                displayAllColumns();
                            }
                        });

            }
        });

        /*
         * To delete a custom column the user has to right click on the column
         * header. If the colum header match with a custom column the menu item
         * will be enable. Otherwise it is disable.
         */
        jmiDeleteColumn = new JMenuItem(language.getText("deleteCustomColumn"));
        jmiDeleteColumn.setIcon(new ImageIcon(getClass().getResource(
                "/icons/removeCol_16.gif")));
        jmiDeleteColumn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myUIfacade.getUndoManager()
                        .undoableEdit("deleteCustomColumn", new Runnable() {
                            public void run() {
                                // deleteCustomColumn(getTable().getColumnName(getTable().columnAtPoint(clickPoint)));
                                int ind = getTable().columnAtPoint(clickPoint);
                                if(ind >=0){
                                    String columnName = getTable().getColumnName(ind);
                                    CustomPropertyDefinition def =
                                        getProject().getTaskCustomColumnManager().getCustomPropertyDefinition(columnName);
                                    getProject().getTaskCustomColumnManager().deleteDefinition(def);
                                jmiDeleteColumn.setEnabled(false);
                                }
                            }
                        });

            }
        });
        jmiDeleteColumn.setEnabled(false);

        popupMenu.add(jmiDisplayAll);
        popupMenu.addSeparator();
        popupMenu.add(jmiAddColumn);
        popupMenu.add(jmiDeleteColumn);
    }

    /**
     * Displays all the table columns.
     */
    private void displayAllColumns() {
        Iterator<ColumnKeeper> it = mapTableColumnColumnKeeper.values().iterator();
        while (it.hasNext()) {
            ColumnKeeper ck = it.next();
            if (!ck.isShown)
                ck.show();
        }
        //createPopupMenu();
    }

    /**
     * Displays all the table columns.
     */
    private void hideAllColumns() {
        Iterator<ColumnKeeper> it = mapTableColumnColumnKeeper.values().iterator();
        while (it.hasNext()) {
            ColumnKeeper ck = it.next();
            if (ck.isShown)
                ck.hide();
        }
        //createPopupMenu();
    }

    /**
     * Display the column whose name is given in parameter.
     *
     * @param name
     *            Name of the column to display.
     */
    private void displayColumn(String name) {
        int indexView = -1;
        int width = -1;
        Iterator<DisplayedColumn> itDc = this.listDisplayedColumns.iterator();
        while (itDc.hasNext()) {
            DisplayedColumn dc = itDc.next();
            if (getNameForId(dc.getID()).equals(name)) {
                indexView = dc.getOrder();
                width = dc.getWidth();
            }
        }

        Iterator<TableColumn> it = mapTableColumnColumnKeeper.keySet().iterator();
        while (it.hasNext()) {
            TableColumn c = it.next();
            String n = (String) c.getHeaderValue();
            if (n.equals(name)) {
                ColumnKeeper ck = mapTableColumnColumnKeeper
                        .get(c);
                if (indexView != -1)
                    ck.index = indexView;
                if (!ck.isShown)
                    ck.show();
                break;
            }
        }

        getTable().getColumnExt(name).setPreferredWidth(width);

        //createPopupMenu();
    }

    private void hideColumn(String name) {
        Iterator<TableColumn> it = mapTableColumnColumnKeeper.keySet().iterator();
        while (it.hasNext()) {
            TableColumn c = it.next();
            String n = (String) c.getHeaderValue();
            if (n.equals(name)) {
                ColumnKeeper ck = mapTableColumnColumnKeeper
                        .get(c);
                if (ck.isShown)
                    ck.hide();
                break;
            }
        }
        //createPopupMenu();
    }

    /**
     * Adds a new custom column. The custom column will affect all tasks and
     * future tasks. Several types are available for the custom columns (string,
     * date, integer, double, boolean). A default value is also set.
     */
    private void addNewCustomColumn(CustomColumn customColumn) {
        TaskContainmentHierarchyFacade tchf = getProject().getTaskManager().getTaskHierarchy();
        setCustomColumnValueToAllNestedTask(
            tchf, tchf.getRootTask(), customColumn.getName(), customColumn.getDefaultValue());

        TableHeaderUIFacade.Column stub = new TableHeaderUIFacade.ColumnStub(
            customColumn.getId(), customColumn.getName(), true, getTable().getColumnCount(), 100);
        ColumnImpl columnImpl = getTableHeaderUiFacade().createColumn(getTable().getModel().getColumnCount() - 1, stub);
        getTableHeaderUiFacade().insertColumnIntoUi(columnImpl);

//        Runnable t = new Runnable() {
//            public void run() {
//                calculateWidth();
//                revalidate();
//            }
//        };
//        SwingUtilities.invokeLater(t);
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
     * @param facade
     *            TaskContainmentHierarchyFacade ot retrive nested tasks.
     * @param root
     *            Root task to start with.
     * @param colName
     *            Name of the new custom column to add to the tasks.
     * @param value
     *            Value for this new custom column.
     */
    private static void setCustomColumnValueToAllNestedTask(
            TaskContainmentHierarchyFacade facade, Task root, String colName, Object value) {
        try {
            root.getCustomValues().setValue(colName, value);
        } catch (CustomColumnsException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
        Task[] tt = facade.getNestedTasks(root);
        for (int i = 0; i < tt.length; i++) {
            try {
                tt[i].getCustomValues().setValue(colName, value);
            } catch (CustomColumnsException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
            setCustomColumnValueToAllNestedTask(facade, tt[i], colName, value);
        }
    }

    /**
     * Remove permanently the custom column for the task <code>root</code> and
     * all its children.
     *
     * @param facade
     *            TaskContainmentHierarchyFacade of retrieved nested tasks.
     * @param root
     *            Root task to start with.
     * @param colName
     *            Name of the custom column to remove.
     */
    private void removeCustomColumnToAllNestedTask(
            TaskContainmentHierarchyFacade facade, Task root, String colName) {
        // root.getCustomValues().removeCustomColumn(colName);

        Task[] tt = facade.getNestedTasks(root);
        for (int i = 0; i < tt.length; i++) {
            tt[i].getCustomValues().removeCustomColumn(colName);
            removeCustomColumnToAllNestedTask(facade, tt[i], colName);
        }
    }

    private void renameCustomColumnForAllNestedTask(
            TaskContainmentHierarchyFacade facade, Task root, String oldName,
            String newName) {
        // root.getCustomValues().renameCustomColumn(oldName,newName);

        Task[] tt = facade.getNestedTasks(root);
        for (int i = 0; i < tt.length; i++) {
            tt[i].getCustomValues().renameCustomColumn(oldName, newName);
            renameCustomColumnForAllNestedTask(facade, tt[i], oldName, newName);
        }
    }

    private String getIdForName(String colName) {
        String id = null;
        if (colName.equals(GanttTreeTableModel.strColType))
            id = "tpd0";
        else if (colName.equals(GanttTreeTableModel.strColPriority))
            id = "tpd1";
        else if (colName.equals(GanttTreeTableModel.strColInfo))
            id = "tpd2";
        else if (colName.equals(GanttTreeTableModel.strColName))
            id = "tpd3";
        else if (colName.equals(GanttTreeTableModel.strColBegDate))
            id = "tpd4";
        else if (colName.equals(GanttTreeTableModel.strColEndDate))
            id = "tpd5";
        else if (colName.equals(GanttTreeTableModel.strColDuration))
            id = "tpd6";
        else if (colName.equals(GanttTreeTableModel.strColCompletion))
            id = "tpd7";
        else if (colName.equals(GanttTreeTableModel.strColCoordinator))
            id = "tpd8";
        else if (colName.equals(GanttTreeTableModel.strColPredecessors))
            id = "tpd9";
        else if (colName.equals(GanttTreeTableModel.strColID))
            id = "tpd10";
        else
            id = getProject().getCustomColumnsStorage().getIdFromName(colName);
        return id;
    }

    private String getNameForId(String id) {
        String name = null;
        if (id.equals("tpd0"))
            name = GanttTreeTableModel.strColType;
        else if (id.equals("tpd1"))
            name = GanttTreeTableModel.strColPriority;
        else if (id.equals("tpd2"))
            name = GanttTreeTableModel.strColInfo;
        else if (id.equals("tpd3"))
            name = GanttTreeTableModel.strColName;
        else if (id.equals("tpd4"))
            name = GanttTreeTableModel.strColBegDate;
        else if (id.equals("tpd5"))
            name = GanttTreeTableModel.strColEndDate;
        else if (id.equals("tpd6"))
            name = GanttTreeTableModel.strColDuration;
        else if (id.equals("tpd7"))
            name = GanttTreeTableModel.strColCompletion;
        else if (id.equals("tpd8"))
            name = GanttTreeTableModel.strColCoordinator;
        else if (id.equals("tpd9"))
            name = GanttTreeTableModel.strColPredecessors;
        else if (id.equals("tpd10"))
            name = GanttTreeTableModel.strColID;
        else
            name = getProject().getCustomColumnsStorage().getNameFromId(id);
        return name;
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

    /*
     * ----- INNER CLASSES -----
     */

    public class DisplayedColumnsList extends ArrayList<DisplayedColumn> {
        public DisplayedColumnsList() {
            super();
        }

        /**
         * Returns <code>true</code> if the column name <code>name</code>
         * is displayed, <code>false</code> otherwise.
         *
         * @param name
         *            Name of the column to check the display.
         * @return <code>true</code> if the column name <code>name</code> is
         *         displayed, <code>false</code> otherwise.
         */
        public boolean isDisplayed(String name) {
            Iterator<DisplayedColumn> it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = it.next();
                if (getNameForId(dc.getID()).equals(name))
                    return dc.isDisplayed();
            }
            return false;
        }

        public int getOrderForName(String name) {
            Iterator<DisplayedColumn> it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = it.next();
                if (getNameForId(dc.getID()).equals(name))
                    return dc.getOrder();
            }
            return -1;
        }

        public String getNameForOrder(int order) {
            Iterator<DisplayedColumn> it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = it.next();
                if (dc.getOrder() == order)
                    return getNameForId(dc.getID());
            }
            return null;
        }

        public boolean add(DisplayedColumn dc) {
            if (dc instanceof DisplayedColumn) {
                Iterator<DisplayedColumn> it = this.iterator();
                while (it.hasNext()) {
                    DisplayedColumn dc1 = it.next();
                    if (dc1.getID().equals(dc.getID())) {
                        this.remove(dc1);
                        return super.add(dc);
                    }
                }
                return super.add(dc);

            }
            return false;
        }

        public Object clone() {
            DisplayedColumnsList l = new DisplayedColumnsList();
            Iterator<DisplayedColumn> it = this.iterator();
            while (it.hasNext()) {
                l.add((DisplayedColumn) it.next().clone());
            }
            return l;
        }
    }

    public class DisplayedColumn implements Comparable<DisplayedColumn>, TableHeaderUIFacade.Column {
        private String id = null;

        private boolean displayed = false;

        private int order = -1;

        private int width = 0;

        public DisplayedColumn(String id) {
            this.id = id;
        }

        public void setID(String id) {
            this.id = id;
        }

        public void setDisplayed(boolean disp) {
            this.displayed = disp;
        }

        public boolean isDisplayed() {
            return this.displayed;
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                displayColumn(getName());
            } else {
                hideColumn(getName());
            }
        }

        public boolean isVisible() {
            return isDisplayed();
        }
        public String getName() {
            return getNameForId(getID());
        }
        public String getID() {
            return this.id;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return this.order;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public Object clone() {
            DisplayedColumn dc = new DisplayedColumn(this.id);
            dc.setDisplayed(this.isDisplayed());
            dc.setOrder(this.getOrder());
            dc.setWidth(this.getWidth());
            return dc;
        }

        public String toString() {
            return "[ID = " + id + ", displayed = " + displayed + ", order = "
                    + order + "]";
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(DisplayedColumn dc) {
            if (dc == null) {
                return 0;
            }
            if (dc instanceof DisplayedColumn) {
                if (this.order != dc.order) {
                    return this.order - dc.order;
                }
                return this.id.compareTo(dc.id);
            }
            return 0;
        }
    }

    /**
     * This actionListener manages the column to be hidden or displayed. It has a
     * TableColumn and hide it or display it
     *
     * @author bbaranne Mar 1, 2005
     */
    private class ColumnKeeper implements ActionListener {
        /**
         * the initial index of the table column.
         */
        private int index;

        /**
         * True if the table column is displayed, false otherwise.
         */
        private boolean isShown = true;

        /**
         * The managed table column.
         */
        private final TableColumn column;

        /**
         * Creates a new ColumnKeeper for the given TableColumn.
         *
         * @param tc
         *            TableColumn to manage.
         */
        private ColumnKeeper(TableColumn tc) {
            column = tc;
            index = column.getModelIndex();
        }

        /**
         * Set the initial index of the table column.
         *
         * @param initIndex
         *            The initial index of the table column.
         */
        private void setInitIndex(int initIndex) {
            index = initIndex;
        }

        /**
         * Hides the table column.
         */
        private void hide() {
            getTable().getColumnModel().removeColumn(column);
            isShown = false;

            String name = (String) column.getHeaderValue();

            String id = getIdForName(name);
            Iterator<DisplayedColumn> it = listDisplayedColumns.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = it.next();
                if (dc.getID().equals(id))
                    dc.setDisplayed(false);
            }
            // Thread t = new Thread(){
            // public void run(){
            calculateWidth();
            revalidate();
            // }
            // };
            // SwingUtilities.invokeLater(t);
        }

        /**
         * Shows the table column.
         */
        private void show() {
            boolean reloadInfo = false;
            getTable().getColumnModel().addColumn(column);
            try {
                int columnViewIndexOld = index;
                int columnModelIndexActual = column.getModelIndex();
                if (column.getIdentifier().equals(
                        GanttTreeTableModel.strColInfo))
                    reloadInfo = true;
                int columnViewIndexActual = getTable()
                        .convertColumnIndexToView(columnModelIndexActual);
                getTable()
                        .moveColumn(columnViewIndexActual, columnViewIndexOld);
            } catch (IllegalArgumentException e) {
                index = getTable().getModel().getColumnCount() - 1;
            }
            isShown = true;

            String name = (String) column.getHeaderValue();
            String id = getIdForName(name);
            boolean found = false;
            Iterator<DisplayedColumn> it = listDisplayedColumns.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = it.next();
                if (dc.getID().equals(id)) {
                    dc.setDisplayed(true);
                    found = true;
                }
            }
            if (!found && id != null) {
                DisplayedColumn dc = new DisplayedColumn(id);
                dc.setDisplayed(true);
                listDisplayedColumns.add(dc);
            }

            if (reloadInfo)
                if (Mediator.getDelayManager() != null)
                    Mediator.getDelayManager().fireDelayObservation();

            // Thread t = new Thread(){
            // public void run(){
            calculateWidth();
            revalidate();
            // }
            // };
            // SwingUtilities.invokeLater(t);

        }

        public void actionPerformed(ActionEvent e) {
            getUiFacade().getUndoManager().undoableEdit(
                    "HIDE OR SHOW A COLUMN", new Runnable() {
                        public void run() {
                            if (!isShown)
                                show();
                            else
                                hide();
                            getTable().repaint();
                        }
                    });
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

    void editNewTask(Task t) {

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
}
