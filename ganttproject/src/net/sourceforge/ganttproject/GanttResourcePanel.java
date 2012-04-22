/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 Thomas Alexandre, GanttProject Team

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
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import net.sourceforge.ganttproject.action.resource.ResourceActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentContext;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.util.collect.Pair;

public class GanttResourcePanel extends TreeTableContainer<HumanResource, ResourceTreeTable, ResourceTreeTableModel>
    implements ResourceView, ResourceContext, AssignmentContext, ResourceTreeUIFacade {

  public final GanttProject appli;

  private final ResourceActionSet myResourceActionSet;

  public ResourceLoadGraphicArea area;

  private HumanResource[] clipboard = null;
  private boolean isCut = false;

  private Action myTaskPropertiesAction;

  private final UIFacade myUIFacade;

  private static Pair<ResourceTreeTable, ResourceTreeTableModel> createTreeTable(IGanttProject project,
      UIFacade uiFacade) {
    ResourceTreeTableModel model = new ResourceTreeTableModel(project.getHumanResourceManager(),
        project.getTaskManager(), project.getResourceCustomPropertyManager());
    return Pair.create(new ResourceTreeTable(project, model, uiFacade), model);
  }

  public GanttResourcePanel(final GanttProject prj, final UIFacade uiFacade) {
    super(createTreeTable(prj.getProject(), uiFacade));
    appli = prj;
    myUIFacade = uiFacade;

    prj.addProjectEventListener(getProjectEventListener());
    myResourceActionSet = new ResourceActionSet(this, this, prj, uiFacade, getTreeTable());

    setArtefactActions(myResourceActionSet.getResourceNewAction(), myResourceActionSet.getResourcePropertiesAction(),
        myResourceActionSet.getResourceDeleteAction());
    getTreeTable().setupActionMaps(myResourceActionSet.getResourceMoveUpAction(),
        myResourceActionSet.getResourceMoveDownAction(), null, null, myResourceActionSet.getResourceDeleteAction(),
        appli.getCutAction(), appli.getCopyAction(), appli.getPasteAction(),
        myResourceActionSet.getResourcePropertiesAction(), myResourceActionSet.getResourceDeleteAction());
    getTreeTable().addActionWithAccelleratorKey(myResourceActionSet.getAssignmentDelete());
    getTreeTable().setRowHeight(20);

    getTreeTable().insertWithLeftyScrollBar(this);
    area = new ResourceLoadGraphicArea(prj, prj.getZoomManager(), this) {
      @Override
      public boolean isExpanded(HumanResource hr) {
        return getResourceTreeTable().isExpanded(hr);
      }

      @Override
      protected int getRowHeight() {
        return getTreeTable().getRowHeight();
      }
    };
    prj.getZoomManager().addZoomListener(area.getZoomListener());
    area.getChartModel().setRowHeight(getTreeTable().getRowHeight());

    this.setBackground(new Color(0.0f, 0.0f, 0.0f));
    // applyComponentOrientation(lang.getComponentOrientation());
  }

  private ProjectEventListener getProjectEventListener() {
    return new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        area.repaint();
        reset();
      }
    };
  }

  @Override
  protected void onSelectionChanged(List<DefaultMutableTreeTableNode> selection) {
    super.onSelectionChanged(selection);
    getPropertiesAction().setEnabled(!selection.isEmpty());
    getDeleteAction().setEnabled(!selection.isEmpty());
    appli.getViewManager().getCopyAction().setEnabled(getResources().length > 0);
    appli.getViewManager().getCutAction().setEnabled(getResources().length > 0);
  }

  @Override
  protected void handlePopupTrigger(MouseEvent e) {
    if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
      DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
      // TODO Allow to have multiple assignments selected as well!
      if (selectedNodes.length == 1 && selectedNodes[0] instanceof AssignmentNode) {
        // Clicked on an assignment node (ie a task assigned to a resource)
        AssignmentNode assignmentNode = (AssignmentNode) selectedNodes[0];
        getTaskSelectionManager().clear();
        getTaskSelectionManager().addTask(assignmentNode.getTask());
        Point popupPoint = getPopupMenuPoint(e);
        getUIFacade().showPopupMenu(this,
            new Action[] { myTaskPropertiesAction, myResourceActionSet.getAssignmentDelete() }, popupPoint.x,
            popupPoint.y);
      } else {
        createPopupMenu(e);
      }
    }
  }

  private Point getPopupMenuPoint(MouseEvent popupTriggerEvent) {
    final int x = popupTriggerEvent.getX();
    final int y = popupTriggerEvent.getY() + getTreeTable().getRowHeight();
    return new Point(x, y);
  }

  /** Create the popup menu */
  private void createPopupMenu(MouseEvent e) {
    JPopupMenu menu = new JPopupMenu();
    AbstractAction[] resourceActions = myResourceActionSet.getActions();
    menu.add(resourceActions[0]);
    if (getSelectedNodes().length == 1) {
      for (int i = 1; i < resourceActions.length; i++) {
        menu.add(resourceActions[i]);
      }
      menu.add(myResourceActionSet.getResourceSendMailAction());
      menu.addSeparator();
      menu.add(myResourceActionSet.getResourceMoveUpAction());
      menu.add(myResourceActionSet.getResourceMoveDownAction());
      menu.addSeparator();
      menu.add(appli.getCutAction());
      menu.add(appli.getCopyAction());
      menu.add(appli.getPasteAction());
      menu.add(myResourceActionSet.getResourceDeleteAction());
    }
    menu.applyComponentOrientation(GanttLanguage.getInstance().getComponentOrientation());
    Point popupPoint = getPopupMenuPoint(e);
    menu.show(this, popupPoint.x, popupPoint.y);
  }

  @Override
  public void resourceAdded(ResourceEvent event) {
    newHuman(event.getResource());
  }

  @Override
  public void resourcesRemoved(ResourceEvent event) {
    getTreeTable().getTreeTable().editingStopped(new ChangeEvent(getTreeTable().getTreeTable()));
    getTreeModel().deleteResources(event.getResources());
  }

  @Override
  public void resourceChanged(ResourceEvent e) {
    getTreeModel().resourceChanged(e.getResource());
    e.getResource().resetLoads();
    repaint();
  }

  @Override
  public void resourceAssignmentsChanged(ResourceEvent e) {
    getTreeModel().resourceAssignmentsChanged(e.getResources());
    repaint();
  }

  // //////////////////////////////////////////////////////////////////////////
  // ResourceContext interface
  @Override
  public HumanResource[] getResources() {
    // ProjectResource[] res;
    // List allRes = model.getAllResouces();
    // res = new ProjectResource[allRes.size()];
    // model.getAllResouces().toArray(res);
    // return res;
    DefaultMutableTreeTableNode[] tNodes = getSelectedNodes();
    if (tNodes == null) {
      return new HumanResource[0];
    }
    int nbHumanResource = 0;
    for (int i = 0; i < tNodes.length; i++) {
      if (tNodes[i] instanceof ResourceNode) {
        nbHumanResource++;
      }
    }

    HumanResource[] res = new HumanResource[nbHumanResource];
    for (int i = 0; i < nbHumanResource; i++) {
      if (tNodes[i] instanceof ResourceNode) {
        res[i] = (HumanResource) ((ResourceNode) tNodes[i]).getUserObject();
      }
    }
    return res;
  }

  /** Create a new Human */
  public void newHuman(HumanResource people) {
    if (people != null) {
      DefaultMutableTreeTableNode result = getTreeModel().addResource(people);
      getTreeTable().getTree().scrollPathToVisible(TreeUtil.createPath(result));
    }
  }

  /** Return the list of the person */
  public List<HumanResource> getPeople() {
    return getTreeModel().getAllResouces();
  }

  public ResourceTreeTable getResourceTreeTable() {
    return getTreeTable();
  }

  public ResourceTreeTableModel getResourceTreeTableModel() {
    return getTreeModel();
  }

  /** Return the number of people on the list */
  public int nbPeople() {
    return getTreeModel().getAllResouces().size();
  }

  /** Reset all human... */
  public void reset() {
    getTreeModel().reset();
  }

  public ResourceContext getContext() {
    return this;
  }

  @Override
  public ResourceAssignment[] getResourceAssignments() {
    ResourceAssignment[] res = null;
    DefaultMutableTreeTableNode[] tNodes = getSelectedNodes();
    if (tNodes != null) {
      int nbAssign = 0;
      for (int i = 0; i < tNodes.length; i++) {
        if (tNodes[i] instanceof AssignmentNode) {
          nbAssign++;
        }
      }

      res = new ResourceAssignment[nbAssign];
      for (int i = 0; i < nbAssign; i++) {
        if (tNodes[i] instanceof AssignmentNode) {
          res[i] = (ResourceAssignment) ((AssignmentNode) tNodes[i]).getUserObject();
        }
      }
    }
    return res;
  }

  public void copySelection() {
    saveSelectionToClipboard(false);
    isCut = false;
  }

  public void cutSelection() {
    saveSelectionToClipboard(true);
    isCut = true;
  }

  public void pasteSelection() {
    if (clipboard == null) {
      return;
    }

    for (HumanResource resource : clipboard) {
      if (isCut) {
        appli.getHumanResourceManager().add(resource);
      } else {
        appli.getHumanResourceManager().add(resource.unpluggedClone());
      }
    }

    // if the selection was cut, we clear the clipboard after pasting
    if (isCut) {
      isCut = false;
    }
  }

  public void saveSelectionToClipboard(boolean cut) {
    DefaultMutableTreeTableNode selectedNodes[] = getSelectedNodes();

    if (selectedNodes == null) {
      return;
    }

    // count instances of ResourceNode
    int count = 0;
    for (DefaultMutableTreeTableNode node : selectedNodes) {
      if (node instanceof ResourceNode) {
        count++;
      }
    }

    clipboard = new HumanResource[count];

    int index = 0;
    for (DefaultMutableTreeTableNode node : selectedNodes) {
      if (node instanceof ResourceNode) {
        ResourceNode rn = (ResourceNode) node;

        clipboard[index] = (HumanResource) rn.getUserObject();
        if (cut) {
          this.appli.getHumanResourceManager().remove(this.clipboard[index], this.appli.getUndoManager());
        }
        index++;
      }
    }
  }

  @Override
  public void setSelected(HumanResource resource, boolean clear) {
    if (clear) {
      clearSelection();
    }
    getTree().getTreeSelectionModel().setSelectionPath(
        TreeUtil.createPath(getResourceTreeTableModel().getNodeForResource(resource)));
  }

  @Override
  public void clearSelection() {
    getTree().clearSelection();
  }

  @Override
  public AbstractAction getMoveUpAction() {
    return myResourceActionSet.getResourceMoveUpAction();
  }

  @Override
  public AbstractAction getMoveDownAction() {
    return myResourceActionSet.getResourceMoveDownAction();
  }

  public ResourceActionSet getResourceActionSet() {
    return myResourceActionSet;
  }

  void setTaskPropertiesAction(Action action) {
    myTaskPropertiesAction = action;
  }

  private UIFacade getUIFacade() {
    return myUIFacade;
  }

  private TaskSelectionManager getTaskSelectionManager() {
    return getUIFacade().getTaskSelectionManager();
  }

  @Override
  protected DefaultMutableTreeTableNode getRootNode() {
    return (DefaultMutableTreeTableNode) getTreeModel().getRoot();
  }

  @Override
  protected Chart getChart() {
    return myUIFacade.getResourceChart();
  }
}
