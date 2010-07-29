/***************************************************************************
 * HumanResourcePanel.java  -  description
 * -------------------
 * begin                : jun 2003
 * copyright            : to the world :)
 * email                : alexthomas(at)ganttproject.org
 ***************************************************************************/
/*******************************************************************************
 * * This program is free software; you can redistribute it and/or modify * it
 * under the terms of the GNU General Public License as published by * the Free
 * Software Foundation; either version 2 of the License, or * (at your option)
 * any later version. * *
 ******************************************************************************/
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.DeleteAssignmentAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.ResourceActionSet;
import net.sourceforge.ganttproject.action.resource.ResourcePropertiesAction;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentContext;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.util.BrowserControl;

public class GanttResourcePanel extends JPanel implements ResourceView,
        ResourceContext, AssignmentContext, ProjectEventListener, ResourceTreeUIFacade {

    private final ResourceTreeTableModel model;

    private final ResourceTreeTable table;

    public final GanttProject appli;

    private GanttLanguage lang = GanttLanguage.getInstance();

    private JScrollBar vbar;

    private ResourceActionSet myResourceActionSet;

    public ResourceLoadGraphicArea area;

    private JScrollPane scrollpane;

    private JPanel left;

    private JPanel myImagePanel;

    private final ResourceContext myContext = (ResourceContext) this;

    private JSplitPane mySplitPane = null;

    private ProjectResource [] clipboard = null;
    private boolean isCut = false;

    private final GPAction myMoveUpAction = new GPAction() {
        protected String getIconFilePrefix() {
            return "up_";
        }
        public void actionPerformed(ActionEvent e) {
            upResource();
        }
        protected String getLocalizedName() {
            return getI18n("upTask");
        }
    };

    private GPAction myMoveDownAction = new GPAction() {
        protected String getIconFilePrefix() {
            return "down_";
        }
        public void actionPerformed(ActionEvent e) {
            downResource();
        }
        protected String getLocalizedName() {
            return getI18n("downTask");
        }
    };

    private GPAction myPropertiesAction = new GPAction() {
        protected String getIconFilePrefix() {
            return "";
        }
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode[] selectedNodes = table.getSelectedNodes();
            if (selectedNodes.length!=1) {
                Mediator.getGanttProjectSingleton().getUndoManager()
                .undoableEdit("New Human Resource by click", new Runnable() {
                    public void run() {
                        Mediator.getGanttProjectSingleton().newHumanResource();
                    }
                });
                return;
            }
            if (selectedNodes[0] instanceof ResourceNode) {
                getResourcePropertiesAction().actionPerformed(null);
            }
            else if (selectedNodes[0] instanceof AssignmentNode) {
                AssignmentNode assignmentNode = (AssignmentNode) selectedNodes[0];
                Mediator.getTaskSelectionManager().clear();
                Mediator.getTaskSelectionManager().addTask(assignmentNode.getTask());
                getTaskPropertiesAction().actionPerformed(null);
            }
        }
        protected String getLocalizedName() {
            return "";
        }
    };
    private final ListSelectionListener myContextListener;

    private final ResourcePropertiesAction myResourcePropertiesAction;

    private Action myTaskPropertiesAction;

    private final UIFacade myUIFacade;

    private final DeleteAssignmentAction myDeleteAssignmentAction;

    public GanttResourcePanel(final GanttProject prj, GanttTree2 tree, UIFacade uiFacade) {
        super();
        myUIFacade = uiFacade;
        myDeleteAssignmentAction = new DeleteAssignmentAction(
                prj.getProject().getHumanResourceManager(),
                (AssignmentContext)this, prj);

        prj.addProjectEventListener(this);
        appli = prj;
        model = new ResourceTreeTableModel(appli.getHumanResourceManager(), prj.getTaskManager());
        table = new ResourceTreeTable(appli.getProject(), model);

        setLayout(new BorderLayout());

        GanttImagePanel but = new GanttImagePanel("big.png", 300, 42);
        myImagePanel = but;
        left = new JPanel(new BorderLayout());
        table.setRowHeight(20);
        left.add(but, "North");
        left.setBackground(new Color(102, 153, 153));

        scrollpane = new JScrollPane();
        setLayout(new BorderLayout());
        add(scrollpane, BorderLayout.CENTER);
        scrollpane.getViewport().add(table);
        scrollpane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        vbar = table.getVerticalScrollBar();
        final JPanel jp = new JPanel(new BorderLayout());
        jp.add(vbar, BorderLayout.CENTER);
        // jp.setBorder(BorderFactory.createEmptyBorder(2,1,2,0));
        // jp.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        jp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        jp.setVisible(false);
        vbar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (table.getSize().getHeight() - 20 < e.getAdjustable()
                        .getMaximum())
                    jp.setVisible(true);
                else
                    jp.setVisible(false);
                repaint();
            }
        });
        left.add(jp, BorderLayout.WEST);

        vbar.addAdjustmentListener(new GanttAdjustmentListener());

        left.add(scrollpane, "Center");

        // A splitpane is use
        mySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        area = new ResourceLoadGraphicArea(prj, prj
                .getZoomManager()) {
            protected int getHeaderHeight() {
                return myImagePanel.getHeight()
                        + table.getTable().getTableHeader().getHeight();
            }

            public boolean isExpanded(ProjectResource pr) {
                return getResourceTreeTable().isExpanded(pr);
            }

            protected int getRowHeight(){
                return table.getRowHeight();
            }
        };
        prj.getZoomManager().addZoomListener(area.getZoomListener());
        area.getChartModel().setRowHeight(table.getRowHeight());
        if (lang.getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT) {
            mySplitPane.setLeftComponent(left);
            mySplitPane.setRightComponent(area);
        } else {
            mySplitPane.setRightComponent(left);
            mySplitPane.setLeftComponent(area);
            mySplitPane.setDividerLocation((int) (Toolkit.getDefaultToolkit()
                    .getScreenSize().getWidth() - left.getPreferredSize()
                    .getWidth()));

        }
        mySplitPane.setOneTouchExpandable(true);
        mySplitPane.setPreferredSize(new Dimension(800, 500));

        add(mySplitPane, BorderLayout.CENTER);
        scrollpane.getViewport().setBackground(new Color(1.0f, 1.0f, 1.0f));
        left.setBackground(new Color(1.0f, 1.0f, 1.0f));
        table.setBackground(new Color(1.0f, 1.0f, 1.0f));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.setBackground(new Color(0.0f, 0.0f, 0.0f));
        applyComponentOrientation(lang.getComponentOrientation());

        table.addKeyListener(prj); // callback for keyboard pressed
        // Add listener for mouse click
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                TreePath selPath = table.getTreeTable().getPathForLocation(
                        e.getX(), e.getY());
                table.getTreeTable().getTree().setSelectionPath(selPath);
                handlePopupTrigger(e);
            }

            public void mouseReleased(MouseEvent e) {
                handlePopupTrigger(e);
            }

            public void mouseClicked(MouseEvent e) {
                e.consume();
                if (e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON1) {
                    handleDoubleClick(e);
                }
                else {
                    handlePopupTrigger(e);
                }
            }
        };
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), "properties");
        table.getActionMap().put("properties", myPropertiesAction);
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getActionMap().put("delete", myDeleteAssignmentAction);
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(GPAction.getKeyStroke("newArtifact.shortcut")
                , "newHuman");
        table.getActionMap().put("newHuman", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                prj.newHumanResource();
            }
        });
        if (!prj.isOnlyViewer) {
            table.addMouseListener(ml);
            table.getTreeTable().getParent().addMouseListener(ml);
        }
        myResourcePropertiesAction = new ResourcePropertiesAction(prj.getProject(), prj.getUIFacade());
        myContextListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                myResourcePropertiesAction.setContext(getContext());
            }
        };
        table.getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                myMoveUpAction.setEnabled(table.canMoveSelectionUp());
                myMoveDownAction.setEnabled(table.canMoveSelectionDown());
            }
        });
        table.getTable().getSelectionModel().addListSelectionListener(myContextListener);
    }

    public void setActions()
    {
        table.setAction(appli.getCopyAction());
        table.setAction(appli.getPasteAction());
        table.setAction(appli.getCutAction());
    }

    private void handlePopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton()==MouseEvent.BUTTON3) {
            DefaultMutableTreeNode[] selectedNodes = table.getSelectedNodes();
            if (selectedNodes.length==1 && selectedNodes[0] instanceof AssignmentNode) {
                AssignmentNode assignmentNode = (AssignmentNode) selectedNodes[0];
                Mediator.getTaskSelectionManager().clear();
                Mediator.getTaskSelectionManager().addTask(assignmentNode.getTask());
                Point popupPoint = getPopupMenuPoint(e);
                getUIFacade().showPopupMenu(this, new Action[] {getTaskPropertiesAction(), myDeleteAssignmentAction}, popupPoint.x, popupPoint.y);
            }
            else {
                createPopupMenu(e);
            }
        }
    }

    private void handleDoubleClick(MouseEvent e) {
        myPropertiesAction.actionPerformed(null);
    }

    private Point getPopupMenuPoint(MouseEvent popupTriggerEvent) {
        final int x = popupTriggerEvent.getX() - scrollpane.getHorizontalScrollBar().getValue()
        + (vbar.isVisible() ? vbar.getWidth() : 0);
        final int y = popupTriggerEvent.getY() + table.getRowHeight() +
        + myImagePanel.getHeight();
        return new Point(x,y);
    }
    /* Create the popup menu */
    private void createPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        AbstractAction[] resourceActions = myResourceActionSet.getActions();
        menu.add(resourceActions[0]);
        if (table.getSelectedNodes().length == 1) {
            menu.add(myResourcePropertiesAction);
            for (int i = 1; i < resourceActions.length; i++) {
                menu.add(resourceActions[i]);
            }
            menu.add(appli.createNewItem(GanttProject.correctLabel(lang
                    .getText("sendMail")), "/icons/send_mail_16.gif"));
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
        newHuman((HumanResource) event.getResource());
    }

    public void resourcesRemoved(ResourceEvent event) {
        table.getTable().editingStopped(new ChangeEvent(table.getTable()));
        model.deleteResources(event.getResources());
    }

    public void resourceChanged(ResourceEvent e) {
        model.resourceChanged(e.getResource());
        ((HumanResource)e.getResource()).resetLoads();
        repaint();
    }

    public void resourceAssignmentsChanged(ResourceEvent e) {
        model.resourceAssignmentsChanged(e.getResources());
        repaint();
    }

    // //////////////////////////////////////////////////////////////////////////
    // ResourceContext interface
    public ProjectResource[] getResources() {
        // ProjectResource[] res;
        // List allRes = model.getAllResouces();
        // res = new ProjectResource[allRes.size()];
        // model.getAllResouces().toArray(res);
        // return res;
        ProjectResource[] res;
        DefaultMutableTreeNode[] tNodes = table.getSelectedNodes();
        if (tNodes==null) {
            return new ProjectResource[0];
        }
        int nbProjectResource = 0;
        for (int i = 0; i < tNodes.length; i++)
            if (tNodes[i] instanceof ResourceNode)
                nbProjectResource++;

        res = new ProjectResource[nbProjectResource];
        for (int i = 0; i < nbProjectResource; i++)
            if (tNodes[i] instanceof ResourceNode)
                res[i] = (ProjectResource) ((ResourceNode) tNodes[i])
                        .getUserObject();
        return res;
    }

    /**
     * Listener when scrollbar move
     */
    public class GanttAdjustmentListener implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (area != null) {
                area.setScrollBar(e.getValue());
                area.repaint();
            }
        }
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
            gdi.show();
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
    public List getPeople() {
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

    public void setResourceActions(ResourceActionSet actionSet) {
        myResourceActionSet = actionSet;
    }

    public ResourceContext getContext() {
        return myContext;
    }

    public ResourceAssignment[] getResourceAssignments() {
        ResourceAssignment[] res = null;
        DefaultMutableTreeNode[] tNodes = table.getSelectedNodes();
        if (tNodes != null) {
            int nbAssign = 0;
            for (int i = 0; i < tNodes.length; i++)
                if (tNodes[i] instanceof AssignmentNode)
                    nbAssign++;

            res = new ResourceAssignment[nbAssign];
            for (int i = 0; i < nbAssign; i++)
                if (tNodes[i] instanceof AssignmentNode)
                    res[i] = (ResourceAssignment) ((AssignmentNode) tNodes[i])
                            .getUserObject();
        }
        return res;
    }

    public void setDividerLocation(int location) {
        mySplitPane.setDividerLocation(location);
    }

    public int getDividerLocation() {
        return mySplitPane.getDividerLocation();
    }

    public void projectModified() {
        // TODO Auto-generated method stub

    }

    public void projectSaved() {
        // TODO Auto-generated method stub

    }

    public void projectClosed() {
        area.repaint();
        reset();
    }

    public void copySelection()
    {
        this.saveSelectionToClipboard(false);
        this.isCut = false;
    }

    public void cutSelection()
    {
        this.saveSelectionToClipboard(true);
        this.isCut = true;
    }

    public void pasteSelection()
    {
        if(this.clipboard == null)
            return;

        for(int i=0; i<this.clipboard.length; i++)
        {
            if(this.isCut)
            {
                this.appli.getHumanResourceManager().add(this.clipboard[i]);
            }
            else
            {
                this.appli.getHumanResourceManager().add(clipboard[i].unpluggedClone());
            }
        }

        /*if the selection was cut, we clear the clipboard after pasting*/
        if(this.isCut)
        {
            this.isCut = false;
        }
    }

    public void saveSelectionToClipboard(boolean cut)
    {
        DefaultMutableTreeNode selectedNodes[] = this.table.getSelectedNodes();

        if(selectedNodes == null)
            return;

        /*count instances of ResourceNode*/
        int count=0;
        for(int i=0; i<selectedNodes.length; i++)
        {
            if(selectedNodes[i] instanceof ResourceNode)
            {
                count++;
            }
        }

        this.clipboard = new ProjectResource[count];

        int index=0;
        for(int i=0; i<selectedNodes.length; i++)
        {
            if(selectedNodes[i] instanceof ResourceNode)
            {
                ResourceNode rn = (ResourceNode)selectedNodes[i];

                this.clipboard[index] = (HumanResource)rn.getUserObject();
                if(cut)
                {
                    this.appli.getHumanResourceManager().remove(this.clipboard[index], this.appli.getUndoManager());
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

    public Action getResourcePropertiesAction() {
        return myResourcePropertiesAction;
    }

    void setTaskPropertiesAction(Action action) {
        myTaskPropertiesAction = action;
    }

    private Action getTaskPropertiesAction() {
        return myTaskPropertiesAction;
    }

    private UIFacade getUIFacade() {
        return myUIFacade;
    }

    public TableHeaderUIFacade getVisibleFields() {
        return table.getVisibleFields();
    }
}
