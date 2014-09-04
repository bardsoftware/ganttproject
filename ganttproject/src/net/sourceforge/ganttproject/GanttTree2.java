/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.task.TaskDeleteAction;
import net.sourceforge.ganttproject.action.task.TaskIndentAction;
import net.sourceforge.ganttproject.action.task.TaskLinkAction;
import net.sourceforge.ganttproject.action.task.TaskMoveDownAction;
import net.sourceforge.ganttproject.action.task.TaskMoveUpAction;
import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.action.task.TaskPropertiesAction;
import net.sourceforge.ganttproject.action.task.TaskUnindentAction;
import net.sourceforge.ganttproject.action.task.TaskUnlinkAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.VisibleNodesFilter;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager.Listener;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.util.collect.Pair;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import biz.ganttproject.core.table.ColumnList;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Class that generate the JTree
 */
public class GanttTree2 extends TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> implements
    /*DragSourceListener, DragGestureListener,*/ TaskTreeUIFacade {
  private UIFacade myUIFacade;

  /** Pointer on graphic area */
  private ChartComponentBase area = null;

  // TODO Replace with IGanttProject and facade classes
  /** Pointer of application */
  private final GanttProject myProject;

  private final TaskManager myTaskManager;
  private final TaskSelectionManager mySelectionManager;

  private final GPAction myIndentAction;

  private final GPAction myUnindentAction;

  private final GPAction myMoveUpAction;

  private final GPAction myMoveDownAction;

  private final GPAction myLinkTasksAction;

  private final GPAction myUnlinkTasksAction;

  private boolean isOnTaskSelectionEventProcessing;

  private Highlighter myDragHighlighter;

  private static Runnable createDirtyfier(final GanttProjectBase project) {
    return new Runnable() {
      @Override
      public void run() {
        project.setModified();
      }
    };
  }
  private static Pair<GanttTreeTable, GanttTreeTableModel> createTreeTable(
      IGanttProject project, Runnable dirtyfier, UIFacade uiFacade) {
    GanttTreeTableModel tableModel = new GanttTreeTableModel(project.getTaskManager(),
        project.getTaskCustomColumnManager(), uiFacade, dirtyfier);
    return Pair.create(new GanttTreeTable(project, uiFacade, tableModel), tableModel);
  }

  public GanttTree2(final GanttProject project, TaskManager taskManager, TaskSelectionManager selectionManager,
      final UIFacade uiFacade) {

    super(createTreeTable(project.getProject(), createDirtyfier(project), uiFacade));
    myUIFacade = uiFacade;
    myProject = project;
    myTaskManager = taskManager;
    mySelectionManager = selectionManager;

    myTaskManager.addTaskListener(new TaskListenerAdapter() {
      @Override
      public void taskModelReset() {
        clearTree();
      }

      @Override
      public void taskRemoved(TaskHierarchyEvent e) {
        MutableTreeTableNode node = getNode(e.getTask());
        if (node == null) {
          return;
        }
        TreeNode parent = node.getParent();
        if (parent != null) {
          getTreeModel().removeNodeFromParent(node);
        }
      }

    });
    mySelectionManager.addSelectionListener(new TaskSelectionManager.Listener() {
      @Override
      public void userInputConsumerChanged(Object newConsumer) {
      }

      @Override
      public void selectionChanged(List<Task> currentSelection) {
        onTaskSelectionChanged(currentSelection);
      }
    });

    // Create Actions
    GPAction propertiesAction = new TaskPropertiesAction(project.getProject(), selectionManager, uiFacade);
    GPAction deleteAction = new TaskDeleteAction(taskManager, selectionManager, uiFacade, this);
    GPAction newAction = new TaskNewAction(project.getProject(), uiFacade);

    setArtefactActions(newAction, propertiesAction, deleteAction);
    myLinkTasksAction = new TaskLinkAction(taskManager, selectionManager, uiFacade);
    myUnlinkTasksAction = new TaskUnlinkAction(taskManager, selectionManager, uiFacade);
    myIndentAction = new TaskIndentAction(taskManager, selectionManager, uiFacade, this);
    myUnindentAction = new TaskUnindentAction(taskManager, selectionManager, uiFacade, this);
    myMoveUpAction = new TaskMoveUpAction(taskManager, selectionManager, uiFacade, this);
    myMoveDownAction = new TaskMoveDownAction(taskManager, selectionManager, uiFacade, this);
    getTreeTable().setupActionMaps(myMoveUpAction, myMoveDownAction, myIndentAction, myUnindentAction, newAction,
        myProject.getCutAction(), myProject.getCopyAction(), myProject.getPasteAction(), propertiesAction, deleteAction);
  }

  @Override
  protected void init() {
    getTreeTable().initTreeTable();
    // Create the root node
    initRootNode();


    getTreeTable().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_DOWN_MASK), "cutTask");
    getTreeTable().getTree().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
      }
    });

    getTreeTable().getTree().addTreeExpansionListener(new GanttTreeExpansionListener());

    ToolTipManager.sharedInstance().registerComponent(getTreeTable());

    getTreeTable().insertWithLeftyScrollBar(this);
    mySelectionManager.addSelectionListener(new Listener() {
      @Override
      public void selectionChanged(List<Task> currentSelection) {
      }

      @Override
      public void userInputConsumerChanged(Object newConsumer) {
        if (getTreeTable().getTable().isEditing()) {
          getTreeTable().getTable().editingStopped(new ChangeEvent(getTreeTable().getTreeTable()));
        }
      }
    });
    getTreeTable().getTree().addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        super.focusGained(e);
        mySelectionManager.setUserInputConsumer(this);
      }
    });
    try {
      GanttTreeDropListener dropListener = new GanttTreeDropListener();
      getTreeTable().getDropTarget().addDropTargetListener(dropListener);
      Color selectionBackground = UIManager.getColor("Table.selectionBackground");
      Color dropBackground = new Color(selectionBackground.getRed(), selectionBackground.getGreen(), selectionBackground.getBlue(), 64);
      Color foreground = UIManager.getColor("Table.selectionForeground");
      myDragHighlighter = new ColorHighlighter(dropListener, dropBackground, foreground);
    } catch (TooManyListenersException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  @Override
  protected void handlePopupTrigger(MouseEvent e) {
    if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
      TreePath selPath = getTreeTable().getTreeTable().getPathForLocation(e.getX(), e.getY());
      if (selPath != null) {
        DefaultMutableTreeTableNode treeNode = (DefaultMutableTreeTableNode) selPath.getLastPathComponent();
        Task task = (Task) treeNode.getUserObject();
        if (!getTaskSelectionManager().isTaskSelected(task)) {
          getTaskSelectionManager().clear();
          getTaskSelectionManager().addTask(task);
        }
      }
      createPopupMenu(e.getX(), e.getY());
      e.consume();
    }
  }

  @Override
  protected void onSelectionChanged(List<DefaultMutableTreeTableNode> selection) {
    if (isOnTaskSelectionEventProcessing) {
      return;
    }
    List<Task> selectedTasks = Lists.newArrayList();
    for (DefaultMutableTreeTableNode node : selection) {
      if (node instanceof TaskNode) {
        selectedTasks.add((Task) node.getUserObject());
      }
    }
    // selection paths in Swing are stored in a hashtable
    // and thus come to selection listeners in pretty random order.
    // For correct indent/outdent operations with need
    // to order them the way they are ordered in the tree.
    Collections.sort(selectedTasks, new Comparator<Task>() {
      @Override
      public int compare(Task o1, Task o2) {
        return myTaskManager.getTaskHierarchy().compareDocumentOrder(o1, o2);
      }
    });
    getTaskSelectionManager().clear();
    for (Task t : selectedTasks) {
      getTaskSelectionManager().addTask(t);
    }
  }

  private TaskSelectionManager getTaskSelectionManager() {
    return mySelectionManager;
  }

  /**
   * Edits the <code>t</code> task name in the treetable.
   */
  public void setEditingTask(Task t) {
    getTreeTable().getTreeTable().editingStopped(new ChangeEvent(getTreeTable().getTreeTable()));

    TaskSelectionManager taskSelectionManager = getTaskSelectionManager();
    taskSelectionManager.clear();
    taskSelectionManager.addTask(t);

    getTreeTable().editSelectedTask();
    getTreeTable().centerViewOnSelectedCell();
  }

  public void stopEditing() {
    getTreeTable().getTable().editingCanceled(new ChangeEvent(getTreeTable().getTreeTable()));
    getTreeTable().getTreeTable().editingCanceled(new ChangeEvent(getTreeTable().getTreeTable()));
  }

  private void initRootNode() {
    getRootNode().setUserObject(myTaskManager.getRootTask());
  }

  public Action[] getPopupMenuActions() {
    List<Action> actions = new ArrayList<Action>();
    actions.add(getNewAction());
    if (!getTaskSelectionManager().getSelectedTasks().isEmpty()) {
      actions.add(getPropertiesAction());
      actions.add(null);
      for (AbstractAction a : getTreeActions()) {
        actions.add(a);
      }
      actions.add(null);
      actions.add(myProject.getCutAction());
      actions.add(myProject.getCopyAction());
      actions.add(myProject.getPasteAction());
      actions.add(getDeleteAction());
    }
    return actions.toArray(new Action[0]);
  }

  /** Create a popup menu when mouse click */
  private void createPopupMenu(int x, int y) {
    Action[] popupMenuActions = getPopupMenuActions();
    JScrollBar vbar = getTreeTable().getVerticalScrollBar();
    myUIFacade.showPopupMenu(this, popupMenuActions,
        x - getTreeTable().getHorizontalScrollBar().getValue() + (vbar.isVisible() ? vbar.getWidth() : 0),
        y - vbar.getValue() + getTreeTable().getTable().getTableHeader().getHeight());
  }

  /** Change graphic part */
  public void setGraphicArea(ChartComponentBase area) {
    this.area = area;
  }

  @Override
  public void applyPreservingExpansionState(Task rootTask, Predicate<Task> callable) {
    MutableTreeTableNode rootNode = getNode(rootTask);
    List<MutableTreeTableNode> subtree = TreeUtil.collectSubtree(rootNode);
    Collections.reverse(subtree);
    LinkedHashMap<Task, Boolean> states = Maps.newLinkedHashMap();
    for (MutableTreeTableNode node : subtree) {
      Task t = (Task)node.getUserObject();
      states.put(t, t.getExpand());
    }
    callable.apply(rootTask);
    for (Map.Entry<Task, Boolean> state : states.entrySet()) {
      setExpanded(state.getKey(), state.getValue());
    }
  }

  /** add an object with the expand information */
  DefaultMutableTreeTableNode addObjectWithExpand(Object child, MutableTreeTableNode parent) {
    DefaultMutableTreeTableNode childNode = new TaskNode((Task) child);

    if (parent == null) {
      parent = getRootNode();
    }

    getTreeModel().insertNodeInto(childNode, parent, parent.getChildCount());
    myProject.refreshProjectInformation();

    return childNode;
  }

  static List<Task> convertNodesListToItemList(List<DefaultMutableTreeTableNode> nodesList) {
    List<Task> res = new ArrayList<Task>(nodesList.size());
    Iterator<DefaultMutableTreeTableNode> itNodes = nodesList.iterator();
    while (itNodes.hasNext()) {
      res.add((Task) itNodes.next().getUserObject());
    }
    return res;
  }

  /** @return an ArrayList with all tasks. */
  List<MutableTreeTableNode> getAllTasks() {
    return TreeUtil.collectSubtree(getRootNode());
  }


  /** Clear the JTree. */
  private void clearTree() {
    // expand.clear();
    TreeUtil.removeAllChildren(getRootNode());
    initRootNode();
    getTreeModel().setRoot(getRootNode());
    // getTreeModel().reload();
  }

  @Override
  public void setSelected(Task task, boolean clear) {
    if (clear) {
      clearSelection();
    }
    getTaskSelectionManager().addTask(task);
  }

  @Override
  public void clearSelection() {
    getTaskSelectionManager().clear();
  }

  private void onTaskSelectionChanged(List<Task> tasks) {
    isOnTaskSelectionEventProcessing = true;
    List<TreePath> paths = new ArrayList<TreePath>();
    for (Task t : tasks) {
      if (t == null) {
        GPLogger.getLogger(getClass()).log(Level.SEVERE, "Found null task in the selection. Full selection=" + tasks,
            new NullPointerException());
        continue;
      }
      MutableTreeTableNode treeNode = getNode(t);
      assert treeNode != null : "Failed to find tree node for task=" + t;
      paths.add(TreeUtil.createPath(treeNode));
    }
    getTreeTable().getTreeSelectionModel().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
    isOnTaskSelectionEventProcessing = false;
  }

  /** @return the mother task. */
  public static DefaultMutableTreeTableNode getParentNode(DefaultMutableTreeTableNode node) {
    if (node == null) {
      return null;
    }
    return (DefaultMutableTreeTableNode) node.getParent();
  }

  /** @return the JTree. */
  JXTreeTable getJTree() {
    return getTreeTable();
  }

  JTable getTable() {
    return getTreeTable().getTable();
  }

  /** @return the root node */
  public DefaultMutableTreeTableNode getRoot() {
    return getRootNode();
  }

  /** Refresh the expansion (recursive function) */
  public void expandRefresh(TreeTableNode moved) {
    if (moved instanceof TaskNode) {
      Task movedTask = (Task) moved.getUserObject();
      if (movedTask.getExpand()) {
        getTreeTable().getTree().expandPath(TreeUtil.createPath(moved));
      }
      for (int i = 0; i < moved.getChildCount(); i++) {
        expandRefresh(moved.getChildAt(i));
      }
    }
  }

  /** Class for expansion and collapse of node */
  private class GanttTreeExpansionListener implements TreeExpansionListener {
    @Override
    public void treeExpanded(TreeExpansionEvent e) {
      if (area != null) {
        area.repaint();
      }
      DefaultMutableTreeTableNode node = (DefaultMutableTreeTableNode) (e.getPath().getLastPathComponent());
      Task task = (Task) node.getUserObject();
      task.setExpand(true);
      myProject.setAskForSave(true);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent e) {
      if (area != null) {
        area.repaint();
      }

      DefaultMutableTreeTableNode node = (DefaultMutableTreeTableNode) (e.getPath().getLastPathComponent());
      Task task = (Task) node.getUserObject();

      task.setExpand(false);
      myProject.setAskForSave(true);
    }
  }

  // ////////////////////////////////////////////////////////////////////////////////////////

  private class GanttTreeDropListener extends DropTargetAdapter implements HighlightPredicate {
    private TreePath lastPath = null;
    private Point lastEventPoint = new Point();
    private Timer hoverTimer;
    private int myOverRow = -1;

    public GanttTreeDropListener() {
      // Set up a hover timer, so that a node will be automatically
      // expanded or collapsed if the user lingers on it for more than a
      // short time
      hoverTimer = new Timer(1000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!getTreeTable().getTree().isExpanded(lastPath)) {
            getTreeTable().getTree().expandPath(lastPath);
          }
        }
      });
      // Set timer to one-shot mode - it will be restarted when the
      // cursor is over a new node
      hoverTimer.setRepeats(false);
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
      Point pt = dtde.getLocation();
      if (pt.equals(lastEventPoint)) {
        return;
      }
      lastEventPoint = pt;
      TreePath path = getTreeTable().getTree().getPathForLocation(pt.x, pt.y);
      myOverRow = getTreeTable().getRowForPath(path);
      if (path != lastPath) {
        getTreeTable().removeHighlighter(myDragHighlighter);
        lastPath = path;
        hoverTimer.restart();
        getTreeTable().addHighlighter(myDragHighlighter);
      }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
      super.dragExit(dte);
      myOverRow = -1;
    }

    @Override
    public void drop(DropTargetDropEvent arg0) {
      myOverRow = -1;
    }

    @Override
    public boolean isHighlighted(Component arg0, ComponentAdapter adapter) {
      return myOverRow == adapter.row;
    }
  }
  private AbstractAction[] myTreeActions;

  GanttTreeTableModel getModel() {
    return getTreeModel();
  }

  public void setSelectionPaths(TreePath[] selectedPaths) {
    getTree().getTreeSelectionModel().setSelectionPaths(selectedPaths);
  }

  // //////////////////////////////////////////////////////////////////////
  // TaskTreeUIFacade
  @Override
  public AbstractAction[] getTreeActions() {
    if (myTreeActions == null) {
      myTreeActions = new AbstractAction[] { myUnindentAction, myIndentAction, myMoveUpAction, myMoveDownAction,
          myLinkTasksAction, myUnlinkTasksAction };
    }
    return myTreeActions;
  }

  @Override
  public ColumnList getVisibleFields() {
    return getTreeTable().getVisibleFields();
  }

  public List<Task> getVisibleNodes(VisibleNodesFilter visibleNodesFilter) {
    return visibleNodesFilter.getVisibleNodes(getJTree(), getTreeTable().getVerticalScrollBar().getValue(),
        getHeight(), getTreeTable().getRowHeight());
  }

  @Override
  public void startDefaultEditing(Task modelElement) {
    if (getTable().isEditing()) {
      getTable().getCellEditor().stopCellEditing();
    }
    setEditingTask(modelElement);
  }

  @Override
  protected DefaultMutableTreeTableNode getRootNode() {
    return getTreeModel().getRootNode();
  }

  @Override
  protected Chart getChart() {
    return myUIFacade.getGanttChart();
  }
}
