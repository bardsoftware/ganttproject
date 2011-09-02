/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 Thomas Alexandre, GanttProject Team

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.resource.ResourceActionSet;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
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
import net.sourceforge.ganttproject.util.BrowserControl;

public class GanttResourcePanel extends JPanel implements ResourceView,
        ResourceContext, AssignmentContext, ResourceTreeUIFacade {

    private final ResourceTreeTableModel model;

    final ResourceTreeTable table;

    public final GanttProject appli;

    private GanttLanguage lang = GanttLanguage.getInstance();

    private final ResourceActionSet myResourceActionSet;

    public ResourceLoadGraphicArea area;

    private HumanResource [] clipboard = null;
    private boolean isCut = false;

    private final GPAction myMoveUpAction = new GPAction() {
        @Override
        protected String getIconFilePrefix() {
            return "up_";
        }
        public void actionPerformed(ActionEvent e) {
            upResource();
        }
        @Override
        protected String getLocalizedName() {
            return getI18n("task.move.up");
        }
    };

    private GPAction myMoveDownAction = new GPAction() {
        @Override
        protected String getIconFilePrefix() {
            return "down_";
        }
        public void actionPerformed(ActionEvent e) {
            downResource();
        }
        @Override
        protected String getLocalizedName() {
            return getI18n("task.move.down");
        }
    };

    private Action myTaskPropertiesAction;

    private final UIFacade myUIFacade;

    public GanttResourcePanel(final GanttProject prj, final UIFacade uiFacade) {
        super(new BorderLayout());
        appli = prj;
        myUIFacade = uiFacade;
        myResourceActionSet = new ResourceActionSet(this, prj, uiFacade);

        prj.addProjectEventListener(getProjectEventListener());
        model = new ResourceTreeTableModel(appli.getHumanResourceManager(), prj.getTaskManager(), prj.getResourceCustomPropertyManager());
        table = new ResourceTreeTable(appli, model, uiFacade);
        table.setupActionMaps(myMoveUpAction, myMoveDownAction, null, null, myResourceActionSet
                .getResourceDeleteAction(), appli.getCutAction(), appli.getCopyAction(), appli.getPasteAction(),
                myResourceActionSet.getResourcePropertiesAction(), myResourceActionSet.getResourceDeleteAction());
        table.setRowHeight(20);
        table.setBackground(new Color(1.0f, 1.0f, 1.0f));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.insertWithLeftyScrollBar(this);
        area = new ResourceLoadGraphicArea(prj, prj.getZoomManager()) {
            @Override
            public boolean isExpanded(HumanResource hr) {
                return getResourceTreeTable().isExpanded(hr);
            }

            @Override
            protected int getRowHeight(){
                return table.getRowHeight();
            }
        };
        prj.getZoomManager().addZoomListener(area.getZoomListener());
        area.getChartModel().setRowHeight(table.getRowHeight());

        this.setBackground(new Color(0.0f, 0.0f, 0.0f));
        //applyComponentOrientation(lang.getComponentOrientation());

        // Add listener for mouse click
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath selPath = table.getTreeTable().getPathForLocation(
                        e.getX(), e.getY());
                table.getTreeTable().getTree().setSelectionPath(selPath);
                handlePopupTrigger(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopupTrigger(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                e.consume();
                if (e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON1) {
                    handleDoubleClick();
                }
                else {
                    handlePopupTrigger(e);
                }
            }
        };
        if (!prj.isOnlyViewer) {
            table.addMouseListener(ml);
            table.getTreeTable().getParent().addMouseListener(ml);
        }
        table.getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                myMoveUpAction.setEnabled(table.canMoveSelectionUp());
                myMoveDownAction.setEnabled(table.canMoveSelectionDown());
            }
        });
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

    private void handlePopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            DefaultMutableTreeNode[] selectedNodes = table.getSelectedNodes();
            if (selectedNodes.length == 1
                    && selectedNodes[0] instanceof AssignmentNode) {
                AssignmentNode assignmentNode = (AssignmentNode) selectedNodes[0];
                getTaskSelectionManager().clear();
                getTaskSelectionManager().addTask(assignmentNode.getTask());
                Point popupPoint = getPopupMenuPoint(e);
                getUIFacade().showPopupMenu(this,
                        new Action[] { myTaskPropertiesAction, myResourceActionSet.getResourceDeleteAction() },
                        popupPoint.x, popupPoint.y);
            } else {
                createPopupMenu(e);
            }
        }
    }

    private void handleDoubleClick() {
        myResourceActionSet.getResourcePropertiesAction().actionPerformed(null);
    }

    private Point getPopupMenuPoint(MouseEvent popupTriggerEvent) {
        final int x = popupTriggerEvent.getX();
        final int y = popupTriggerEvent.getY() + table.getRowHeight();
        return new Point(x,y);
    }

    /** Create the popup menu */
    private void createPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        AbstractAction[] resourceActions = myResourceActionSet.getActions();
        menu.add(resourceActions[0]);
        if (table.getSelectedNodes().length == 1) {
            for (int i = 1; i < resourceActions.length; i++) {
                menu.add(resourceActions[i]);
            }
            menu.add(appli.createNewItem(GanttLanguage.getInstance()
                    .correctLabel(lang.getText("sendMail")),
                    "/icons/send_mail_16.gif"));
            menu.addSeparator();
            menu.add(myMoveUpAction);
            menu.add(myMoveDownAction);
            menu.addSeparator();
            menu.add(appli.getCutAction());
            menu.add(appli.getCopyAction());
            menu.add(appli.getPasteAction());
        }
        menu.applyComponentOrientation(lang.getComponentOrientation());
        Point popupPoint = getPopupMenuPoint(e);
        menu.show(this, popupPoint.x, popupPoint.y);
    }

    /** Function called when the language is changed */
    public void refresh(GanttLanguage language) {
        lang = language;
        model.changeLanguage(lang);
    }

    public void resourceAdded(ResourceEvent event) {
        newHuman(event.getResource());
    }

    public void resourcesRemoved(ResourceEvent event) {
        table.getTable().editingStopped(new ChangeEvent(table.getTable()));
        model.deleteResources(event.getResources());
    }

    public void resourceChanged(ResourceEvent e) {
        model.resourceChanged(e.getResource());
        e.getResource().resetLoads();
        repaint();
    }

    public void resourceAssignmentsChanged(ResourceEvent e) {
        model.resourceAssignmentsChanged(e.getResources());
        repaint();
    }

    ////////////////////////////////////////////////////////////////////////////
    // ResourceContext interface
    public HumanResource[] getResources() {
        // ProjectResource[] res;
        // List allRes = model.getAllResouces();
        // res = new ProjectResource[allRes.size()];
        // model.getAllResouces().toArray(res);
        // return res;
        DefaultMutableTreeNode[] tNodes = table.getSelectedNodes();
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
                res[i] = (HumanResource) ((ResourceNode) tNodes[i])
                        .getUserObject();
            }
        }
        return res;
    }

    /** Create a new Human */
    public void newHuman(HumanResource people) {
        if (people != null) {
            DefaultMutableTreeNode result = model.addResource(people);
            table.getTree().scrollPathToVisible(new TreePath(result.getPath()));
        }
    }

    /** Send an Email to the current resource */
    public void sendMail(GanttProject parent) {
        if(table != null && table.getSelectedNodes()!=null && table.getSelectedNodes().length>0)
        {
            HumanResource people = (HumanResource) table.getSelectedNodes()[0]
                .getUserObject();
            if (people != null) {
                try {
                    BrowserControl.displayURL("mailto:" + people.getMail());
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }
        else
        {
            GanttDialogInfo gdi = new GanttDialogInfo(
                    appli, GanttDialogInfo.INFO,
                    GanttDialogInfo.YES_OPTION, GanttLanguage.getInstance()
                            .getText("msg26"),
                    GanttLanguage.getInstance().getText("sendMail"));
            gdi.setVisible(true);
        }
    }

    /** Move up the selected resource */
    private void upResource() {
        table.upResource();
    }

    /** Move down the selected resource */
    private void downResource() {
        table.downResource();
    }

    /** Return the list of the person */
    public List<HumanResource> getPeople() {
        return model.getAllResouces();
    }

    public ResourceTreeTable getResourceTreeTable() {
        return table;
    }

    public ResourceTreeTableModel getResourceTreeTableModel() {
        return model;
    }

    /** Return the number of people on the list */
    public int nbPeople() {
        return model.getAllResouces().size();
    }

    /** Reset all human... */
    public void reset() {
        model.reset();
    }

    public ResourceContext getContext() {
        return this;
    }

    public ResourceAssignment[] getResourceAssignments() {
        ResourceAssignment[] res = null;
        DefaultMutableTreeNode[] tNodes = table.getSelectedNodes();
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
                    res[i] = (ResourceAssignment) ((AssignmentNode) tNodes[i])
                            .getUserObject();
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
        if(clipboard == null) {
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
        DefaultMutableTreeNode selectedNodes[] = table.getSelectedNodes();

        if(selectedNodes == null) {
            return;
        }

        // count instances of ResourceNode
        int count = 0;
        for (DefaultMutableTreeNode node : selectedNodes) {
            if (node instanceof ResourceNode) {
                count++;
            }
        }

        clipboard = new HumanResource[count];

        int index = 0;
        for (DefaultMutableTreeNode node : selectedNodes) {
            if (node instanceof ResourceNode) {
                ResourceNode rn = (ResourceNode) node;

                clipboard[index] = (HumanResource) rn.getUserObject();
                if (cut) {
                    this.appli.getHumanResourceManager().remove(
                            this.clipboard[index], this.appli.getUndoManager());
                }
                index++;
            }
        }
    }

    public Action getMoveUpAction() {
        return myMoveUpAction;
    }

    public Action getMoveDownAction() {
        return myMoveDownAction;
    }

    public Component getUIComponent() {
        return this;
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

    public TableHeaderUIFacade getVisibleFields() {
        return table.getVisibleFields();
    }
}
