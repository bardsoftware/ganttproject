package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import net.sourceforge.ganttproject.gui.ResourceDialogCustomColumn;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.resource.ResourceColumn;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManager.RoleEvent;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.HierarchicalColumnHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;
import org.jdesktop.swing.table.TableColumnExt;

public class ResourceTreeTable extends GPTreeTableBase implements CustomPropertyManager{
    private final RoleManager myRoleManager;

    /**
     * Unique instance of GanttLanguage.
     */
    private static GanttLanguage language = GanttLanguage.getInstance();

    private final ResourceTreeTableModel myResourceTreeModel;

    /**
     * PopupMenu showed on right click on the table header.
     */
    private JPopupMenu popupMenu = null;


    /** Component used to delete a custom column */
    JMenuItem delColumnItem = null;

    /** Position where the user clicked on the table header */
    private Point clickPoint = null;

    private final HumanResourceManager myResourceManager;

    private final IGanttProject myProject;

    private final TableHeaderUIFacade myVisibleFields = new TableHeaderImpl();

    /**
     * Creates an instance of GanttTreeTable with the given TreeTableModel.
     *
     * @param model
     *            TreeTableModel.
     */
    public ResourceTreeTable(IGanttProject project, ResourceTreeTableModel model) {
        super(project, model);
        myProject = project;
        myProject.addProjectEventListener(new ProjectEventListener(){
            public void projectClosed() {
                deleteAllColumns();
            }
            public void projectModified() {
            }
            public void projectSaved() {
            }
        });
        myResourceManager = (HumanResourceManager) project.getHumanResourceManager();
        myRoleManager = project.getRoleManager();
        myRoleManager.addRoleListener(new RoleManager.Listener() {
            public void rolesChanged(RoleEvent e) {
                setUpRolesRenderer();
                setUpAssignementRolesRenderer();
            }
        });
        myResourceTreeModel = model;
        this.setTreeTableModel(model);
        initTreeTable();
        myResourceTreeModel.setSelectionModel(getTree().getSelectionModel());
    }

