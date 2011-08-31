/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.NewTaskAction;
import net.sourceforge.ganttproject.action.task.DeleteTasksAction;
import net.sourceforge.ganttproject.action.task.TaskPropertiesAction;
import net.sourceforge.ganttproject.chart.VisibleNodesFilter;
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.delay.DelayObserver;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager.Listener;
import net.sourceforge.ganttproject.task.algorithm.AdjustTaskBoundsAlgorithm;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * Class that generate the JTree
 */
public class GanttTree2 extends JPanel implements DragSourceListener,
        DragGestureListener, DelayObserver, TaskTreeUIFacade {
    /** The root node of the Tree */
    private TaskNode rootNode;

    /** The model for the JTableTree */
    private GanttTreeTableModel treeModel;

    private UIFacade myUIFacade;

    /** The GanttTreeTable. */
    private GanttTreeTable treetable;

    /** Pointer on graphic area */
    private ChartComponentBase area = null;

    /** Pointer on application */
    private GanttProject appli;

    private Action myLinkTasksAction;

    private Action myUnlinkTasksAction;

    /** The language use */
    private static GanttLanguage language = GanttLanguage.getInstance();

    /** Number of tasks on the tree. */
    private int nbTasks = 0;

    private TreePath dragPath = null;

    private BufferedImage ghostImage = null; // The 'drag image'

    private Point offsetPoint = new Point(); // Where, in the drag image, the

    private final TaskManager myTaskManager;
    private final TaskSelectionManager mySelectionManager;

    private final GPAction myIndentAction = new TaskIndentAction();

    private final GPAction myUnindentAction = new TaskUnindentAction();

    private final GPAction myMoveUpAction = new TaskMoveUpAction();

    private final GPAction myMoveDownAction = new TaskMoveDownAction();

    private final Action myNewTaskAction;

    private final Action myDeleteAction;

    private boolean isOnTaskSelectionEventProcessing;

    public GanttTree2(final GanttProject app, TaskManager taskManager,
            TaskSelectionManager selectionManager, final UIFacade uiFacade) {

        super(new BorderLayout());
        myUIFacade = uiFacade;

        myTaskManager = taskManager;
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskModelReset() {
                clearTree();
            }
        });
        mySelectionManager = selectionManager;
        mySelectionManager.addSelectionListener(new TaskSelectionManager.Listener() {
            @Override
            public void userInputConsumerChanged(Object newConsumer) {
            }
            @Override
            public void selectionChanged(List<Task> currentSelection) {
                onTaskSelectionChanged(currentSelection);
            }
        });
        this.appli = app;
        
        // Create Actions
        myTaskPropertiesAction = new TaskPropertiesAction(app.getProject(), selectionManager, uiFacade);
        myDeleteAction = new DeleteTasksAction(taskManager, selectionManager, uiFacade, this);
        myNewTaskAction = new NewTaskAction(app.getProject(), getUndoManager());

        // Create the root node
        initRootNode();

        treeModel = new GanttTreeTableModel(rootNode, app.getProject().getTaskCustomColumnManager());
        treeModel.addTreeModelListener(new GanttTreeModelListener());
        // Create the JTree
        treetable = new GanttTreeTable(app.getProject(), uiFacade, treeModel);

        treetable.setupActionMaps(myMoveUpAction, myMoveDownAction, myIndentAction, myUnindentAction, myNewTaskAction,
            getAppFrame().getCutAction(), getAppFrame().getCopyAction(), getAppFrame().getPasteAction(),
            getTaskPropertiesAction(), myDeleteAction);

        treetable.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_DOWN_MASK), "cutTask");
        treetable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (false == treetable.getTable().isEditing()) {
                    app.keyPressed(e);
                }
            }
        }); // callback for keyboard pressed
        treetable.getTree().addTreeSelectionListener(
                new TreeSelectionListener() {
                    public void valueChanged(TreeSelectionEvent e) {
                        if (isOnTaskSelectionEventProcessing) {
                            return;
                        }
                        List<Task> selectedTasks = new ArrayList<Task>();
                        TaskNode tn[] = getSelectedTaskNodes();
                        if (tn != null) {
                            for (TaskNode taskNode : tn) {
                                selectedTasks.add((Task)taskNode.getUserObject());
                            }
                        }
                        getTaskSelectionManager().clear();
                        for (Task t : selectedTasks) {
                            getTaskSelectionManager().addTask(t);
                        }
                    }
                });

        treetable.setBackground(new Color(1.0f, 1.0f, 1.0f));
        treetable.getTree().addTreeExpansionListener(
                new GanttTreeExpansionListener());

        ToolTipManager.sharedInstance().registerComponent(treetable);

        treetable.insertWithLeftyScrollBar(this);
        mySelectionManager.addSelectionListener(new Listener() {
            public void selectionChanged(List<Task> currentSelection) {
            }
            public void userInputConsumerChanged(Object newConsumer) {
                if (treetable.getTable().isEditing()) {
                    treetable.getTable().editingStopped(new ChangeEvent(treetable.getTreeTable()));
                }
            }
        });
        treetable.getTree().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                mySelectionManager.setUserInputConsumer(this);
            }
        });
        // A listener on mouse click (menu)
        MouseListener ml = new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopupTrigger(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handlePopupTrigger(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    TreePath selPath = treetable.getTreeTable().getPathForLocation(e.getX(), e.getY());
                    if (selPath != null) {
                        e.consume();
                        appli.propertiesTask();
                    }
                } else  {
                    handlePopupTrigger(e);
                }
            }

            private void handlePopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    TreePath selPath = treetable.getTreeTable().getPathForLocation(e.getX(), e.getY());
                    if (selPath == null) {
                        return;
                    }
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    Task task = (Task) treeNode.getUserObject();
                    if (!getTaskSelectionManager().isTaskSelected(task)) {
                        getTaskSelectionManager().clear();
                        getTaskSelectionManager().addTask(task);
                    }
                    createPopupMenu(e.getX(), e.getY());
                    e.consume();
                }
            }
        };
        treetable.addMouseListener(ml);
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(treetable,
                DnDConstants.ACTION_COPY_OR_MOVE, this);
        dragSource.addDragSourceListener(this);
        DropTarget dropTarget = new DropTarget(treetable,
                new GanttTreeDropListener());
        dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

        getTreeTable().setToolTipText("aze");
        getTreeTable().getTreeTable().setToolTipText("rty");
    }

    private TaskSelectionManager getTaskSelectionManager() {
        return mySelectionManager;
    }

    private UIFacade getUiFacade() {
        return myUIFacade;
    }

    private GanttProject getAppFrame() {
        return appli;
    }

    /**
     * Edits the <code>t</code> task name in the treetable.
     */
    public void setEditingTask(Task t) {
        treetable.getTreeTable().editingStopped(new ChangeEvent(treetable.getTreeTable()));

        TaskSelectionManager taskSelectionManager = getTaskSelectionManager();
        taskSelectionManager.clear();
        taskSelectionManager.addTask(t);

        treetable.editSelectedTask();
        treetable.centerViewOnSelectedCell();
    }

    public void stopEditing() {
        treetable.getTable().editingCanceled(
                new ChangeEvent(treetable.getTreeTable()));
        treetable.getTreeTable().editingCanceled(
                new ChangeEvent(treetable.getTreeTable()));
    }

    public void changeLanguage(GanttLanguage ganttLanguage) {
        this.language = ganttLanguage;
        //this.treetable.changeLanguage(language);
    }

    private void initRootNode() {
        getTaskManager().getRootTask().setName("root");
        rootNode = new TaskNode(getTaskManager().getRootTask());
    }

    Action[] getPopupMenuActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new NewTaskAction(appli, appli.getUndoManager()));
        if (!getTaskSelectionManager().getSelectedTasks().isEmpty()) {
            actions.add(getTaskPropertiesAction());
            actions.add(getDeleteTasksAction());
            actions.add(null);
            actions.add(myIndentAction);
            actions.add(myUnindentAction);
            actions.add(getMoveUpAction());
            actions.add(getMoveDownAction());
            actions.add(null);
            actions.add(getUnlinkTasksAction());
            actions.add(getLinkTasksAction());
            actions.add(null);
            actions.add(appli.getCutAction());
            actions.add(appli.getCopyAction());
            actions.add(appli.getPasteAction());
        }
        return actions.toArray(new Action[0]);
    }

    /** Create a popup menu when mouse click */
    private void createPopupMenu(int x, int y) {
        Action[] popupMenuActions = getPopupMenuActions();
        JScrollBar vbar = treetable.getVerticalScrollBar();
        myUIFacade.showPopupMenu(this, popupMenuActions,
                x - treetable.getHorizontalScrollBar().getValue()
                    + (vbar.isVisible() ? vbar.getWidth() : 0),
                y - vbar.getValue() + treetable.getTable().getTableHeader().getHeight());
    }

    /** Change graphic part */
    public void setGraphicArea(ChartComponentBase area) {
        this.area = area;
    }

    /** add an object with the expand information */
    DefaultMutableTreeNode addObjectWithExpand(Object child,
            DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode childNode = new TaskNode((Task) child);

        if (parent == null) {
            parent = rootNode;
        }

        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
        //forwardScheduling();

        // Task task = (Task) (childNode.getUserObject());
        boolean res = true;

        // test for expansion
        while (parent != null) {
            Task taskFather = (Task) (parent.getUserObject());
            if (!taskFather.getExpand()) {
                res = false;
                break;
            }
            parent = (DefaultMutableTreeNode) (parent.getParent());
        }

        treetable.getTree().scrollPathToVisible(
                new TreePath(childNode.getPath()));
        if (!res && parent != null) {
            treetable.getTree().collapsePath(new TreePath(parent.getPath()));
        } //else {
        //    task.setExpand(false);
        // }

        nbTasks++;
        appli.refreshProjectInfos();

        return childNode;
    }

    /** Add a sub task. */
    TaskNode addObject(Object child, MutableTreeNode parent, int index) {
        TaskNode childNode = new TaskNode((Task) child);

        if (parent == null)
            parent = rootNode;

        // GanttTask tmpTask = (GanttTask)(childNode.getUserObject());
        // tmpTask.indentID((String)(((GanttTask)(parent.getUserObject())).getID()));

        treeModel.insertNodeInto(childNode, parent, index == -1 ? parent
                .getChildCount() : index);

        treetable.getTree().scrollPathToVisible(
                new TreePath(childNode.getPath()));

        nbTasks++;
        appli.refreshProjectInfos();

        return childNode;
    }

    /** @return the selected task */
    private GanttTask getSelectedTask() {
        DefaultMutableTreeNode node = getSelectedTaskNode();
        if (node == null)
            return null;
        return (GanttTask) (node.getUserObject());
    }

    /** @return the selected node */
    DefaultMutableTreeNode getSelectedNode() {
        TreePath currentSelection = treetable.getTree().getSelectionPath();
        if (currentSelection == null) {
            return null;
        }
        DefaultMutableTreeNode dmtnselected = (DefaultMutableTreeNode) currentSelection
                .getLastPathComponent();
        return dmtnselected;
    }

    /** @return the selected node */
    private DefaultMutableTreeNode getSelectedTaskNode() {
        TreePath currentSelection = treetable.getTree().getSelectionPath();
        if (currentSelection == null
                || !(currentSelection.getLastPathComponent() instanceof TaskNode)) {
            return null;
        }
        DefaultMutableTreeNode dmtnselected = (DefaultMutableTreeNode) currentSelection
                .getLastPathComponent();
        return dmtnselected;
    }

    private TaskNode[] getSelectedTaskNodes() {
        TreePath[] currentSelection = treetable.getTree().getSelectionPaths();

        if (currentSelection == null || currentSelection.length == 0)
            return null;

        DefaultMutableTreeNode[] dmtnselected = new DefaultMutableTreeNode[currentSelection.length];
        for (int i = 0; i < currentSelection.length; i++)
            dmtnselected[i] = (DefaultMutableTreeNode) currentSelection[i]
                    .getLastPathComponent();

        TaskNode[] res = getOnlyTaskNodes(dmtnselected);

        return res;
    }

    /** @return the list of the selected nodes. */
    public DefaultMutableTreeNode[] getSelectedNodes() {
        TreePath[] currentSelection = treetable.getTree().getSelectionPaths();

        if (currentSelection == null || currentSelection.length == 0) {
            // no elements are selected
            return null;
        }

        DefaultMutableTreeNode[] dmtnselected = new DefaultMutableTreeNode[currentSelection.length];
        for (int i = 0; i < currentSelection.length; i++)
            dmtnselected[i] = (DefaultMutableTreeNode) currentSelection[i]
                    .getLastPathComponent();

        DefaultMutableTreeNode[] res = dmtnselected;

        return res;
    }

    private TaskNode[] getOnlyTaskNodes(DefaultMutableTreeNode[] array) {
        List<DefaultMutableTreeNode> resAsList = new ArrayList<DefaultMutableTreeNode>();
        for (int i = 0; i < array.length; i++) {
            DefaultMutableTreeNode next = array[i];
            if (next instanceof TaskNode)
                resAsList.add(next);
        }
        return resAsList.toArray(new TaskNode[0]);
    }

    /** @return the DefaultMutableTreeNode with the name name. */
    private DefaultMutableTreeNode getNode(int id /* String name */) {
        DefaultMutableTreeNode res, base;
        base = (DefaultMutableTreeNode) treetable.getTreeTableModel().getRoot();
        Enumeration<DefaultMutableTreeNode> e = base.preorderEnumeration();
        while (e.hasMoreElements()) {
            res = e.nextElement();
            if (res instanceof TaskNode) {
                if (((Task) res.getUserObject()).getTaskID() == id) {
                    return res;
                }
            }
        }
        return null;
    }

    static List<Task> convertNodesListToItemList(List<DefaultMutableTreeNode> nodesList) {
        List<Task> res = new ArrayList<Task>(nodesList.size());
        Iterator<DefaultMutableTreeNode> itNodes = nodesList.iterator();
        while (itNodes.hasNext()) {
            res.add((Task) itNodes.next().getUserObject());
        }
        return res;
    }

    /** @return an ArrayList with all tasks. */
    ArrayList<TaskNode> getAllTasks() {
        ArrayList<TaskNode> res = new ArrayList<TaskNode>();
        Enumeration<TreeNode> enumeration = rootNode.preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            Object o = enumeration.nextElement();
            if (o instanceof TaskNode) {
                res.add((TaskNode) o);
            }
        }
        return res;
    }

    List<DefaultMutableTreeNode> getAllVisibleNodes() {
        List<DefaultMutableTreeNode> res = new ArrayList<DefaultMutableTreeNode>();
        Enumeration<TreeNode> enumeration = rootNode.preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode o = (DefaultMutableTreeNode) enumeration
                    .nextElement();
            if (getTreeTable().getTree().isVisible(new TreePath(o.getPath())))
                res.add(o);
        }
        return res;
    }

    /** @return all sub tasks for the tree node base */
    private ArrayList<TaskNode> getAllChildTasks(DefaultMutableTreeNode base) {
        ArrayList<TaskNode> res = new ArrayList<TaskNode>();
        if (base == null || !(base instanceof TaskNode)) {
            return res;
        }
        Enumeration e = base.children();
        while (e.hasMoreElements()) {
            Object next = e.nextElement();
            if (next instanceof TaskNode) {
                res.add((TaskNode) next);
            }
        }
        return res;
    }

    /** Removes currentNode */
    public void removeCurrentNode(DefaultMutableTreeNode currentNode) {
        TreeNode parent = currentNode.getParent();
        getTaskManager().deleteTask((Task) currentNode.getUserObject());
        if (parent != null) {
            treeModel.removeNodeFromParent(currentNode);
            nbTasks--;
            appli.refreshProjectInfos();
        }
    }

    /** Clear the JTree. */
    private void clearTree() {
        // expand.clear();
        rootNode.removeAllChildren();
        initRootNode();
        treeModel.setRoot(rootNode);
        treeModel.reload();
        nbTasks = 0;
    }

    private void selectTasks(List<Task> tasksList) {
        getTaskSelectionManager().clear();
        for (Task t : tasksList) {
            getTaskSelectionManager().addTask(t);
        }
    }

    private DefaultMutableTreeNode getTreeNode(Task t) {
        for (Enumeration<TreeNode> nodes = rootNode.preorderEnumeration(); nodes.hasMoreElements();) {
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) nodes.nextElement();
            if (!(nextNode instanceof TaskNode)) {
                continue;
            }
            if (nextNode.getUserObject().equals(t)) {
                return nextNode;
            }
        }
        return null;
    }

    private void onTaskSelectionChanged(List<Task> tasks) {
        isOnTaskSelectionEventProcessing = true;
        List<TreePath> paths = new ArrayList<TreePath>();
        for (Task t : tasks) {
            DefaultMutableTreeNode treeNode = getTreeNode(t);
            assert treeNode != null;
            paths.add(new TreePath(treeNode.getPath()));
        }
        getTreeTable().getTree().getSelectionModel().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
        isOnTaskSelectionEventProcessing = false;
    }

    /** @return the mother task. */
    DefaultMutableTreeNode getFatherNode(Task node) {
        if (node == null) {
            return null;
        }
        DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) getNode(node
                .getTaskID());
        if (tmp == null) {
            return null;
        }

        return (DefaultMutableTreeNode) tmp.getParent();
    }

    /** @return the mother task. */
    private DefaultMutableTreeNode getFatherNode(DefaultMutableTreeNode node) {
        if (node == null) {
            return null;
        }
        return (DefaultMutableTreeNode) node.getParent();
    }

    /** Return the JTree. */
    JTree getJTree() {
        return treetable.getTree();
    }

    JTable getTable() {
        return treetable.getTable();
    }

    public GanttTreeTable getTreeTable() {
        return treetable;
    }

    /** @return the root node */
    public DefaultMutableTreeNode getRoot() {
        return rootNode;
    }

    /** Function to move the selected tasks up */
    private void upCurrentNodes() {
        final DefaultMutableTreeNode[] cdmtn = getSelectedNodes();
        if (cdmtn == null) {
            myUIFacade.setStatusText(language.getText("msg21"));
            return;
        }
        final GanttTree2 gt2 = this;
        appli.getUndoManager().undoableEdit("Up", new Runnable() {
            public void run() {
                for (int i = 0; i < cdmtn.length; i++) {
                    DefaultMutableTreeNode father = gt2.getFatherNode(cdmtn[i]);
                    int index = father.getIndex((TreeNode) cdmtn[i]);

                    index--;

                    Task task = (Task) cdmtn[i].getUserObject();

                    if (index >= 0) {
                        DefaultMutableTreeNode[] child = new DefaultMutableTreeNode[cdmtn[i]
                                .getChildCount()];

                        if (task.getExpand()) {
                            for (int j = 0; j < cdmtn[i].getChildCount(); j++) {
                                child[j] = (DefaultMutableTreeNode) cdmtn[i]
                                        .getChildAt(j);
                            }

                            for (int j = 0; j < child.length; j++) {
                                child[j].removeFromParent();
                                treeModel.nodesWereRemoved(cdmtn[i],
                                        new int[] { 0 },
                                        new Object[] { child });
                            }
                        }

                        cdmtn[i].removeFromParent();
                        treeModel.nodesWereRemoved(father,
                                new int[] { index + 1 },
                                new Object[] { cdmtn });

                        father.insert(cdmtn[i], index);
                        treeModel.nodesWereInserted(father, new int[] { index });

                        if (task.getExpand()) {
                            for (int j = 0; j < child.length; j++) {
                                cdmtn[i].insert(child[j], j);
                                treeModel.nodesWereInserted(cdmtn[i], new int[] { j });
                            }
                        }
                        forwardScheduling();
                    }
                }
            }
        });

        //treetable.getTree().setSelectionPaths(selectedPaths);

        appli.setAskForSave(true);
        area.repaint();
    }

    /** Function to move the selected tasks down */
    private void downCurrentNodes() {

        final DefaultMutableTreeNode[] cdmtn = getSelectedNodes();
        if (cdmtn == null) {
            myUIFacade.setStatusText(language.getText("msg21"));
            return;
        }

        //final TreePath[] selectedPaths = new TreePath[cdmtn.length];

        // Parse in reverse mode because tasks are sorted from top to bottom.
        // appli.setQuickSave (false);
        final GanttTree2 gt2 = this;
        appli.getUndoManager().undoableEdit("Down", new Runnable() {
            public void run() {
                for (int i = cdmtn.length - 1; i >= 0; i--) {
                    DefaultMutableTreeNode father = gt2.getFatherNode(cdmtn[i]);
                    int index = father.getIndex((TreeNode) cdmtn[i]);
                    index++;

                    Task task = (Task) cdmtn[i].getUserObject();

                    // New position
                    if (index < father.getChildCount()) {
                        DefaultMutableTreeNode[] child = new DefaultMutableTreeNode[cdmtn[i].getChildCount()];

                        if (task.getExpand()) {
                            for (int j = 0; j < cdmtn[i].getChildCount(); j++) {
                                child[j] = (DefaultMutableTreeNode) cdmtn[i].getChildAt(j);
                            }

                            for (int j = 0; j < child.length; j++) {
                                child[j].removeFromParent();
                                treeModel.nodesWereRemoved(cdmtn[i],
                                        new int[] { 0 },
                                        new Object[] { child });
                            }
                        }

                        cdmtn[i].removeFromParent();
                        treeModel.nodesWereRemoved(father,
                                new int[] { index - 1 },
                                new Object[] { cdmtn });

                        father.insert(cdmtn[i], index);
                        treeModel.nodesWereInserted(father, new int[] {index});

                        if (task.getExpand()) {
                            for (int j = 0; j < child.length; j++) {
                                cdmtn[i].insert(child[j], j);
                                treeModel.nodesWereInserted(cdmtn[i], new int[] { j });
                            }
                        }

                        forwardScheduling();
                    }
                }
            }
        });

        appli.setAskForSave(true);
        area.repaint();
    }

    /**
     * Indent several nodes that are selected. Based on the IndentCurrentNode
     * method.
     */
    private void indentCurrentNodes() {

        final DefaultMutableTreeNode[] cdmtn = getSelectedTaskNodes();
        if (cdmtn == null) {
            myUIFacade.setStatusText(language.getText("msg21"));
            return;
        }
        getUndoManager().undoableEdit("Indent", new Runnable() {
            public void run() {
                for (int i = 0; i < cdmtn.length; i++) {
                    // Where is my nearest sibling in ascending order ?
                    DefaultMutableTreeNode newFather = cdmtn[i].getPreviousSibling();
                    // If there is no more indentation possible we must stop
                    if (!(newFather instanceof TaskNode)) {
                        continue;
                    }
                    if (cdmtn[i] instanceof TaskNode && newFather instanceof TaskNode) {
                        Task nextTask = (Task) cdmtn[i].getUserObject();
                        Task container = (Task) newFather.getUserObject();
                        if (!getTaskManager().getDependencyCollection().canCreateDependency(container, nextTask)) {
                            continue;
                        }
                        getTaskManager().getTaskHierarchy().move(nextTask, container);
                    }
                }
                area.repaint();
                appli.repaint2();
            }
        });
    }

    /**
     * Method to dedent selected task this will change the parent child
     * relationship. This Code is based on the UP/DOWN Coder I found in here
     * barmeier
     * <br/><br/>
     * Unindent the selected nodes. */
    private void dedentCurrentNodes() {
        final DefaultMutableTreeNode[] cdmtn = getSelectedTaskNodes();
        if (cdmtn == null) {
            myUIFacade.setStatusText(language.getText("msg21"));
            return;
        }
        final GanttTree2 gt2 = this;
        getUndoManager().undoableEdit("Dedent", new Runnable() {
            public void run() {
                TreePath[] selectedPaths = new TreePath[cdmtn.length];

                // Information about previous node is needed to determine if current node had sibling that was moved.
                DefaultMutableTreeNode previousFather = new DefaultMutableTreeNode();
                DefaultMutableTreeNode father = new DefaultMutableTreeNode();

                HashSet<Task> targetContainers = new HashSet<Task>();
                for (int i = 0; i < cdmtn.length; i++) {

                    // We use information about previous father to determine new index of the node in the tree.
                    if (i > 0) {
                        previousFather = father;
                    }
                    father = gt2.getFatherNode(cdmtn[i]);

                    // Getting the fathers father !? The grandpa I think  :)
                    DefaultMutableTreeNode newFather = gt2.getFatherNode(father);
                    // If no grandpa is available we must stop.
                    if (newFather == null) {
                        return;
                    }

                    int oldIndex = father.getIndex((TreeNode) cdmtn[i]);

                    cdmtn[i].removeFromParent();
                    treeModel.nodesWereRemoved(father, new int[] { oldIndex }, new Object[] { cdmtn });

                    targetContainers.add((Task) father.getUserObject());
                    // If node and previous node were siblings add current node after its previous sibling
                    int newIndex;
                    if (i > 0 && father.equals(previousFather) ) {
                        newIndex = newFather.getIndex(cdmtn[i-1]) + 1;
                    } else {
                        newIndex = newFather.getIndex(father) + 1;
                    }

                    treeModel.insertNodeInto(cdmtn[i], newFather, newIndex);

                    // Select again this node
                    TreeNode[] treepath = cdmtn[i].getPath();
                    TreePath path = new TreePath(treepath);
                    // tree.setSelectionPath(path);
                    selectedPaths[i] = path;

                    // refresh the father date
                    // Task current = (Task)(cdmtn[i].getUserObject());
                    // refreshAllFather(current.toString());

                    expandRefresh(cdmtn[i]);

                    if (father.getChildCount() == 0)
                        ((Task) father.getUserObject()).setProjectTask(false);
                }
                getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(
                        targetContainers.toArray(new Task[0]));
                forwardScheduling();
                treetable.getTree().setSelectionPaths(selectedPaths);

                area.repaint();

                // appli.setQuickSave (true);
                // appli.quickSave ("Dedent");
            }
        });
    }

    /** Refresh the expansion (recursive function) */
    private void expandRefresh(DefaultMutableTreeNode moved) {
        if (moved instanceof TaskNode) {
            Task movedTask = (Task) moved.getUserObject();
            // if (expand.contains(new Integer(movedTask.getTaskID()))) {
            if (movedTask.getExpand()) {
                treetable.getTree().expandPath(new TreePath(moved.getPath()));
            }

            Enumeration children = moved.children();
            while (children.hasMoreElements()) {
                expandRefresh((DefaultMutableTreeNode) children.nextElement());
            }
        }
    }

    private final class TaskMoveDownAction extends GPAction {
        public TaskMoveDownAction() {
            super("task.movedown");
        }

        @Override
        protected String getIconFilePrefix() {
            return "down_";
        }

        public void actionPerformed(ActionEvent e) {
            downCurrentNodes();
        }

        @Override
        protected String getLocalizedName() {
            return getI18n("downTask");
        }
    }

    private final class TaskMoveUpAction extends GPAction {
        public TaskMoveUpAction() {
            super("task.moveup");
        }

        @Override
        protected String getIconFilePrefix() {
            return "up_";
        }

        public void actionPerformed(ActionEvent e) {
            upCurrentNodes();
        }

        @Override
        protected String getLocalizedName() {
            return getI18n("upTask");
        }
    }

    private final class TaskUnindentAction extends GPAction {
        TaskUnindentAction() {
            super("task.unindent");
        }

        @Override
        protected String getIconFilePrefix() {
            return "unindent_";
        }

        public void actionPerformed(ActionEvent e) {
            dedentCurrentNodes();
        }

        @Override
        protected String getLocalizedName() {
            return getI18n("dedentTask");
        }
    }

    private final class TaskIndentAction extends GPAction {
        TaskIndentAction() {
            super("task.indent");
        }

        @Override
        protected String getIconFilePrefix() {
            return "indent_";
        }

        public void actionPerformed(ActionEvent e) {
            indentCurrentNodes();
        }

        @Override
        protected String getLocalizedName() {
            return getI18n("indentTask");
        }
    }

    /**
     * Class for expansion and collapse of node
     */
    private class GanttTreeExpansionListener implements TreeExpansionListener {
        public void treeExpanded(TreeExpansionEvent e) {
            if (area != null) {
                area.repaint();
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                    .getLastPathComponent());
            Task task = (Task) node.getUserObject();
            task.setExpand(true);
            appli.setAskForSave(true);
        }

        public void treeCollapsed(TreeExpansionEvent e) {
            if (area != null) {
                area.repaint();
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getPath()
                    .getLastPathComponent());
            Task task = (Task) node.getUserObject();

            task.setExpand(false);
            appli.setAskForSave(true);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Listener to generate modification on the model
     */
    private class GanttTreeModelListener implements TreeModelListener {
        /** modify a node */
        public void treeNodesChanged(TreeModelEvent e) {
            if (area != null) {
                area.repaint();
            }
        }

        /** Insert a new node. */
        public void treeNodesInserted(TreeModelEvent e) {
            if (area != null)
                area.repaint();
        }

        /** Delete a node. */
        public void treeNodesRemoved(TreeModelEvent e) {
            if (area != null) {
                area.repaint();
            }
        }

        /** Structure change. */
        public void treeStructureChanged(TreeModelEvent e) {
            if (area != null) {
                area.repaint();
            }
        }
    }

    private class GanttTreeDropListener implements DropTargetListener {
        private TreePath lastPath = null;

        private Rectangle2D cueLineRect = new Rectangle2D.Float();

        private Rectangle2D ghostImageRect = new Rectangle2D.Float();

        private Color cueLineColor;

        private Point lastEventPoint = new Point();

        private Timer hoverTimer;

        public GanttTreeDropListener() {
            cueLineColor = new Color(SystemColor.controlShadow.getRed(),
                    SystemColor.controlShadow.getGreen(),
                    SystemColor.controlShadow.getBlue(), 64);

            // Set up a hover timer, so that a node will be automatically
            // expanded or collapsed
            // if the user lingers on it for more than a short time
            hoverTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!treetable.getTree().isExpanded(lastPath)) {
                        treetable.getTree().expandPath(lastPath);
                    }
                }
            });
            // Set timer to one-shot mode - it will be restarted when the
            // cursor is over a new node
            hoverTimer.setRepeats(false);
        }

        public void dragEnter(DropTargetDragEvent dtde) {
            if (ghostImage == null) {
                // In case if you drag a file from out and it's not an
                // acceptable, and it can crash if the image is null
                ghostImage = new BufferedImage(1, 1,
                        BufferedImage.TYPE_INT_ARGB_PRE);
            }
            if (!isDragAcceptable(dtde)) {
                dtde.rejectDrag();
            } else {
                dtde.acceptDrag(dtde.getDropAction());
            }
        }

        public void dragOver(DropTargetDragEvent dtde) {
            if (!isDragAcceptable(dtde)) {
                dtde.rejectDrag();
            } else {
                dtde.acceptDrag(dtde.getDropAction());
            }

            // Even if the mouse is not moving, this method is still invoked
            // 10 times per second
            Point pt = dtde.getLocation();
            if (pt.equals(lastEventPoint)) {
                return;
            }

            lastEventPoint = pt;

            Graphics2D g2 = (Graphics2D) treetable.getGraphics();

            // If a drag image is not supported by the platform, then draw our
            // own drag image
            if (!DragSource.isDragImageSupported()) {
                // Rub out the last ghost image and cue line
                treetable.paintImmediately(ghostImageRect.getBounds());
                // And remember where we are about to draw the new ghost image
                ghostImageRect.setRect(pt.x - offsetPoint.x, pt.y
                        - offsetPoint.y, ghostImage.getWidth(), ghostImage
                        .getHeight());
                g2.drawImage(ghostImage, AffineTransform.getTranslateInstance(
                        ghostImageRect.getX(), ghostImageRect.getY()), null);
            } else {
                // Just rub out the last cue line
                treetable.paintImmediately(cueLineRect.getBounds());
            }

            TreePath path = treetable.getTree().getClosestPathForLocation(pt.x,
                    pt.y);
            if (!(path == lastPath)) {
                lastPath = path;
                hoverTimer.restart();
            }

            // In any case draw (over the ghost image if necessary) a cue line
            // indicating where a drop will occur
            Rectangle raPath = treetable.getTree().getPathBounds(path);
            if (raPath == null)
                raPath = new Rectangle(1, 1);
            cueLineRect.setRect(0, raPath.y + (int) raPath.getHeight(),
                    getWidth(), 2);

            g2.setColor(cueLineColor);
            g2.fill(cueLineRect);

            // And include the cue line in the area to be rubbed out next time
            ghostImageRect = ghostImageRect.createUnion(cueLineRect);

        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
            if (!isDragAcceptable(dtde)) {
                dtde.rejectDrag();
            } else {
                dtde.acceptDrag(dtde.getDropAction());
            }
        }

        public void drop(DropTargetDropEvent dtde) {
            if (!isDropAcceptable(dtde)) {
                dtde.rejectDrop();
                return;
            }

            // Prevent hover timer from doing an unwanted expandPath or
            // collapsePath
            hoverTimer.stop();

            dtde.acceptDrop(dtde.getDropAction());

            Transferable transferable = dtde.getTransferable();

            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                DataFlavor flavor = flavors[i];
                if (flavor
                        .isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)) {
                    try {
                        Point pt = dtde.getLocation();
                        DefaultMutableTreeNode target = (DefaultMutableTreeNode) treetable
                                .getTree()
                                .getClosestPathForLocation(pt.x, pt.y)
                                .getLastPathComponent();
                        TreePath pathSource = (TreePath) transferable
                                .getTransferData(flavor);
                        DefaultMutableTreeNode source = (DefaultMutableTreeNode) pathSource
                                .getLastPathComponent();

                        TreeNode sourceFather = source.getParent();
                        int index = sourceFather.getIndex(source);
                        source.removeFromParent();

                        treeModel.nodesWereRemoved(sourceFather,
                                new int[] { index }, new Object[] { source });

                        treeModel.insertNodeInto(source, target, 0);

                        TreePath pathNewChild = new TreePath(
                                ((DefaultMutableTreeNode) pathSource
                                        .getLastPathComponent()).getPath());
                        // Mark this as the selected path in the tree
                        treetable.getTree().setSelectionPath(pathNewChild);

                        // refreshAllFather(source.getUserObject().toString());

                        expandRefresh(source);

                        forwardScheduling();

                        area.repaint();

                        appli.setAskForSave(true);

                        break; // No need to check remaining flavors
                    } catch (UnsupportedFlavorException ufe) {
                        System.out.println(ufe);
                        dtde.dropComplete(false);
                        return;
                    } catch (IOException ioe) {
                        System.out.println(ioe);
                        dtde.dropComplete(false);
                        return;
                    }
                }
            }
            dtde.dropComplete(true);
        }

        public void dragExit(DropTargetEvent dte) {
            if (!DragSource.isDragImageSupported()) {
                repaint(ghostImageRect.getBounds());
            }
            treetable.repaint();
        }

        public boolean isDragAcceptable(DropTargetDragEvent e) {
            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
                return false;
            }

            // Only accept this particular flavor
            if (!e.isDataFlavorSupported(GanttTransferableTreePath.TREEPATH_FLAVOR)) {
                return false;
            }

            // Do not accept dropping on the source node
            Point pt = e.getLocation();
            TreePath path = treetable.getTree().getClosestPathForLocation(pt.x,
                    pt.y);
            if (dragPath.isDescendant(path)) {
                return false;
            }
            if (path.equals(dragPath)) {
                return false;
            }

            // Check if the task is a milestone task
            Task task = (Task) (((DefaultMutableTreeNode) path
                    .getLastPathComponent()).getUserObject());
            if (task.isMilestone()) {
                return false;
            }

            return true;
        }

        public boolean isDropAcceptable(DropTargetDropEvent e) {
            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
                return false;
            }

            // Only accept this particular flavor
            if (!e.isDataFlavorSupported(GanttTransferableTreePath.TREEPATH_FLAVOR)) {
                return false;
            }

            // prohibit dropping onto the drag source
            Point pt = e.getLocation();
            TreePath path = treetable.getTree().getClosestPathForLocation(pt.x,
                    pt.y);
            if (path.equals(dragPath)) {
                return false;
            }
            return true;
        }

    }

    private static class GanttTransferableTreePath implements Transferable {
        // The type of DnD object being dragged...
        public static final DataFlavor TREEPATH_FLAVOR = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType, "TreePath");

        private TreePath _path;

        private DataFlavor[] _flavors = { TREEPATH_FLAVOR };

        /**
         * Constructs a transferable tree path object for the specified path.
         */
        public GanttTransferableTreePath(TreePath path) {
            _path = path;
        }

        // Transferable interface methods...
        public DataFlavor[] getTransferDataFlavors() {
            return _flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return java.util.Arrays.asList(_flavors).contains(flavor);
        }

        public synchronized Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType())) {
                return _path;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    private ArrayList<DefaultMutableTreeNode> cpNodesArrayList;

    private ArrayList<DefaultMutableTreeNode> allNodes;

    private ArrayList<TaskDependency> cpDependencies;

    // private ArrayList copyID;
    // private ArrayList pasteID;

    private Map<Integer, Integer> mapOriginalIDCopyID;

    /** Cut the current selected tree node */
    void cutSelectedNode() {
        final TreePath currentSelection = treetable.getTree()
                .getSelectionPath();
        final DefaultMutableTreeNode[] cdmtn = getSelectedNodes();
        if (currentSelection != null) {
            getUndoManager().undoableEdit("Cut", new Runnable() {
                public void run() {
                    cpNodesArrayList = new ArrayList<DefaultMutableTreeNode>();
                    cpAllDependencies(cdmtn);
                    GanttTask taskFather = null;
                    DefaultMutableTreeNode father = null;
                    DefaultMutableTreeNode current = null;
                    for (int i = 0; i < cdmtn.length; i++) {
                        current = getSelectedTaskNode();
                        if (current != null) {
                            cpNodesArrayList.add(cdmtn[i]);
                            father = getFatherNode(current/* task */);
                            where = father.getIndex(current);
                            removeCurrentNode(current);
                            current.setParent(father);
                            taskFather = (GanttTask) father.getUserObject();
                            AdjustTaskBoundsAlgorithm alg = getTaskManager()
                                    .getAlgorithmCollection()
                                    .getAdjustTaskBoundsAlgorithm();
                            alg.run(taskFather);
                            // taskFather.refreshDateAndAdvancement(this);
                            father.setUserObject(taskFather);
                        }
                    }
                    if (father.getChildCount() == 0) {
                        ((Task) father.getUserObject()).setProjectTask(false);
                    }
                    if (taskFather != null) {
                        getTaskSelectionManager().addTask(taskFather);
                    }
                    area.repaint();
                }
            });
            appli.repaint();
        }
    }

    private int where = -1;

    private final Action myTaskPropertiesAction;

    /** Copy the current selected tree node */
    void copySelectedNode() {
        DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if (selectedNodes != null) {
            DefaultMutableTreeNode[] selectedRoots = findSelectedSubtreeRoots(selectedNodes);
            cpNodesArrayList = new ArrayList<DefaultMutableTreeNode>();
            cpAllDependencies(selectedRoots);
            for (int i = 0; i < selectedRoots.length; i++) {
                cpNodesArrayList.add(selectedNodes[i]);
            }
        }
    }

    private DefaultMutableTreeNode[] findSelectedSubtreeRoots(
            DefaultMutableTreeNode[] selectedNodes) {
        final HashSet<DefaultMutableTreeNode> set = new HashSet<DefaultMutableTreeNode>(
                Arrays.asList(selectedNodes));
        for (int i = 0; i < selectedNodes.length; i++) {
            for (TreeNode parent = selectedNodes[i].getParent();
                    parent != null; parent = parent.getParent()) {
                if (set.contains(parent)) {
                    set.remove(selectedNodes[i]);
                    break;
                }
            }
        }
        return set.toArray(new DefaultMutableTreeNode[set.size()]);
    }

    /** Paste the node and its child node to current selected position */
    void pasteNode() {
        if (cpNodesArrayList != null) {
            getUndoManager().undoableEdit("Paste", new Runnable() {
                public void run() {
                    TaskNode current = (TaskNode) treetable.getTree()
                            .getLastSelectedPathComponent();
                    List<Task> tasksList = new ArrayList<Task>();
                    if (current == null) {
                        current = rootNode;
                    }

                    boolean isAProjectTaskChild = false;
                    DefaultMutableTreeNode father = (DefaultMutableTreeNode) current
                            .getParent();
                    // if the task as a projectTask parent
                    while (father != null) {
                        if (((Task) father.getUserObject()).isProjectTask()) {
                            isAProjectTaskChild = true;
                        }
                        father = (DefaultMutableTreeNode) father.getParent();
                    }
                    mapOriginalIDCopyID = new HashMap<Integer, Integer>();
                    // copyID = new ArrayList ();
                    // pasteID = new ArrayList ();

                    // for(int i=0; i < cpNodesArrayList.size(); i++) {
                    for (int i = cpNodesArrayList.size() - 1; i >= 0; i--) {
                        if (isAProjectTaskChild) {
                            ((Task) ((TaskNode) cpNodesArrayList.get(i))
                                    .getUserObject()).setProjectTask(false);
                        }
                        /*
                         * this will add new custom columns to the newly created task.
                         */
                        TreeNode sel = getSelectedTaskNode();
                        TreeNode parent = null;
                        if (sel != null) {
                            parent = sel.getParent();
                            if (parent != null)
                                where = parent.getIndex(sel);
                        }
                        tasksList.add((Task) insertClonedNode(
                                current == rootNode ? current : (DefaultMutableTreeNode) current.getParent(),
                                cpNodesArrayList.get(i), where + 1, true)
                                .getUserObject());
                        nbTasks++;
                    }
                    if (cpDependencies != null) {
                        for (int i = 0; i < cpDependencies.size(); i++) {
                            TaskDependency td = cpDependencies.get(i);
                            Task dependee = td.getDependee();
                            Task dependant = td.getDependant();
                            TaskDependencyConstraint constraint = td.getConstraint();
                            for (int j = 0; j < allNodes.size(); j++) {
                                for (int k = 0; k < allNodes.size(); k++) {
                                    if ((dependant.equals((allNodes.get(j).getUserObject())))
                                            && (dependee.equals((allNodes.get(k).getUserObject())))) {
                                        try {
                                            TaskDependency newDependency = getTaskManager()
                                                    .getDependencyCollection().createDependency(
                                                            getTaskManager().getTask(mapOriginalIDCopyID
                                                                    .get(new Integer(dependant.getTaskID())).intValue()),
                                                            getTaskManager().getTask(mapOriginalIDCopyID
                                                                    .get(new Integer(dependee.getTaskID())).intValue()),
                                                            getTaskManager().createConstraint(constraint.getID()));
                                            newDependency.setDifference(td.getDifference());
                                            newDependency.setHardness(td.getHardness());
                                        } catch (TaskDependencyException e) {
                                            myUIFacade.showErrorDialog(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    selectTasks(tasksList);
                }
            });
            appli.refreshProjectInfos();
        }

    }

    /** Insert the cloned node and its children */
    private TaskNode insertClonedNode(DefaultMutableTreeNode parent,
            DefaultMutableTreeNode child, int location, boolean first) {
        if (parent == null) {
            return null; // it is the root node
        }
        if (first) {
            GanttTask _t = (GanttTask) (parent.getUserObject());
            if (_t.isMilestone()) {
                _t.setMilestone(false);
                GanttTask _c = (GanttTask) (child.getUserObject());
                _t.setLength(_c.getLength());
                _t.setStart(_c.getStart());
            }
        }

        GanttTask originalTask = (GanttTask) child.getUserObject();
        GanttTask newTask = originalTask.Clone();

        newTask.setName((first ? language.getText("copy2") + "_" : "")
                + newTask.toString());

        TaskManagerImpl tmi = (TaskManagerImpl) getTaskManager();
        newTask.setTaskID(tmi.getMaxID() + 1);
        mapOriginalIDCopyID.put(new Integer(originalTask.getTaskID()),
                new Integer(newTask.getTaskID()));

        getTaskManager().registerTask(newTask);

        DefaultMutableTreeNode cloneChildNode = new TaskNode(newTask);

        for (int i = 0; i < child.getChildCount(); i++) {
            insertClonedNode(cloneChildNode, (DefaultMutableTreeNode) child
                    .getChildAt(i), i, false);
        }

        // if(child.getParent() != null)
        // if(!child.getParent().equals(parent))
        // parent = (DefaultMutableTreeNode)parent.getParent();
        // if (parent == null) {
        //     location = 0;
        // }

        if (parent.getChildCount() < location) {
            location = parent.getChildCount();
        }

        treeModel.insertNodeInto(cloneChildNode, parent, location);

        treetable.getTree().scrollPathToVisible(
                new TreePath(cloneChildNode.getPath()));

        // Remove the node from the expand list
        /*
         * int index = expand.indexOf(new
         * Integer(newTask.getTaskID())cloneChildNode.toString()); if (index >=
         * 0) expand.remove(index);
         */
        newTask.setExpand(false);
        return (TaskNode) cloneChildNode;
    }

    private void forwardScheduling() {
        RecalculateTaskScheduleAlgorithm alg = getTaskManager()
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        try {
            alg.run();
        } catch (TaskDependencyException e) {
            myUIFacade.showErrorDialog(e);
        }
    }

    private TaskManager getTaskManager() {
        return myTaskManager;
    }

    public void dragEnter(DragSourceDragEvent dsde) {
    }

    public void dragOver(DragSourceDragEvent dsde) {
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

    public void dragExit(DragSourceEvent dse) {
    }

    public void dragGestureRecognized(DragGestureEvent dge) {

        Point ptDragOrigin = dge.getDragOrigin();
        TreePath path = treetable.getTree().getPathForLocation(ptDragOrigin.x,
                ptDragOrigin.y);
        if (path == null) {
            return;
        }

        // Work out the offset of the drag point from the TreePath bounding
        // rectangle origin
        Rectangle raPath = treetable.getTree().getPathBounds(path);
        offsetPoint.setLocation(ptDragOrigin.x - raPath.x, ptDragOrigin.y
                - raPath.y);

        // Get the cell renderer (which is a JLabel) for the path being dragged
        // JLabel lbl = (JLabel)
        // treetable.getTree().getCellRenderer().getTreeCellRendererComponent
        // (
        // treetable, // tree
        // path.getLastPathComponent(), // value
        // false, // isSelected (dont want a colored background)
        // treetable.getTree().isExpanded(path), // isExpanded
        // ((DefaultTreeTableModel)treetable.getModel()).isLeaf(path.getLastPathComponent()),
        // // isLeaf
        // 0, // row (not important for rendering)
        // false // hasFocus (dont want a focus rectangle)
        // );
        JLabel lbl = new JLabel();
        lbl.setSize((int) raPath.getWidth(), (int) raPath.getHeight()); // <-- The layout manager would normally do this

        // Get a buffered image of the selection for dragging a ghost image
        ghostImage = new BufferedImage((int) raPath.getWidth(), (int) raPath
                .getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = ghostImage.createGraphics();

        // Ask the cell renderer to paint itself into the BufferedImage
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f)); // Make the image ghostlike
        lbl.paint(g2);

        // Now paint a gradient UNDER the ghosted JLabel text (but not under the
        // icon if any)
        // Note: this will need tweaking if your icon is not positioned to the
        // left of the text
        Icon icon = lbl.getIcon();
        int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f)); // Make the gradient ghostlike
        g2.setPaint(new GradientPaint(nStartOfText, 0,
                SystemColor.controlShadow, getWidth(), 0, new Color(255, 255, 255, 0)));
        g2.fillRect(nStartOfText, 0, getWidth(), ghostImage.getHeight());

        g2.dispose();

        treetable.getTree().setSelectionPath(path); // Select this path in the tree

        // Wrap the path being transferred into a Transferable object
        Transferable transferable = new GanttTransferableTreePath(path);

        // Remember the path being dragged (because if it is being moved, we
        // will have to delete it later)
        dragPath = path;

        // We pass our drag image just in case it IS supported by the platform
        dge.startDrag(null, ghostImage, new Point(5, 5), transferable, this);
    }

    private void cpAllDependencies(DefaultMutableTreeNode[] cdmtn) {
        // to get all the dependencies who need to be paste.
        cpDependencies = new ArrayList<TaskDependency>();
        allNodes = new ArrayList<DefaultMutableTreeNode>();
        for (int i = 0; i < cdmtn.length; i++) {
            getAllNodes(cdmtn[i]);
        }
        TaskDependency[] dependencies = getTaskManager()
                .getDependencyCollection().getDependencies();
        for (int i = 0; i < dependencies.length; i++) {
            Task dependant = dependencies[i].getDependant();
            Task dependee = dependencies[i].getDependee();
            for (int j = 0; j < allNodes.size(); j++) {
                for (int k = 0; k < allNodes.size(); k++) {
                    if (((Task) (allNodes.get(j).getUserObject())).equals(dependant)
                            && ((Task) (allNodes.get(k).getUserObject())).equals(dependee)) {
                        cpDependencies.add(dependencies[i]);
                    }
                }
            }
        }
    }

    // public int cpId (int index) {
    // System.out.println("index = " + index + " -> " +
    // ((Integer)pasteID.get(index)).intValue());
    // return ((Integer)pasteID.get(index)).intValue();
    // }

    private void getAllNodes(DefaultMutableTreeNode dmt) {
        // get all the nodes the parent and all his descendance
        if (!allNodes.contains(dmt))
            allNodes.add(dmt);
        for (int i = 0; i < dmt.getChildCount(); i++) {
            getAllNodes((DefaultMutableTreeNode) dmt.getChildAt(i));
        }
    }

    public ArrayList<TaskNode> getProjectTasks() {
        ArrayList<TaskNode> projectTasks = new ArrayList<TaskNode>();
        getProjectTasks(rootNode, projectTasks);
        return projectTasks;
    }

    private void getProjectTasks(TaskNode node, ArrayList<TaskNode> list) {
        ArrayList<TaskNode> childs = getAllChildTasks(node);
        for (TaskNode child : childs) {
            if (((Task) child.getUserObject()).isProjectTask()) {
                list.add(child);
            } else {
                getProjectTasks(child, list);
            }
        }
    }

    public void setDelay(final Task task, final Delay delay) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TaskNode taskNode = (TaskNode) getNode(task.getTaskID());
                if (taskNode != null) {
                    treetable.setDelay(taskNode, delay);
                }
            }
        });
    }

    GanttTreeTableModel getModel() {
        return treeModel;
    }

    private GPUndoManager getUndoManager() {
        return myUIFacade.getUndoManager();
    }

    ////////////////////////////////////////////////////////////////////////
    // TaskTreeUIFacade
    public Component getTreeComponent() {
        return this;
    }

    public Action getIndentAction() {
        return myIndentAction;
    }

    public Action getUnindentAction() {
        return myUnindentAction;
    }

    public Action getMoveUpAction() {
        return myMoveUpAction;
    }

    public Action getMoveDownAction() {
        return myMoveDownAction;
    }

    public void setLinkTasksAction(Action action) {
        myLinkTasksAction = action;
    }

    public Action getLinkTasksAction() {
        return myLinkTasksAction;
    }

    public void setUnlinkTasksAction(Action action) {
        myUnlinkTasksAction = action;
    }

    public Action getUnlinkTasksAction() {
        return myUnlinkTasksAction;
    }

    Action getTaskPropertiesAction() {
        return myTaskPropertiesAction;
    }

    Action getDeleteTasksAction() {
        return myDeleteAction;
    }

    public TableHeaderUIFacade getVisibleFields() {
        return treetable.getVisibleFields();
    }

    public List<Task> getVisibleNodes(VisibleNodesFilter visibleNodesFilter) {
        return visibleNodesFilter.getVisibleNodes(
                getJTree(), getTreeTable().getVerticalScrollBar().getValue(), getHeight(),
                getTreeTable().getRowHeight());
    }
}
