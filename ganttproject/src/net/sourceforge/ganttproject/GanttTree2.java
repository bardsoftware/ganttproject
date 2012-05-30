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

import java.awt.AlphaComposite;
import java.awt.Color;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.delay.DelayObserver;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
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
import net.sourceforge.ganttproject.util.collect.Pair;

/**
 * Class that generate the JTree
 */
public class GanttTree2 extends TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> implements
    DragSourceListener, DragGestureListener, DelayObserver, TaskTreeUIFacade {
  private UIFacade myUIFacade;

  /** Pointer on graphic area */
  private ChartComponentBase area = null;

  // TODO Replace with IGanttProject and facade classes
  /** Pointer of application */
  private final GanttProject myProject;

  /** The used language */
  private static GanttLanguage language = GanttLanguage.getInstance();

  private TreePath dragPath = null;

  private BufferedImage ghostImage = null; // The 'drag image'

  private Point offsetPoint = new Point(); // Where, in the drag image, the

  private final TaskManager myTaskManager;
  private final TaskSelectionManager mySelectionManager;

  private final GPAction myIndentAction;

  private final GPAction myUnindentAction;

  private final GPAction myMoveUpAction;

  private final GPAction myMoveDownAction;

  private final GPAction myLinkTasksAction;

  private final GPAction myUnlinkTasksAction;

  private boolean isOnTaskSelectionEventProcessing;

  private static Pair<GanttTreeTable, GanttTreeTableModel> createTreeTable(IGanttProject project, UIFacade uiFacade) {
    GanttTreeTableModel tableModel = new GanttTreeTableModel(project.getTaskManager(),
        project.getTaskCustomColumnManager());
    return Pair.create(new GanttTreeTable(project, uiFacade, tableModel), tableModel);
  }

  public GanttTree2(final GanttProject project, TaskManager taskManager, TaskSelectionManager selectionManager,
      final UIFacade uiFacade) {

    super(createTreeTable(project.getProject(), uiFacade));
    myUIFacade = uiFacade;
    myProject = project;
    myTaskManager = taskManager;
    mySelectionManager = selectionManager;

    myTaskManager.addTaskListener(new TaskListenerAdapter() {
      @Override
      public void taskModelReset() {
        clearTree();
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

    // Create the root node
    initRootNode();

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
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(getTreeTable(), DnDConstants.ACTION_COPY_OR_MOVE, this);
    dragSource.addDragSourceListener(this);
    DropTarget dropTarget = new DropTarget(getTreeTable(), new GanttTreeDropListener());
    dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

    getTreeTable().setToolTipText("aze");
    getTreeTable().getTreeTable().setToolTipText("rty");
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

  public void changeLanguage(GanttLanguage ganttLanguage) {
    language = ganttLanguage;
    // this.treetable.changeLanguage(language);
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

  /** add an object with the expand information */
  DefaultMutableTreeTableNode addObjectWithExpand(Object child, MutableTreeTableNode parent) {
    DefaultMutableTreeTableNode childNode = new TaskNode((Task) child);

    if (parent == null) {
      parent = getRootNode();
    }

    getTreeModel().insertNodeInto(childNode, parent, parent.getChildCount());
    // forwardScheduling();

    // Task task = (Task) (childNode.getUserObject());
    boolean res = true;

    // test for expansion
    while (parent != null) {
      Task taskFather = (Task) (parent.getUserObject());
      if (!taskFather.getExpand()) {
        res = false;
        break;
      }
      parent = (DefaultMutableTreeTableNode) (parent.getParent());
    }

    getTreeTable().getTree().scrollPathToVisible(TreeUtil.createPath(childNode));
    if (!res && parent != null) {
      getTreeTable().getTree().collapsePath(TreeUtil.createPath(parent));
    }
    myProject.refreshProjectInformation();

    return childNode;
  }

  /** @return the selected node */
  private DefaultMutableTreeTableNode getSelectedTaskNode() {
    DefaultMutableTreeTableNode selectedNode = getSelectedNode();
    return selectedNode instanceof TaskNode ? selectedNode : null;
  }

  /** @return the DefaultMutableTreeTableNode with the name name. */
  private MutableTreeTableNode getNode(final int id /* String name */) {
    return Iterables.find(TreeUtil.collectSubtree(getRoot()), new Predicate<MutableTreeTableNode>() {
      @Override
      public boolean apply(MutableTreeTableNode input) {
        return ((Task) input.getUserObject()).getTaskID() == id;
      }
    });
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

  // List<DefaultMutableTreeTableNode> getAllVisibleNodes() {
  // List<DefaultMutableTreeTableNode> res = new
  // ArrayList<DefaultMutableTreeTableNode>();
  // Enumeration<TreeNode> enumeration = getRootNode().preorderEnumeration();
  // while (enumeration.hasMoreElements()) {
  // DefaultMutableTreeTableNode o = (DefaultMutableTreeTableNode)
  // enumeration.nextElement();
  // if (getTreeTable().getTree().isVisible(new TreePath(o.getPath())))
  // res.add(o);
  // }
  // return res;
  // }

  /** Removes currentNode */
  public void removeCurrentNode(MutableTreeTableNode currentNode) {
    TreeNode parent = currentNode.getParent();
    myTaskManager.deleteTask((Task) currentNode.getUserObject());
    if (parent != null) {
      getTreeModel().removeNodeFromParent(currentNode);
      myProject.refreshProjectInformation();
    }
  }

  /** Clear the JTree. */
  private void clearTree() {
    // expand.clear();
    TreeUtil.removeAllChildren(getRootNode());
    initRootNode();
    getTreeModel().setRoot(getRootNode());
    // getTreeModel().reload();
  }

  private void selectTasks(List<Task> tasksList) {
    clearSelection();
    for (Task t : tasksList) {
      setSelected(t, false);
    }
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

  private class GanttTreeDropListener implements DropTargetListener {
    private TreePath lastPath = null;

    private Rectangle2D cueLineRect = new Rectangle2D.Float();

    private Rectangle2D ghostImageRect = new Rectangle2D.Float();

    private Color cueLineColor;

    private Point lastEventPoint = new Point();

    private Timer hoverTimer;

    public GanttTreeDropListener() {
      cueLineColor = new Color(SystemColor.controlShadow.getRed(), SystemColor.controlShadow.getGreen(),
          SystemColor.controlShadow.getBlue(), 64);

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
    public void dragEnter(DropTargetDragEvent dtde) {
      if (ghostImage == null) {
        // In case if you drag a file from out and it's not an
        // acceptable, and it can crash if the image is null
        ghostImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
      }
      if (!isDragAcceptable(dtde)) {
        dtde.rejectDrag();
      } else {
        dtde.acceptDrag(dtde.getDropAction());
      }
    }

    @Override
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

      Graphics2D g2 = (Graphics2D) getTreeTable().getGraphics();

      // If a drag image is not supported by the platform, then draw our
      // own drag image
      if (!DragSource.isDragImageSupported()) {
        // Rub out the last ghost image and cue line
        getTreeTable().paintImmediately(ghostImageRect.getBounds());
        // And remember where we are about to draw the new ghost image
        ghostImageRect.setRect(pt.x - offsetPoint.x, pt.y - offsetPoint.y, ghostImage.getWidth(),
            ghostImage.getHeight());
        g2.drawImage(ghostImage, AffineTransform.getTranslateInstance(ghostImageRect.getX(), ghostImageRect.getY()),
            null);
      } else {
        // Just rub out the last cue line
        getTreeTable().paintImmediately(cueLineRect.getBounds());
      }

      TreePath path = getTreeTable().getTree().getPathForLocation(pt.x, pt.y);
      if (path != lastPath) {
        lastPath = path;
        hoverTimer.restart();
      }

      // In any case draw (over the ghost image if necessary) a cue line
      // indicating where a drop will occur
      Rectangle raPath = getTreeTable().getTree().getCellRect(getTree().rowAtPoint(pt), getTree().columnAtPoint(pt),
          true);
      if (raPath == null) {
        raPath = new Rectangle(1, 1);
      }
      cueLineRect.setRect(0, raPath.y + (int) raPath.getHeight(), getWidth(), 2);

      g2.setColor(cueLineColor);
      g2.fill(cueLineRect);

      // And include the cue line in the area to be rubbed out next time
      ghostImageRect = ghostImageRect.createUnion(cueLineRect);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
      if (!isDragAcceptable(dtde)) {
        dtde.rejectDrag();
      } else {
        dtde.acceptDrag(dtde.getDropAction());
      }
    }

    @Override
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
      for (DataFlavor flavor : flavors) {
        if (flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)) {
          try {
            Point pt = dtde.getLocation();
            DefaultMutableTreeTableNode target = (DefaultMutableTreeTableNode) getTree().getPathForLocation(pt.x, pt.y).getLastPathComponent();
            TreePath pathSource = (TreePath) transferable.getTransferData(flavor);
            DefaultMutableTreeTableNode source = (DefaultMutableTreeTableNode) pathSource.getLastPathComponent();

            getTreeModel().removeNodeFromParent(source);
            getTreeModel().insertNodeInto(source, target, 0);

            TreePath pathNewChild = TreeUtil.createPath((TreeNode) pathSource.getLastPathComponent());

            // Mark this as the selected path in the tree
            getTree().getTreeSelectionModel().setSelectionPath(pathNewChild);

            expandRefresh(source);

            forwardScheduling();

            area.repaint();

            myProject.setAskForSave(true);

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

    @Override
    public void dragExit(DropTargetEvent dte) {
      if (!DragSource.isDragImageSupported()) {
        repaint(ghostImageRect.getBounds());
      }
      getTreeTable().repaint();
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
      TreePath path = getTree().getPathForLocation(pt.x, pt.y);
      if (dragPath.isDescendant(path)) {
        return false;
      }
      if (path.equals(dragPath)) {
        return false;
      }

      // Check if the task is a milestone task
      Task task = (Task) (((DefaultMutableTreeTableNode) path.getLastPathComponent()).getUserObject());
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
      TreePath path = getTree().getPathForLocation(pt.x, pt.y);
      if (path.equals(dragPath)) {
        return false;
      }
      return true;
    }
  }

  /** Transferable tree path class for the path specified in the constructor */
  private static class GanttTransferableTreePath implements Transferable {
    // The type of DnD object being dragged...
    public static final DataFlavor TREEPATH_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "TreePath");

    private TreePath _path;

    private DataFlavor[] _flavors = { TREEPATH_FLAVOR };

    public GanttTransferableTreePath(TreePath path) {
      _path = path;
    }

    // Transferable interface methods...
    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return _flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return java.util.Arrays.asList(_flavors).contains(flavor);
    }

    @Override
    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType())) {
        return _path;
      }
      throw new UnsupportedFlavorException(flavor);
    }
  }

  private List<DefaultMutableTreeTableNode> cpNodesArrayList;

  private List<MutableTreeTableNode> allNodes;

  private List<TaskDependency> cpDependencies;

  private Map<Integer, Integer> mapOriginalIDCopyID;

  private int where = -1;

  private AbstractAction[] myTreeActions;

  /** Cut the current selected tree node */
  public void cutSelectedNode() {
    final TreePath currentSelection = getTree().getTreeSelectionModel().getSelectionPath();
    if (currentSelection != null) {
      final DefaultMutableTreeTableNode[] selection = getSelectedNodes();
      getUndoManager().undoableEdit("Cut", new Runnable() {
        @Override
        public void run() {
          cpNodesArrayList = new ArrayList<DefaultMutableTreeTableNode>();
          cpAllDependencies(selection);
          GanttTask taskFather = null;
          MutableTreeTableNode parent = null;
          DefaultMutableTreeTableNode current = null;
          for (DefaultMutableTreeTableNode node : selection) {
            current = node;
            if (current != null) {
              cpNodesArrayList.add(current);
              parent = (MutableTreeTableNode) node.getParent();
              where = parent.getIndex(current);
              removeCurrentNode(current);
              taskFather = (GanttTask) parent.getUserObject();
              AdjustTaskBoundsAlgorithm alg = myTaskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm();
              alg.run(taskFather);
            }
          }
          if (parent.getChildCount() == 0) {
            ((Task) parent.getUserObject()).setProjectTask(false);
          }
          if (taskFather != null) {
            getTaskSelectionManager().addTask(taskFather);
          }
          area.repaint();
        }
      });
      myProject.repaint();
    }
  }

  /** Copy the current selected tree node */
  public void copySelectedNode() {
    DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes != null) {
      DefaultMutableTreeTableNode[] selectedRoots = findSelectedSubtreeRoots(selectedNodes);
      cpNodesArrayList = new ArrayList<DefaultMutableTreeTableNode>();
      cpAllDependencies(selectedRoots);
      for (int i = 0; i < selectedRoots.length; i++) {
        cpNodesArrayList.add(selectedNodes[i]);
      }
    }
  }

  private DefaultMutableTreeTableNode[] findSelectedSubtreeRoots(DefaultMutableTreeTableNode[] selectedNodes) {
    final HashSet<DefaultMutableTreeTableNode> set = new HashSet<DefaultMutableTreeTableNode>(
        Arrays.asList(selectedNodes));
    for (int i = 0; i < selectedNodes.length; i++) {
      for (TreeNode parent = selectedNodes[i].getParent(); parent != null; parent = parent.getParent()) {
        if (set.contains(parent)) {
          set.remove(selectedNodes[i]);
          break;
        }
      }
    }
    return set.toArray(new DefaultMutableTreeTableNode[set.size()]);
  }

  /** Paste the node and its child node to current selected position */
  public void pasteNode() {
    if (cpNodesArrayList != null) {
      getUndoManager().undoableEdit("Paste", new Runnable() {
        @Override
        public void run() {
          TaskNode current = (TaskNode) getSelectedTaskNode();
          List<Task> tasksList = new ArrayList<Task>();
          if (current == null) {
            current = (TaskNode) getRootNode();
          }

          mapOriginalIDCopyID = new HashMap<Integer, Integer>();

          for (int i = cpNodesArrayList.size() - 1; i >= 0; i--) {
            if (hasProjectTaskParent(current)) {
              ((Task) cpNodesArrayList.get(i).getUserObject()).setProjectTask(false);
            }
            // this will add new custom columns to the newly created task.
            TreeNode sel = getSelectedTaskNode();
            TreeNode parent = null;
            if (sel != null) {
              parent = sel.getParent();
              if (parent != null) {
                where = parent.getIndex(sel);
              }
            }
            tasksList.add((Task) insertClonedNode(
                current == getRootNode() ? current : (DefaultMutableTreeTableNode) current.getParent(),
                cpNodesArrayList.get(i), where + 1, true).getUserObject());
          }
          if (cpDependencies != null) {
            for (TaskDependency td : cpDependencies) {
              Task dependee = td.getDependee();
              Task dependant = td.getDependant();
              TaskDependencyConstraint constraint = td.getConstraint();
              boolean hasDependeeNode = false;
              boolean hasDependantNode = false;
              for (MutableTreeTableNode node : allNodes) {
                Object userObject = node.getUserObject();
                if (dependant.equals(userObject)) {
                  hasDependantNode = true;
                }
                if (dependee.equals(userObject)) {
                  hasDependeeNode = true;
                }
              }
              if (hasDependantNode && hasDependeeNode) {
                try {
                  TaskDependency dep = myTaskManager.getDependencyCollection().createDependency(
                      myTaskManager.getTask(mapOriginalIDCopyID.get(new Integer(dependant.getTaskID())).intValue()),
                      myTaskManager.getTask(mapOriginalIDCopyID.get(new Integer(dependee.getTaskID())).intValue()),
                      myTaskManager.createConstraint(constraint.getID()));
                  dep.setDifference(td.getDifference());
                  dep.setHardness(td.getHardness());
                } catch (TaskDependencyException e) {
                  myUIFacade.showErrorDialog(e);
                }
              }
            }
          }
          selectTasks(tasksList);
        }
      });
      myProject.refreshProjectInformation();
    }
  }

  // TODO Maybe place method in Task?
  /** @return true if the task has a parent which is a ProjectTask */
  private boolean hasProjectTaskParent(TaskNode task) {
    DefaultMutableTreeTableNode parent = (DefaultMutableTreeTableNode) task.getParent();
    while (parent != null) {
      if (((Task) parent.getUserObject()).isProjectTask()) {
        return true;
      }
      parent = (DefaultMutableTreeTableNode) parent.getParent();
    }
    return false;
  }

  /** Insert the cloned node and its children */
  private TaskNode insertClonedNode(DefaultMutableTreeTableNode parent, DefaultMutableTreeTableNode child,
      int location, boolean first) {
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
    GanttTask newTask = new GanttTask(originalTask);

    String newName = language.formatText("task.copy.prefix", language.getText("copy2"), newTask.getName());
    newTask.setName(newName);

    mapOriginalIDCopyID.put(new Integer(originalTask.getTaskID()), new Integer(newTask.getTaskID()));

    myTaskManager.registerTask(newTask);

    DefaultMutableTreeTableNode cloneChildNode = new TaskNode(newTask);

    for (int i = 0; i < child.getChildCount(); i++) {
      insertClonedNode(cloneChildNode, (DefaultMutableTreeTableNode) child.getChildAt(i), i, false);
    }

    if (parent.getChildCount() < location) {
      location = parent.getChildCount();
    }

    getTreeModel().insertNodeInto(cloneChildNode, parent, location);

    getTreeTable().getTree().scrollPathToVisible(TreeUtil.createPath(cloneChildNode));

    newTask.setExpand(false);
    return (TaskNode) cloneChildNode;
  }

  private void forwardScheduling() {
    RecalculateTaskScheduleAlgorithm alg = myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
    try {
      alg.run();
    } catch (TaskDependencyException e) {
      myUIFacade.showErrorDialog(e);
    }
  }

  @Override
  public void dragEnter(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragOver(DragSourceDragEvent dsde) {
  }

  @Override
  public void dropActionChanged(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragDropEnd(DragSourceDropEvent dsde) {
  }

  @Override
  public void dragExit(DragSourceEvent dse) {
  }

  @Override
  public void dragGestureRecognized(DragGestureEvent dge) {

    Point ptDragOrigin = dge.getDragOrigin();
    TreePath path = getTreeTable().getTree().getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
    if (path == null) {
      return;
    }

    // Work out the offset of the drag point from the TreePath bounding
    // rectangle origin
    Rectangle raPath = getTreeTable().getTree().getCellRect(getTree().rowAtPoint(ptDragOrigin),
        getTree().columnAtPoint(ptDragOrigin), true);
    offsetPoint.setLocation(ptDragOrigin.x - raPath.x, ptDragOrigin.y - raPath.y);

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
    lbl.setSize((int) raPath.getWidth(), (int) raPath.getHeight()); // <-- The
                                                                    // layout
                                                                    // manager
                                                                    // would
                                                                    // normally
                                                                    // do this

    // Get a buffered image of the selection for dragging a ghost image
    ghostImage = new BufferedImage((int) raPath.getWidth(), (int) raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g2 = ghostImage.createGraphics();

    // Ask the cell renderer to paint itself into the BufferedImage
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f)); // Make
                                                                           // the
                                                                           // image
                                                                           // ghostlike
    lbl.paint(g2);

    // Now paint a gradient UNDER the ghosted JLabel text (but not under the
    // icon if any)
    // Note: this will need tweaking if your icon is not positioned to the
    // left of the text
    Icon icon = lbl.getIcon();
    int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f)); // Make
                                                                                // the
                                                                                // gradient
                                                                                // ghostlike
    g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow, getWidth(), 0,
        new Color(255, 255, 255, 0)));
    g2.fillRect(nStartOfText, 0, getWidth(), ghostImage.getHeight());

    g2.dispose();

    getTree().getTreeSelectionModel().setSelectionPath(path); // Select this
                                                              // path in the
    // tree

    // Wrap the path being transferred into a Transferable object
    Transferable transferable = new GanttTransferableTreePath(path);

    // Remember the path being dragged (because if it is being moved, we
    // will have to delete it later)
    dragPath = path;

    // We pass our drag image just in case it IS supported by the platform
    dge.startDrag(null, ghostImage, new Point(5, 5), transferable, this);
  }

  private void cpAllDependencies(DefaultMutableTreeTableNode[] nodes) {
    // to get all the dependencies who need to be paste.
    cpDependencies = new ArrayList<TaskDependency>();
    allNodes = Lists.newArrayList();
    for (DefaultMutableTreeTableNode node : nodes) {
      allNodes.addAll(TreeUtil.collectSubtree(node));
    }
    TaskDependency[] dependencies = myTaskManager.getDependencyCollection().getDependencies();
    for (TaskDependency dependency : dependencies) {
      Task dependant = dependency.getDependant();
      Task dependee = dependency.getDependee();
      boolean hasDependeeNode = false;
      boolean hasDependantNode = false;
      for (MutableTreeTableNode node : allNodes) {
        Object userObject = node.getUserObject();
        if (userObject.equals(dependant)) {
          hasDependantNode = true;
        }
        if (userObject.equals(dependee)) {
          hasDependeeNode = true;
        }
      }
      if (hasDependantNode && hasDependeeNode) {
        cpDependencies.add(dependency);
      }
    }
  }

  @Override
  public void setDelay(final Task task, final Delay delay) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        TaskNode taskNode = (TaskNode) getNode(task.getTaskID());
        if (taskNode != null) {
          getTreeTable().setDelay(taskNode, delay);
        }
      }
    });
  }

  GanttTreeTableModel getModel() {
    return getTreeModel();
  }

  private GPUndoManager getUndoManager() {
    return myUIFacade.getUndoManager();
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
  public TableHeaderUIFacade getVisibleFields() {
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