    private void deleteAllColumns() {
        List<CustomPropertyDefinition> customPropsDefinitions = getDefinitions();
        for (int i=0; i<customPropsDefinitions.size(); i++) {
            CustomPropertyDefinition nextDefinition = customPropsDefinitions.get(i);
            deleteCustomColumn(nextDefinition.getName());
        }
        myResourceTreeModel.decreaseCustomPropertyIndex(customPropsDefinitions.size());
    }
    public boolean isVisible(DefaultMutableTreeNode node) {
        return this.getTreeTable().getTree().isVisible(
                new TreePath(node.getPath()));
    }

    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
    }

    /**
     * Initialize the treetable. Addition of various listeners, tree's icons,
     */
    public void initTreeTable() {
        // ttModel = (ResourceTreeTableModel) this.getTreeTableModel();
        Enumeration enumeration = getTable().getColumnModel().getColumns();
        Collection<TableColumnExt> lToDel = new ArrayList<TableColumnExt>();
        while (enumeration.hasMoreElements()) {
            TableColumnExt tc = (TableColumnExt) enumeration.nextElement();
            lToDel.add(tc);
        }

        Iterator<TableColumnExt> it = lToDel.iterator();
        while (it.hasNext())
            getTable().removeColumn(it.next());

        getTable().setAutoCreateColumnsFromModel(false);
        getTable().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        TableColumnExt tce1 = newTableColumnExt(0);
        tce1.setTitle(ResourceTreeTableModel.strResourceName);
        TableColumnExt tce2 = newTableColumnExt(1);
        tce2.setTitle(ResourceTreeTableModel.strResourceRole);
        TableColumnExt tce3 = newTableColumnExt(2);
        tce3.setTitle(ResourceTreeTableModel.strResourceEMail);
        TableColumnExt tce4 = newTableColumnExt(3);
        tce4.setTitle(ResourceTreeTableModel.strResourcePhone);
        TableColumnExt tce5 = newTableColumnExt(4);
        tce5.setTitle(ResourceTreeTableModel.strResourceRoleForTask);

        /* adding the columns on the screen and to the data model*/
        this.addMandatoryColumn(new ResourceColumn(tce1, myResourceTreeModel.useNextIndex(), String.class));
        this.addMandatoryColumn(new ResourceColumn(tce2, myResourceTreeModel.useNextIndex(), String.class));
        this.addMandatoryColumn(new ResourceColumn(tce3, myResourceTreeModel.useNextIndex(), String.class));
        this.addMandatoryColumn(new ResourceColumn(tce4, myResourceTreeModel.useNextIndex(), String.class));
        this.addMandatoryColumn(new ResourceColumn(tce5, myResourceTreeModel.useNextIndex(), String.class));

        initColumnsAlignements();
        ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
        for (int i=2; i<cols.size(); i++) {
            hideColumn(cols.get(i));
        }
        // Highlighters to ease the reading of the table.
        setHighlighters(new HighlighterPipeline(new Highlighter[] {
                AlternateRowHighlighter.quickSilver,
                new HierarchicalColumnHighlighter() }));

        setShowHorizontalLines(true);
        setHasColumnControl(true);

        ImageIcon icon = new ImageIcon(getClass().getResource(
                "/icons/simple_task.gif"));

        setOpenIcon(icon);
        setClosedIcon(icon);
        setCollapsedIcon(new ImageIcon(getClass()
                .getResource("/icons/plus.gif")));
        setExpandedIcon(new ImageIcon(getClass()
                .getResource("/icons/minus.gif")));
        setLeafIcon(icon);

        this.getTreeTable().getParent().setBackground(Color.WHITE);
        setUpRolesRenderer();
        setUpAssignementRolesRenderer();

        // getTable().getTableHeader().addMouseListener(new MouseListener(){
        //
        // public void mouseClicked(MouseEvent arg0) {
        // // TODO Auto-generated method stub
        //
        // }
        //
        // public void mousePressed(MouseEvent arg0) {
        // // TODO Auto-generated method stub
        //
        // }
        //
        // public void mouseReleased(MouseEvent e) {
        // Enumeration en = getTable().getColumnModel().getColumns();
        // while(en.hasMoreElements())
        // {
        // TableColumn tc = (TableColumn)en.nextElement();
        // tc.setPreferredWidth(tc.getWidth());
        // }
        // }
        //
        // public void mouseEntered(MouseEvent arg0) {
        // // TODO Auto-generated method stub
        //
        // }
        //
        // public void mouseExited(MouseEvent arg0) {
        // // TODO Auto-generated method stub
        //
        // }
        //
        // });

        this.getTreeTable().getTree().addTreeExpansionListener(
                new TreeExpansionListener() {
                    public void treeExpanded(TreeExpansionEvent arg0) {
                        Mediator.getGanttProjectSingleton().repaint2();

                    }

                    public void treeCollapsed(TreeExpansionEvent arg0) {
                        Mediator.getGanttProjectSingleton().repaint2();
                    }
                });
        this.setPreferredSize(new Dimension(this.getPreferredSize().width, 0));

        /* listener provoking the popup menu for the column management */
        this.getTable().getTableHeader().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                handlePopupTrigger(e);
            }

            public void mouseReleased(MouseEvent e) {
                handlePopupTrigger(e);
            }

            private void handlePopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    clickPoint = e.getPoint();
                    String name = getTable().getColumnName(getTable().columnAtPoint(clickPoint));
                    createPopup();

                    /* the delete button is activated only for removable columns*/
                    if (myResourceTreeModel.checkRemovableCol(name))
                        delColumnItem.setEnabled(true);
                    else
                        delColumnItem.setEnabled(false);

                    Component c = (Component) e.getSource();
                    popupMenu.show(c, e.getX(), e.getY());
                }
            }


        });
        getTable().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
            }
            public void columnMarginChanged(ChangeEvent e) {
            }
            public void columnMoved(TableColumnModelEvent e) {
                if (e.getFromIndex()!=e.getToIndex()) {
                    updateColumnOrders(e.getFromIndex(), e.getToIndex());
                    getProject().setModified();
                }
            }
            public void columnRemoved(TableColumnModelEvent e) {

            }
            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });

    }

    protected void updateColumnOrders(int fromIndex, int toIndex) {
        List<ResourceColumn> columns = myResourceTreeModel.getColumns();
        for (int i=0; i<columns.size(); i++) {
            ResourceColumn nextColumn = columns.get(i);
            if (nextColumn.getOrder()==fromIndex) {
                nextColumn.setOrder(toIndex);
                continue;
            }
            if (nextColumn.getOrder()==toIndex) {
                nextColumn.setOrder(fromIndex);
                continue;
            }
        }
    }

    /* creates the popup menu for the column management */
    private void createPopup() {
        popupMenu = new JPopupMenu();

        /* show columns list */
        ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
        ResourceColumn col;
        int size = cols.size();
        for (int i =0; i < size; i++) {
            col = cols.get(i);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(col.getTitle(), col.isVisible());
            item.addActionListener(new ColumnHandler(col));
            popupMenu.add(item);
        }
        popupMenu.addSeparator();

        /* 'display all columns' button*/
        JMenuItem showAllItem = new JMenuItem(language.getText("displayAll"));
        showAllItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    /* TODO the undo management */
                    Mediator.getGanttProjectSingleton().getUndoManager()
                    .undoableEdit("displayAllColumns", new Runnable() {
                            public void run() {
                                /* sets all the columns visible */
                                ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
                                for (int i =0; i < cols.size(); i++) {
                                    ResourceColumn col = cols.get(i);
                                    if (!col.isVisible()) {
                                        showColumn(col);
                                    }
                                }
                                getProject().setModified(true);
                            }
                        });
                }
            });
        popupMenu.add(showAllItem);
        popupMenu.addSeparator();

        /* 'add new column' button */
        JMenuItem addColumnItem = new JMenuItem(language.getText("addCustomColumn"));
        addColumnItem.setIcon(new ImageIcon(getClass().getResource("/icons/addCol_16.gif")));
        addColumnItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    /* TODO the undo management */
                    Mediator.getGanttProjectSingleton().getUndoManager()
                    .undoableEdit("addCustomColumn", new Runnable() {
                            public void run() {
                                ResourceColumn column = null;
                                addCustomColumn(column);
                                getProject().setModified();
                            }
                        });
                }
            });
        popupMenu.add(addColumnItem);

        /* 'delete column' button */
        delColumnItem = new JMenuItem(language.getText("deleteCustomColumn"));
        delColumnItem.setIcon(new ImageIcon(getClass().getResource("/icons/removeCol_16.gif")));
        delColumnItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /* TODO undo managment */
                Mediator.getGanttProjectSingleton().getUndoManager()
                        .undoableEdit("deleteCustomColumn", new Runnable() {
                            public void run() {
                                int ind = getTable().columnAtPoint(clickPoint);
                                if(ind >=0){
                                    deleteCustomColumn(getTable().getColumnName(ind));
                                    delColumnItem.setEnabled(false);
                                    getProject().setModified();
                                }
                            }
                        });

            }
        });
        popupMenu.add(delColumnItem);
    }

    /* Shows the given column. The column will appear on
     * it's default position */
    void showColumn(ResourceColumn col) {
        col.setVisible(true);
        if (col.getOrder()<getTable().getColumnCount()) {
            col.setOrder(getTable().getColumnCount());
        }
        this.addColumn(col.getColumn());
    }

    void hideColumn(ResourceColumn col) {
        col.setVisible(false);
        this.removeColumn(col.getColumn());
    }

    public void addMandatoryColumn(ResourceColumn column) {
        this.addColumn(column.getColumn());
        myResourceTreeModel.addMandatoryColumn(column);
    }

    private ResourceColumn newResourceColumn() {
        TableColumnExt col = newTableColumnExt(myResourceTreeModel.useNextIndex());
        ResourceColumn result = new ResourceColumn(col, col.getModelIndex());
        return result;
    }

    private ResourceColumn newResourceColumn(int id) {
        TableColumnExt col = newTableColumnExt(myResourceTreeModel.useNextIndex());
        ResourceColumn result = new ResourceColumn(col, id);
        return result;
    }
    /* creates a custom property column in the datamodel and on the screen */
    public void addCustomColumn(ResourceColumn column) {
        if (column == null) {
            /* create dialog and create column */
            column = newResourceColumn();
            ResourceDialogCustomColumn d = new ResourceDialogCustomColumn(Mediator
                    .getGanttProjectSingleton().getUIFacade(), column);
            d.setVisible(true);
            if (!d.isOk()) {
                return;
            }
            column = d.getColumn();
        }

        if (column.getTitle() != null) {
            /* adding the column into the datamodel */
            try {
                myResourceTreeModel.addCustomColumn(column.getTitle(), column);
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        e.getMessage(),
                        /* TODO add translation */
                        "Column add",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            /* adding the column to the screen */
            this.addColumn(column.getColumn());

            /* setting the cell renderer */
            String colClass = column.getType().getName();
            int align;

            if (colClass.equals("java.lang.Integer") || colClass.equals("java.lang.Double")) {
                align = SwingConstants.RIGHT;
                setColumnHorizontalAlignment(column.getTitle(), align);
            }
            else if (colClass.equals("java.util.GregorianCalendar")) {
                align = SwingConstants.CENTER;
                setColumnHorizontalAlignment(column.getTitle(), align);
                /* the customised date cell editor */
                column.getColumn().setCellEditor(newDateCellEditor());
            }
            else {
                align = SwingConstants.LEFT;
                setColumnHorizontalAlignment(column.getTitle(), align);
            }
        }
    }

    /* deletes the column from the screen and the data model */
    public void deleteCustomColumn(String name) {
        ResourceColumn col = null;
        col = myResourceTreeModel.deleteCustomColumn(name);
        this.removeColumn(col.getColumn());
    }

    void setUpRolesRenderer() {
        Role roles[] = getRoleManager().getEnabledRoles();
        final JComboBox comboBox = new JComboBox();
        for (int i = 0; i < roles.length; i++)
            comboBox.addItem(roles[i]);

        try {
            TableColumn roleColumn = this
                    .getColumn(ResourceTreeTableModel.strResourceRole);
            comboBox.setEditable(false);
            roleColumn.setCellEditor(new DefaultCellEditor(comboBox));
        } catch (IllegalArgumentException ex) {

        }
    }

    private RoleManager getRoleManager() {
        return myRoleManager;
    }

    void setUpAssignementRolesRenderer() {
        // Role roles[] = Mediator.getRoleManager().getEnabledRoles();
        // final JComboBox comboBox = new JComboBox();
        // for (int i = 0; i < roles.length; i++)
        // comboBox.addItem(roles[i]);
        final JComboBox comboBox = new JComboBox(getRoleManager()
                .getEnabledRoles());

        try {
            TableColumn roleColumn = this
                    .getColumn(ResourceTreeTableModel.strResourceRoleForTask);
            comboBox.setEditable(false);
            roleColumn.setCellEditor(new DefaultCellEditor(comboBox));
        } catch (IllegalArgumentException ex) {

        }
    }

    private void initColumnsAlignements() {
        // setColumnHorizontalAlignment(ResourceTreeTableModel.strResourceName,
        // SwingConstants.LEFT);
        setColumnHorizontalAlignment(ResourceTreeTableModel.strResourceRole,
                SwingConstants.LEFT);
        setColumnHorizontalAlignment(ResourceTreeTableModel.strResourceEMail,
                SwingConstants.LEFT);
        setColumnHorizontalAlignment(ResourceTreeTableModel.strResourcePhone,
                SwingConstants.RIGHT);
        setColumnHorizontalAlignment(
                ResourceTreeTableModel.strResourceRoleForTask,
                SwingConstants.LEFT);

        // Set the columns widths
        getTable().getColumnExt(ResourceTreeTableModel.strResourceName)
                .setPreferredWidth(150);
        getTable().getColumnExt(ResourceTreeTableModel.strResourceRole)
                .setPreferredWidth(120);
        getTable().getColumnExt(ResourceTreeTableModel.strResourceEMail)
                .setPreferredWidth(100);
        getTable().getColumnExt(ResourceTreeTableModel.strResourcePhone)
                .setPreferredWidth(100);
        getTable().getColumnExt(ResourceTreeTableModel.strResourceRoleForTask)
                .setPreferredWidth(100);
        // getTable().getColumnExt(ResourceTreeTableModel.strResourceName)
        // .setMaxWidth(300);
        // getTable().getColumnExt(ResourceTreeTableModel.strResourceRole)
        // .setMaxWidth(300);
        // getTable().getColumnExt(ResourceTreeTableModel.strResourceEMail)
        // .setMaxWidth(300);
        // getTable().getColumnExt(ResourceTreeTableModel.strResourcePhone)
        // .setMaxWidth(300);
        // getTable().getColumnExt(ResourceTreeTableModel.strResourceRoleForTask)
        // .setMaxWidth(300);
    }

    /** @return the list of the selected nodes. */
    public DefaultMutableTreeNode[] getSelectedNodes() {
        TreePath[] currentSelection = getTreeTable().getTree()
                .getSelectionPaths();

        if (currentSelection == null || currentSelection.length == 0) {
            return new DefaultMutableTreeNode[0];
        }
        DefaultMutableTreeNode[] dmtnselected = new DefaultMutableTreeNode[currentSelection.length];

        for (int i = 0; i < currentSelection.length; i++) {
            dmtnselected[i] = (DefaultMutableTreeNode) currentSelection[i]
                    .getLastPathComponent();
        }
        return dmtnselected;
    }

    public boolean isExpanded(ProjectResource pr) {
        ResourceNode node = ((ResourceTreeTableModel) getTreeTableModel())
                .exists(pr);
        if (node != null)
            return getTreeTable().isExpanded(new TreePath(node.getPath()));
        return false;
    }

    public void addKeyListener(KeyListener listener) {
        //getTreeTable().addKeyListener(listener);
        super.addKeyListener(listener);
        getTable().addKeyListener(listener);
        getTree().addKeyListener(listener);
    }

    public JTree getTree()
    {
        return this.getTreeTable().getTree();
    }

    public void setAction(Action action) {
        InputMap inputMap = new InputMap();

        inputMap.put((KeyStroke) action.getValue(Action.ACCELERATOR_KEY),
                action.getValue(Action.NAME));

        inputMap.setParent(getTreeTable().getInputMap(JComponent.WHEN_FOCUSED));
        getTreeTable().setInputMap(JComponent.WHEN_FOCUSED, inputMap);

        // Add the action to the component
        getTreeTable().getActionMap().put(action.getValue(Action.NAME), action);
    }

    public void addMouseListener(MouseListener listener) {
        getTreeTable().addMouseListener(listener);
    }

    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    /* This actionlistener changes the column's visibility */
    class ColumnHandler implements ActionListener {
        private ResourceColumn column;

        public ColumnHandler(ResourceColumn c) {
            column = c;
        }

        public void actionPerformed(ActionEvent e) {
            if (column.isVisible())
                hideColumn(column);
            else
                showColumn(column);
            getProject().setModified();
        }
    }

    boolean canMoveSelectionUp() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return false;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode previousSibling = selectedNode.getPreviousSibling();
        if (previousSibling == null) {
            return false;
        }
        return true;
    }

    void upResource() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode previousSibling = selectedNode.getPreviousSibling();
        if (previousSibling == null) {
            return;
        }
        if (selectedNode instanceof ResourceNode) {
            HumanResource people = (HumanResource)selectedNode.getUserObject();
            myResourceTreeModel.moveUp(people);
            getTree().setSelectionPath(new TreePath(selectedNode.getPath()));
        } else if (selectedNode instanceof AssignmentNode) {
            swapAssignents(selectedNode, previousSibling);
        }
    }

    boolean canMoveSelectionDown() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return false;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode nextSibling = selectedNode.getNextSibling();
        if (nextSibling == null) {
            return false;
        }
        return true;
    }

    /** Move down the selected resource */
    void downResource() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length==0) {
            return;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode nextSibling = selectedNode.getNextSibling();
        if (nextSibling == null) {
            return;
        }
        if (selectedNode instanceof ResourceNode) {
            HumanResource people = (HumanResource)selectedNode.getUserObject();
            myResourceTreeModel.moveDown(people);
            getTree().setSelectionPath(new TreePath(selectedNode.getPath()));
        } else if (selectedNode instanceof AssignmentNode) {
            swapAssignents(selectedNode, nextSibling);
        }
    }

    void swapAssignents(DefaultMutableTreeNode selected, DefaultMutableTreeNode sibling) {
        ResourceAssignment selectedAssignment = ((AssignmentNode)selected).getAssignment();
        assert sibling instanceof AssignmentNode;
        ResourceAssignment previousAssignment = ((AssignmentNode)sibling).getAssignment();
        selectedAssignment.getResource().swapAssignments(selectedAssignment, previousAssignment);
    }

    public 	CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString) {
        final ResourceColumn newColumn = newResourceColumn(Integer.valueOf(id).intValue());
        newColumn.setTitle(name);
        final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, defaultValueAsString);
        newColumn.setType(stubDefinition.getType());
        newColumn.setDefaultVal(stubDefinition.getDefaultValue());
        assert String.valueOf(newColumn.getIndex()).equals(id);
        addCustomColumn(newColumn);
        List<CustomPropertyDefinition> definitions = myResourceManager.getDefinitions();
        return definitions.get(definitions.size()-1);
    }

    public List<CustomPropertyDefinition> getDefinitions() {
        return myResourceManager.getDefinitions();
    }

    public void importData(CustomPropertyManager source) {
        List sourceDefs = source.getDefinitions();
        for (int i=0; i<sourceDefs.size(); i++) {
            CustomPropertyDefinition nextDefinition = (CustomPropertyDefinition) sourceDefs.get(i);
            createDefinition(nextDefinition.getID(),
                             nextDefinition.getTypeAsString(),
                             nextDefinition.getName(),
                             nextDefinition.getDefaultValueAsString());
        }
    }

    public TableHeaderUIFacade getVisibleFields() {
        return myVisibleFields;
    }

    class TableHeaderImpl implements TableHeaderUIFacade {
        public void add(String name, int order, int width) {
            ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
            for (int i =0; i < cols.size(); i++) {
                ResourceColumn col = cols.get(i);
                if (name.equals(col.getID()) && !col.isVisible()) {
                    col.setWidth(width);
                    col.setOrder(order);
                    showColumn(col);
                }
            }
        }

        public void clear() {
            deleteAllColumns();
        }

        public Column getField(int index) {
            return myResourceTreeModel.getColumns().get(index);
        }

        public int getSize() {
            return myResourceTreeModel.getColumns().size();
        }

        public void importData(TableHeaderUIFacade source) {
            if (source.getSize() == 0) {
              return;
            }
            ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
            for (int i =0; i < cols.size(); i++) {
                ResourceColumn col = cols.get(i);
                hideColumn(col);
            }
            ArrayList<Column> sourceColumns = new ArrayList<Column>();
            for (int i=0; i<source.getSize(); i++) {
                Column nextField = source.getField(i);
                sourceColumns.add(nextField);
            }
            Collections.sort(sourceColumns, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Column lhs = (Column) o1;
                    Column rhs = (Column) o2;
                    return lhs.getOrder()-rhs.getOrder();
                }
            });
            for (int i=0; i<sourceColumns.size(); i++) {
                Column nextField = sourceColumns.get(i);
                add(nextField.getID(), i, nextField.getWidth());
            }

        }

    }

    @Override
    public void addListener(CustomPropertyListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public CustomPropertyDefinition createDefinition(String typeAsString,
            String colName, String defValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteDefinition(CustomPropertyDefinition def) {
        // TODO Auto-generated method stub

    }

    @Override
    public CustomPropertyDefinition getCustomPropertyDefinition(String id) {
        // TODO Auto-generated method stub
        return null;
    }
}
