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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.gui.GanttDialogCustomColumn;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.language.GanttLanguage.Listener;
import net.sourceforge.ganttproject.task.CustomColumEvent;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomColumsListener;
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
 * Treetable used to displayed tabular data and hierarchical data.
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
     * retore the column at the same index it has been removed.
     */
    private final Map<TableColumnExt, ColumnKeeper> mapTableColumnColumnKeeper = new LinkedHashMap<TableColumnExt, ColumnKeeper>();

    /**
     * Menu item to delete columns.
     */
    private JMenuItem jmiDeleteColumn;

    DisplayedColumnsList listDisplayedColumns = null;

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
    public GanttTreeTable(IGanttProject project, UIFacade uifacade, GanttTreeTableModel model) {
        super(project, model);
        initTreeTable();
        this.ttModel = model;
        myUIfacade = uifacade;
    }


    void setAction(Action action) {

        addAction(action, (KeyStroke) action.getValue(Action.ACCELERATOR_KEY));


        // Add the action to the component
    }

    void addAction(Action action, KeyStroke keyStroke) {
        InputMap inputMap = getInputMap();
        inputMap.put(keyStroke, action.getValue(Action.NAME));
        getActionMap().put(action.getValue(Action.NAME), action);
    }

    private void updateDisplayedColumnsOrder() {
        Iterator it = this.listDisplayedColumns.iterator();
        while (it.hasNext()) {
            DisplayedColumn dc = (DisplayedColumn) it.next();
            if (dc.isDisplayed()) {
                String id = dc.getID();
                String name = getNameForId(id);
                int viewIndex = getTable().convertColumnIndexToView(
                        getColumn(name).getModelIndex());
                dc.setOrder(viewIndex);
                dc.setWidth(getColumn(name).getPreferredWidth());
            }
        }
    }

    public DisplayedColumnsList getDisplayColumns() {
        updateDisplayedColumnsOrder();
        return this.listDisplayedColumns;
    }

    public void setDisplayedColumns(DisplayedColumnsList displayedColumns) {
        DisplayedColumnsList l = (DisplayedColumnsList) displayedColumns
                .clone();
        displayAllColumns();
        hideAllColumns();
        this.listDisplayedColumns = l;
        Collections.sort(this.listDisplayedColumns);
        Iterator it = this.listDisplayedColumns.iterator();
        while (it.hasNext()) {
            DisplayedColumn dc = (DisplayedColumn) it.next();
            String id = dc.getID();
            String name = getNameForId(id);

            if (dc.displayed)
                displayColumn(name);
            else
                hideColumn(name);
        }
    }

    void reloadColumns() {
        List columns = Collections.list(getTable().getColumnModel().getColumns());
        for (int i=0; i<columns.size(); i++) {
            getTable().removeColumn((TableColumn) columns.get(i));
        }
        if (myLanguageListener!=null) {
            GanttLanguage.getInstance().removeListener(myLanguageListener);
        }
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
        getTable().getColumnExt(GanttTreeTableModel.strColName).setCellEditor(
                new NameCellEditor());
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
        getTable().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);


        // the model must implement TableColumnModelListener

        // Highlighters to ease the reading of the table.

        // set the CellEditor for the dates.

        setShowHorizontalLines(true);

        setOpenIcon(null);
        setClosedIcon(null);
        setCollapsedIcon(new ImageIcon(getClass()
                .getResource("/icons/plus.gif")));
        setExpandedIcon(new ImageIcon(getClass()
                .getResource("/icons/minus.gif")));
        setLeafIcon(null);


        this.setHasColumnControl(false);
        // this.getTable().moveColumn(0,2);
        // List l = new ArrayList();
        // DisplayedColumn dc1 = new DisplayedColumn("tpd3");
        // DisplayedColumn dc2 = new DisplayedColumn("tpd8");
        // dc1.setDisplayed(true);
        // dc2.setDisplayed(true);
        // l.add(dc1);
        // l.add(dc2);
        // this.setDisplayedColumns(l);
        // // hideColumn("Name");

        this.getTreeTable().getParent().setBackground(Color.WHITE);
//        EventListener listeners[] = this.getTreeTable().getParent()
//                .getListeners(MouseListener.class);
//        for (int i = 0; i < listeners.length; i++)
//            this.getTreeTable().getParent().removeMouseListener(
//                    (MouseListener) listeners[i]);
        this.getTreeTable().getParent().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                e.consume();
                if (e.getClickCount() == 1)
                    Mediator.getGanttProjectSingleton().getTree()
                            .selectTreeRow(-1);
                else if (e.getClickCount() == 2
                        && e.getButton() == MouseEvent.BUTTON1) {
                    Mediator.getGanttProjectSingleton().getUndoManager()
                            .undoableEdit("New Task by click", new Runnable() {
                                public void run() {
                                    Mediator.getGanttProjectSingleton()
                                            .newTask();
                                }
                            });
                }
            }
        });
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
            getTable().getTableHeader().addMouseListener(new HeaderMouseListener());
            // The following is used to store the new index of a moved column in
            // order
            // to restore it properly.
            getTable().getColumnModel().addColumnModelListener(
                    new TableColumnModelListener() {
                        public void columnAdded(TableColumnModelEvent e) {
                            // nothing to do
                        }

                        public void columnRemoved(TableColumnModelEvent e) {
                            // nothing to do
                        }

                        public void columnMoved(TableColumnModelEvent e) {
                            DefaultTableColumnModel o = (DefaultTableColumnModel) e
                                    .getSource();
                            TableColumn tc = o.getColumn(e.getFromIndex());
                            ColumnKeeper ck = mapTableColumnColumnKeeper
                                    .get(tc);
                            if (ck != null)
                                ck.setInitIndex(e.getToIndex());
                            if (Mediator.getGanttProjectSingleton() != null)
                                Mediator.getGanttProjectSingleton().setAskForSave(
                                        true);
                            updateDisplayedColumnsOrder();
                        }

                        public void columnMarginChanged(ChangeEvent e) {
                            // nothing to do
                        }

                        public void columnSelectionChanged(ListSelectionEvent e) {
                            // nothing to do
                        }
                    });

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
    }

    protected void onCellSelectionChanged() {
        if (!getTable().isEditing()) {
            int row = getTable().getSelectedRow();
            int col = getTable().getSelectedColumn();
            Rectangle rect = getTable().getCellRect(row, col, true);
            scrollPane.scrollRectToVisible(rect);
        }
    }


    void addScrollPaneMouseListener(MouseListener ml) {
        this.getTreeTable().getParent().addMouseListener(ml);
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

    void calculateWidth() {
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
        //Iterator it = mapTableColumnColumnKeeper.keySet().iterator();
        popupMenu = new JPopupMenu();
        TableColumnModel tcModel = this.getTable().getColumnModel();
        // int nbCol = tcModel.getColumnCount();

        //int nbCol = mapTableColumnColumnKeeper.keySet().size();
        for (Iterator entries = mapTableColumnColumnKeeper.entrySet().iterator(); entries.hasNext();) {
            // TableColumn column = tcModel.getColumn(i);
            Map.Entry nextEntry = (Entry) entries.next();
            TableColumn column = (TableColumn) nextEntry.getKey();
            JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem(column
                    .getHeaderValue().toString());

            ColumnKeeper ck = (ColumnKeeper)nextEntry.getValue();
            assert ck!=null;
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
        Iterator itDc = this.listDisplayedColumns.iterator();
        while (itDc.hasNext()) {
            DisplayedColumn dc = (DisplayedColumn) itDc.next();
            if (getNameForId(dc.getID()).equals(name)) {
                indexView = dc.getOrder();
                width = dc.getWidth();
            }
        }

        Iterator<TableColumnExt> it = mapTableColumnColumnKeeper.keySet().iterator();
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
        Iterator<TableColumnExt> it = mapTableColumnColumnKeeper.keySet().iterator();
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
    public void addNewCustomColumn(CustomColumn customColumn) {
        if (customColumn.getName() != null) // if something has been entered
        {
            GanttTreeTableModel treeTableModel = (GanttTreeTableModel) getTreeTableModel();
            int nbCol = treeTableModel.getColumnCountTotal(); // +
            // treeTableModel.getCustomColumnCount();
            String newName = customColumn.getName();

            ((GanttTreeTableModel) ttModel).addCustomColumn(newName);

            TaskContainmentHierarchyFacade tchf = getProject().getTaskManager()
                    .getTaskHierarchy();
            setCustomColumnValueToAllNestedTask(tchf, tchf.getRootTask(),
                    customColumn.getName(), customColumn.getDefaultValue());

            TableColumnExt t = newTableColumnExt(nbCol, customColumn);
            t.setMaxWidth(500);
            t.setHeaderValue(newName);
            getTable().getColumnModel().addColumn(t);
            try {
                if (clickPoint != null)
                    getTable().getColumnModel().moveColumn(
                            getTable().getColumnCount() - 1,
                            getTable().columnAtPoint(clickPoint));
            } catch (IllegalArgumentException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
            int align = SwingConstants.CENTER;
            if (customColumn.getType().equals(GregorianCalendar.class))
                align = SwingConstants.RIGHT;
            setColumnHorizontalAlignment(newName, align);

            DisplayedColumn dc = new DisplayedColumn(
                    getProject().getCustomColumnsStorage().getIdFromName(newName));
            dc.setDisplayed(true);
            dc.setOrder(getTable().convertColumnIndexToView(
                    getColumn(newName).getModelIndex()));
            dc.setWidth(getColumn(newName).getPreferredWidth());
            this.listDisplayedColumns.add(dc);

            if (GregorianCalendar.class
                    .isAssignableFrom(customColumn.getType()))
                getTable().getColumnExt(newName).setCellEditor(newDateCellEditor());

            //
            JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem(newName);
            jcbmi.setSelected(true);
            ColumnKeeper ck = new ColumnKeeper(t);
            jcbmi.addActionListener(ck);
            mapTableColumnColumnKeeper.put(t, ck);
            //popupMenu.insert(jcbmi, popupMenu.getSubElements().length - 3);
            //
            getProject().setModified();

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
     * Delete permanently a custom column.
     *
     * @param name
     *            Name of the column to delete.
     */
    public void deleteCustomColumn(CustomColumn column) {

        final String name = column.getName();
        // the column has to be displayed to be removed.
        this.displayColumn(name);

        deleteColumnFromUI(name);
        // Every tasks
        TaskContainmentHierarchyFacade tchf = Mediator
                .getGanttProjectSingleton().getTaskManager().getTaskHierarchy();
        tchf.getRootTask().getCustomValues().removeCustomColumn(name);
        removeCustomColumnToAllNestedTask(tchf, tchf.getRootTask(), name);

        Mediator.getGanttProjectSingleton().setAskForSave(true);
    }

    private void deleteColumnFromUI(String name) {
        //DisplayedColumn toDel = null;
        Iterator it = listDisplayedColumns.iterator();

        while (it.hasNext()) {
            DisplayedColumn dc = (DisplayedColumn) it.next();
            if (getNameForId(dc.getID()).equals(name)) {
                it.remove();
                break;
            }
        }

        int index = getTable().getColumnModel().getColumnIndex(name);
        int modelIndex = getTable().convertColumnIndexToModel(index);
        TableColumnModelEvent tcme = new TableColumnModelEvent(getTable()
                .getColumnModel(), modelIndex, modelIndex);
        getTable().removeColumn(getTable().getColumnExt(name));
        getTable().columnRemoved(tcme);
        /*
         * TODO There is a problem here : I don't remove the custom column from
         * the treetablemodel. If I remove it there will be a problem when
         * deleting a custom column if it isn't the last created.
         */
        // TreeTableModel
        ttModel.deleteCustomColumn(name);

        // newBB
        Iterator<TableColumnExt> it2 = mapTableColumnColumnKeeper.keySet().iterator();
        while (it2.hasNext()) {
            TableColumn c = it2.next();
            String n = (String) c.getHeaderValue();
            if (n.equals(name)) {
                mapTableColumnColumnKeeper.remove(c);
                break;
            }
        }
    }

    public void renameCustomcolumn(String name, String newName) {
        this.displayColumn(name);
        TableColumnExt tc = (TableColumnExt) getTable().getColumn(name);
        tc.setTitle(newName);
        tc.setIdentifier(newName);

        TaskContainmentHierarchyFacade tchf = Mediator
                .getGanttProjectSingleton().getTaskManager().getTaskHierarchy();
        tchf.getRootTask().getCustomValues().renameCustomColumn(name, newName);
        renameCustomColumnForAllNestedTask(tchf, tchf.getRootTask(), name,
                newName);
        ttModel.renameCustomColumn(name, newName);

        // newBB
        Iterator<TableColumnExt> it = mapTableColumnColumnKeeper.keySet().iterator();
        while (it.hasNext()) {
            TableColumn c = it.next();
            String n = (String) c.getHeaderValue();
            if (n.equals(name)) {
                ColumnKeeper ck = mapTableColumnColumnKeeper
                        .get(c);
                ((TableColumnExt) c).setTitle(newName);
                break;
            }
        }

        assert getColumn(newName)!=null;
    }

    // public void changeDefaultValue(String name, Object newDefaultValue)
    // {
    // // this.displayColumn(name);
    // }

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
    private void setCustomColumnValueToAllNestedTask(
            TaskContainmentHierarchyFacade facade, Task root, String colName,
            Object value) {
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
     * Remove permanetly the custom column for the task <code>root</code> and
     * all its children.
     *
     * @param facade
     *            TaskContainmentHierarchyFacade ot retrive nested tasks.
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

    String getIdForName(String colName) {
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
     * Returns the JTree used in the treetable.
     *
     * @return The JTree used in the treetable.
     */
    public JTree getTree() {
        return this.getTreeTable().getTree();
    }

    /**
     * @return The vertical scrollbar.
     */
    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     *
     * @inheritDoc
     */
//    public void requestFocus() {
//        if (getDisplayColumns().isDisplayed(GanttTreeTableModel.strColName)) {
//            int c = getTable().convertColumnIndexToView(
//                    getColumn(GanttTreeTableModel.strColName).getModelIndex());
//            NameCellEditor ed = (NameCellEditor) getTable()
//                    .getCellEditor(-1, c);
//            ed.requestFocus();
//        }
//    }

    public void centerViewOnSelectedCell() {
        int row = getTable().getSelectedRow();
        int col = getTable().getEditingColumn();
        if (col == -1)
            col = getTable().getSelectedColumn();
        Rectangle rect = getTable().getCellRect(row, col, true);
        scrollPane.getHorizontalScrollBar().scrollRectToVisible(rect);
        scrollPane.getViewport().scrollRectToVisible(rect);

    }

    public void addMouseListener(MouseListener mouseListener) {
        super.addMouseListener(mouseListener);
        getTable().addMouseListener(mouseListener);
        getTree().addMouseListener(mouseListener);
        this.getTreeTable().getParent().addMouseListener(mouseListener);
    }

    /**
     * Adds the key listener to the Table, the tree and this.
     */
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

    public class DisplayedColumnsList extends ArrayList {
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
            Iterator it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = (DisplayedColumn) it.next();
                if (getNameForId(dc.getID()).equals(name))
                    return dc.isDisplayed();
            }
            return false;
        }

        public int getOrderForName(String name) {
            Iterator it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = (DisplayedColumn) it.next();
                if (getNameForId(dc.getID()).equals(name))
                    return dc.getOrder();
            }
            return -1;
        }

        public String getNameForOrder(int order) {
            Iterator it = this.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = (DisplayedColumn) it.next();
                if (dc.getOrder() == order)
                    return getNameForId(dc.getID());
            }
            return null;
        }

        public boolean add(Object o) {
            if (o instanceof DisplayedColumn) {
                DisplayedColumn dc1 = (DisplayedColumn) o;
                Iterator it = this.iterator();
                while (it.hasNext()) {
                    DisplayedColumn dc = (DisplayedColumn) it.next();
                    if (dc.getID().equals(dc1.getID())) {
                        this.remove(dc);
                        return super.add(dc1);
                    }
                }
                return super.add(dc1);

            }
            return false;
        }

        public Object clone() {
            DisplayedColumnsList l = new DisplayedColumnsList();
            Iterator it = this.iterator();
            while (it.hasNext())
                l.add(((DisplayedColumn) it.next()).clone());

            return l;

        }
    }

    public class DisplayedColumn implements Comparable, TableHeaderUIFacade.Column {
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
        public int compareTo(Object o) {
            if (o == null)
                return 0;
            if (o instanceof DisplayedColumn) {
                DisplayedColumn dc = (DisplayedColumn) o;
                if (this.order != dc.order)
                    return this.order - dc.order;
                return this.id.compareTo(dc.id);
            }
            return 0;
        }
    }

    /**
     * This actionListener manages the column to be hiden or displayed. It has a
     * TableColumn and hide it or display it
     *
     * @author bbaranne Mar 1, 2005
     */
    class ColumnKeeper implements ActionListener {
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
        protected TableColumn column;

        /**
         * Creates a new ColumnKeeper for the given TableColumn.
         *
         * @param tc
         *            TableColumn to manage.
         */
        public ColumnKeeper(TableColumn tc) {
            column = tc;
            index = column.getModelIndex();
        }

        /**
         * Set the initial index of the table column.
         *
         * @param initIndex
         *            The initial index of the table column.
         */
        public void setInitIndex(int initIndex) {
            index = initIndex;
        }

        /**
         * Returns the initial index of the table column.
         *
         * @return The initial index of the table column.
         */
        public int getInitIndex() {
            return index;
        }

        /**
         * Hides the table column.
         */
        void hide() {
            getTable().getColumnModel().removeColumn(column);
            isShown = false;

            String name = (String) column.getHeaderValue();

            String id = getIdForName(name);
            Iterator it = listDisplayedColumns.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = (DisplayedColumn) it.next();
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
        void show() {
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
            Iterator it = listDisplayedColumns.iterator();
            while (it.hasNext()) {
                DisplayedColumn dc = (DisplayedColumn) it.next();
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
            Mediator.getGanttProjectSingleton().getUndoManager().undoableEdit(
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
     * This class handles the mouse actions on the treetable header.
     *
     * @author bbaranne Mar 1, 2005
     * @version 1.0 Show the popupMenu when popup is triggered.
     */
    class HeaderMouseListener extends MouseAdapter {
        /**
         * Creates a new HeaderMouseListener
         */
        public HeaderMouseListener() {
            super();
        }

        /*
         * Something ugly !! TODO find a means to display the popupmenu with the
         * right UI.
         */
        boolean first = false;

        /**
         * @inheritDoc Shows the popupMenu to hide/show columns and to add
         *             custom columns.
         */
        public void mousePressed(MouseEvent e) {
            handlePopupTrigger(e);
        }

        public void mouseReleased(MouseEvent e) {
            handlePopupTrigger(e);
        }

        private void handlePopupTrigger(MouseEvent e) {
            if (e.isPopupTrigger()) {
                createPopupMenu();
                Component c = (Component) e.getSource();
                //reorderPopuMenu();
                popupMenu.show(c, e.getX(), e.getY());
                clickPoint = e.getPoint();//popupMenu.getLocationOnScreen();
                CustomColumn cc = getProject().getCustomColumnsStorage().getCustomColumn(
                        getTable().getColumnName(
                                getTable().columnAtPoint(e.getPoint())));
                if (cc != null)
                    jmiDeleteColumn.setEnabled(true);
                else
                    jmiDeleteColumn.setEnabled(false);
            }
        }
    }

    /**
     * The class replaces the cell editor used in the hierarchical column of
     * the tree table.
     *
     * @author bbaranne (Benoit Baranne)
     */
    class NameCellEditor extends DefaultCellEditor {
        private JTextField field = null;

        public NameCellEditor() {
            super(new JTextField());
            field = (JTextField) this.editorComponent;
        }


        public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
            final JTextField result = (JTextField) super.getTableCellEditorComponent(arg0, arg1, arg2, arg3, arg4);
            result.selectAll();
//			result.addFocusListener(new FocusAdapter() {
//				public void focusGained(FocusEvent arg0) {
//					super.focusGained(arg0);
//					((JTextComponent)result).selectAll();
//				}
//
//				public void focusLost(FocusEvent arg0) {
//					// TODO Auto-generated method stub
//					super.focusLost(arg0);
//				}
//
//			});
//
            return result;
        }

//
        public void requestFocus() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    field.requestFocus();
                    field.selectAll();
                }
            });
        }
    }

    /**
     * This class repaints the GraphicArea and the table every time the table
     * model has been modified. TODO Add the refresh functionnality when
     * available.
     *
     * @author Benoit Baranne
     */
    class ModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            // Mediator.getGanttProjectSingleton().getArea().repaint();
            // getTable().repaint();
            Mediator.getGanttProjectSingleton().repaint();
        }
    }

    public void editNewTask(Task t) {
        TreePath selectedPath = getTree().getSelectionPath();
        int c = getTable().convertColumnIndexToView(
                getTable().getColumn(GanttTreeTableModel.strColName)
                        .getModelIndex());

        NameCellEditor nameCellEditor = (NameCellEditor) getTable().getCellEditor(-1, c);
        getTreeTable().editCellAt(getTree().getRowForPath(selectedPath), c);
        nameCellEditor.requestFocus();
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
            renameCustomcolumn(event.getOldName(), event.getColName());
            break;
        }
    }


    public TableHeaderUIFacade getVisibleFields() {
        return myVisibleFields;
    }

    class VisibleFieldsImpl implements TableHeaderUIFacade {
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
