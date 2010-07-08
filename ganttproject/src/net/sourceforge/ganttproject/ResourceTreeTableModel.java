package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.resource.ResourceManager;
import net.sourceforge.ganttproject.resource.ResourceColumn;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;

import org.jdesktop.swing.treetable.DefaultTreeTableModel;

public class ResourceTreeTableModel extends DefaultTreeTableModel {
    private static GanttLanguage language = GanttLanguage.getInstance();

    public static String strResourceName = null;

    public static String strResourceRole = null;

    public static String strResourceEMail = null;

    public static String strResourcePhone = null;

    public static String strResourceRoleForTask = null;

    public static final int INDEX_RESOURCE_NAME = 0;

    public static final int INDEX_RESOURCE_ROLE = 1;

    public static final int INDEX_RESOURCE_EMAIL = 2;

    public static final int INDEX_RESOURCE_PHONE = 3;

    public static final int INDEX_RESOURCE_ROLE_TASK = 4;

    /** all the columns */
    private final Map columns = new LinkedHashMap();

    /** Column indexer */
    private static int index = -1;

    private DefaultMutableTreeNode root = null;

    private final HumanResourceManager myResourceManager;

    private final TaskManager myTaskManager;

    private TreeSelectionModel mySelectionModel;

    public ResourceTreeTableModel(ResourceManager resMgr, TaskManager taskManager) {
        super();
        myResourceManager = (HumanResourceManager) resMgr;
        myTaskManager = taskManager;
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            public void taskRemoved(TaskHierarchyEvent e) {
                fireResourceChange(e.getTask());
            }
            public void taskScheduleChanged(TaskScheduleEvent e) {
                fireResourceChange(e.getTask());
            }
            void fireResourceChange(Task task) {
                ResourceAssignment[] assignments = task.getAssignments();
                for (int i=0; i<assignments.length; i++) {
                    assignments[i].getResource().resetLoads();
                    resourceAssignmentsChanged(new ProjectResource[] {
                            assignments[i].getResource()});
                }
            }
        });
        changeLanguage(language);
        root = buildTree();
        this.setRoot(root);
    }

    public int useNextIndex() {
        index++;
        return index;
    }

    public ResourceNode getNodeForResource(ProjectResource resource) {
        ResourceNode res = null;
        Enumeration childs = root.children();
        while (childs.hasMoreElements() && res == null) {
            ResourceNode rn = (ResourceNode) childs.nextElement();
            if (resource.equals(rn.getUserObject()))
                res = rn;
        }
        return res;
    }

    public AssignmentNode getNodeForAssigment(ResourceAssignment assignement) {
        AssignmentNode res = null;
        Enumeration childs = getNodeForResource(assignement.getResource())
                .children();
        while (childs.hasMoreElements() && res == null) {
            AssignmentNode an = (AssignmentNode) childs.nextElement();
            if (assignement.equals(an.getUserObject()))
                res = an;
        }
        return res;
    }

    private ResourceNode buildTree() {

        ResourceNode root = new ResourceNode(null);
        List listResources = myResourceManager.getResources();
        Iterator itRes = listResources.iterator();

        while (itRes.hasNext()) {
            ProjectResource pr = (ProjectResource) itRes.next();

            ResourceAssignment[] tra = pr.getAssignments();
            ResourceNode rnRes = new ResourceNode(pr); // the first for the
            // resource
            root.add(rnRes);
        }
        return root;
    }

    public void updateResources() {
        ProjectResource[] listResources = myResourceManager.getResourcesArray();

        for (int idxResource=0; idxResource<listResources.length; idxResource++) {
            ProjectResource pr = listResources[idxResource];

            ResourceNode rnRes = exists(pr);
            if (rnRes == null) {
                rnRes = new ResourceNode(pr);
            }
            buildAssignmentsSubtree(rnRes);
//            for (int i = 0; i < tra.length; i++) {
//                AssignmentNode an = exists(rnRes, tra[i]);
//                if (an == null) {
//                    an = new AssignmentNode(tra[i]);
//                    rnRes.add(an);
//                }
//            }
            if (exists(pr) == null)
                root.add(rnRes);
            this.nodeStructureChanged(rnRes);
        }
        // this.setRoot(root);

    }

    ResourceNode exists(ProjectResource pr) {
        ResourceNode res = null;
        Enumeration en = root.children();
        while (res == null && en.hasMoreElements()) {
            ResourceNode rn = (ResourceNode) en.nextElement();
            if (rn.getUserObject().equals(pr))
                res = rn;
        }
        return res;
    }

    private AssignmentNode exists(ResourceNode rn, ResourceAssignment ra) {
        AssignmentNode res = null;
        Enumeration en = rn.children();
        while (res == null && en.hasMoreElements()) {
            AssignmentNode an = (AssignmentNode) en.nextElement();
            if (an.getUserObject().equals(ra))
                res = an;
        }
        return res;
    }

    /**
     * Changes the language.
     *
     * @param ganttLanguage
     *            New language to use.
     */
    public void changeLanguage(GanttLanguage ganttLanguage) {
        strResourceName = language.getText("tableColResourceName");
        strResourceRole = language.getText("tableColResourceRole");
        strResourceEMail = language.getText("tableColResourceEMail");
        strResourcePhone = language.getText("tableColResourcePhone");
        strResourceRoleForTask = language
                .getText("tableColResourceRoleForTask");

        // hack assume that INDEX_RESOURCE_ROLE_TASK is the last index
        String[] cols = new String[INDEX_RESOURCE_ROLE_TASK + 1];
        cols[INDEX_RESOURCE_EMAIL] = strResourceEMail;
        cols[INDEX_RESOURCE_NAME] = strResourceName;
        cols[INDEX_RESOURCE_PHONE] = strResourcePhone;
        cols[INDEX_RESOURCE_ROLE] = strResourceRole;
        cols[INDEX_RESOURCE_ROLE_TASK] = strResourceRoleForTask;
        for (int i = 0; i < cols.length; i++)
        {
            ResourceColumn col = (ResourceColumn)columns.get(new Integer(i));
            if (col != null)
                col.setTitle(cols[i]);
        }

    }

    /**
     * Invoked this to insert newChild at location index in parents children.
     * This will then message nodesWereInserted to create the appropriate event.
     * This is the preferred way to add children as it will create the
     * appropriate event.
     */
    public void insertNodeInto(MutableTreeNode newChild,
            MutableTreeNode parent, int index) {
        parent.insert(newChild, index);

        int[] newIndexs = new int[1];

        newIndexs[0] = index;
        nodesWereInserted(parent, newIndexs);
    }

    /**
     * Message this to remove node from its parent. This will message
     * nodesWereRemoved to create the appropriate event. This is the preferred
     * way to remove a node as it handles the event creation for you.
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        if (node != null) {
            MutableTreeNode parent = (MutableTreeNode) node.getParent();

            if (parent == null)
                throw new IllegalArgumentException(
                        "node does not have a parent.");

            int[] childIndex = new int[1];
            Object[] removedArray = new Object[1];

            childIndex[0] = parent.getIndex(node);
            parent.remove(childIndex[0]);
            removedArray[0] = node;
            nodesWereRemoved(parent, childIndex, removedArray);
        }
    }

    public void changePeople(List peoples) {
        Iterator it = peoples.iterator();
        while (it.hasNext())
            addResource((ProjectResource) it.next());
    }

    public DefaultMutableTreeNode addResource(ProjectResource people) {
        DefaultMutableTreeNode result = new ResourceNode(people);
        insertNodeInto(result, root, root.getChildCount());
        myResourceManager.toString();
        return result;
    }

    public void deleteResources(ProjectResource[] peoples) {
        for (int i = 0; i < peoples.length; i++) {
            deleteResource(peoples[i]);
        }
    }

    public void deleteResource(ProjectResource people) {
        removeNodeFromParent(getNodeForResource(people));
        // myResourceManager.remove(people);
    }

    /** Move Up the selected resource */
    public boolean moveUp(HumanResource resource) {
        myResourceManager.up(resource);
        ResourceNode rn = getNodeForResource(resource);
        int index = root.getIndex(root.getChildBefore(rn));
        removeNodeFromParent(rn);
        insertNodeInto(rn, root, index);
        return true;
    }

    public boolean moveDown(HumanResource resource) {
        myResourceManager.down(resource);
        ResourceNode rn = getNodeForResource(resource);
        int index = root.getIndex(root.getChildAfter(rn));
        removeNodeFromParent(rn);
        insertNodeInto(rn, root, index);
        return true;
    }

    public void reset() {
        myResourceManager.clear();
    }

    public List getAllResouces() {
        return myResourceManager.getResources();
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return columns.size();
    }

    public ArrayList getColumns()
    {
        ArrayList res = new ArrayList(columns.values());
        return res;
    }

    /** Returns the ResourceColumn associated to the given index */
    public ResourceColumn getColumn(int index) {
        return (ResourceColumn)columns.get(new Integer(index));
    }

    /**
     * {@inheritDoc}
     */
    public Class getColumnClass(int colIndex) {
        if (colIndex == 0) {
            return hierarchicalColumnClass;
        }
        ResourceColumn column = (ResourceColumn)columns.get(new Integer(colIndex));
        return column==null ? String.class : column.getType();
    }

    public String getColumnName(int column) {
        return ((ResourceColumn)columns.get(new Integer(column))).getTitle();
    }

    /**
     * @inheritDoc
     */
    public boolean isCellEditable(Object node, int column) {
        return (node instanceof ResourceNode && (column == INDEX_RESOURCE_EMAIL
                || column == INDEX_RESOURCE_NAME
                || column == INDEX_RESOURCE_PHONE || column == INDEX_RESOURCE_ROLE))
                || (node instanceof AssignmentNode && (column == INDEX_RESOURCE_ROLE_TASK)
                /* assumes the INDEX_RESOURCE_ROLE_TASK is the last mandatory column */
                || column > INDEX_RESOURCE_ROLE_TASK);
    }

    /**
     * @inheritDoc
     */
    public Object getValueAt(Object node, int column) {
        Object res = null;
        ResourceNode rn = null;
        AssignmentNode an = null;

        if (node instanceof ResourceNode)
            rn = (ResourceNode) node;
        else if (node instanceof AssignmentNode)
            an = (AssignmentNode) node;

        boolean hasChild = rn != null;

        switch (column) {
        case 0: // name
            if (hasChild) {
                res = rn.getName();
            } else {
                res = an.getTask().getName();
            }
            break;
        case 1: // def role
            if (hasChild) {
                res = rn.getDefaultRole();
            } else {
                res = "";
            }
            break;
        case 2: // mail
            if (hasChild) {
                res = rn.getEMail();
            } else {
                res = "";
            }
            break;
        case 3: // phone
            if (hasChild) {
                res = rn.getPhone();
            } else {
                res = "";
            }
            break;
        case 4: // assign role
            if (hasChild) {
                res = "";
            } else {
                res = an.getRoleForAssigment();
            }
            break;
        default: // custom column
            if (hasChild) {
                res = rn.getCustomField(this.getColumnName(column));
            }
            else
                res ="";
            break;
        }
        return res;
    }

    /**
     * @inheritDoc
     */
    public void setValueAt(Object value, Object node, int column) {
        if (isCellEditable(node, column))
            switch (column) {
            case INDEX_RESOURCE_NAME:
                ((ResourceNode) node).setName(value.toString());
                break;
            case INDEX_RESOURCE_EMAIL:
                ((ResourceNode) node).setEMail(value.toString());
                break;
            case INDEX_RESOURCE_PHONE:
                ((ResourceNode) node).setPhone(value.toString());
                break;
            case INDEX_RESOURCE_ROLE:
                ((ResourceNode) node).setDefaultRole((Role) value);
                break;
            case INDEX_RESOURCE_ROLE_TASK:
                ((AssignmentNode) node).setRoleForAssigment((Role) value);
                break;
            default:
                ((ResourceNode)node).setCustomField(this.getColumnName(column), value);
                break;
            }
        Mediator.getGanttProjectSingleton().setAskForSave(true);
    }

    /** Adds a column that cannot be removed afterwards. */
    public void addMandatoryColumn(ResourceColumn col) {
        columns.put(new Integer(col.getIndex()), col);
    }

    /** Adds a custom column (which is removable) to the datamodel */
    public void addCustomColumn(String title, ResourceColumn col) throws Exception{
        if (((HumanResourceManager)myResourceManager).checkCustomField(title))
            /* TODO add translation */
            throw new Exception("Column exists");
        ((HumanResourceManager)myResourceManager).addCustomField(col);
        columns.put(new Integer(index), col);
    }

    /** deletes a custom column from the datamodel */
    public ResourceColumn deleteCustomColumn(String name){
        ResourceColumn toDel = null;
        Collection vals = columns.values();
        Iterator i = vals.iterator();

        while (i.hasNext()) {
            toDel = (ResourceColumn)i.next();
            if (name.equals( toDel.getTitle() )) {
                ((HumanResourceManager)myResourceManager).removeCustomField(toDel.getTitle());
                /* this deletes the object from the Hashtable too */
                vals.remove(toDel);
                return toDel;
            }
        }

        return null;
    }

    /** checks if the given column is removable */
    public boolean checkRemovableCol(String name) {
        /* only custom columns are removable */
        return ((HumanResourceManager)myResourceManager).checkCustomField(name);
    }

    public void resourceChanged(ProjectResource resource) {
        ResourceNode node = getNodeForResource(resource);
        if (node==null) {
            return;
        }
        TreeNode parent = node.getParent();
        int index = parent.getIndex(node);
        assert index>=0;
        nodesChanged(parent, new int[] {index});
    }

    public void resourceAssignmentsChanged(ProjectResource[] resources) {
        for (int i=0; i<resources.length; i++) {
            ResourceNode nextNode = exists(resources[i]);
            SelectionKeeper selectionKeeper = new SelectionKeeper(mySelectionModel, nextNode);
            buildAssignmentsSubtree(nextNode);
            selectionKeeper.restoreSelection();
        }
    }

    private void buildAssignmentsSubtree(ResourceNode resourceNode) {
        ProjectResource resource = resourceNode.getResource();
        resourceNode.removeAllChildren();
        ResourceAssignment[] assignments = resource.getAssignments();
        int[] indices = new int[assignments.length];
        TreeNode[] children = new TreeNode[assignments.length];
        if (assignments.length>0) {
            for (int i = 0; i < assignments.length; i++) {
                indices[i] = i;
                AssignmentNode an = new AssignmentNode(assignments[i]);
                children[i] = an;
                resourceNode.add(an);
            }
        }
        fireTreeStructureChanged(this, resourceNode.getPath(), indices, children);

    }

    void decreaseCustomPropertyIndex(int i) {
        index -= i;
    }

    void setSelectionModel(TreeSelectionModel selectionModel) {
        mySelectionModel = selectionModel;
    }

    private static class SelectionKeeper {
        private final DefaultMutableTreeNode myChangingSubtreeRoot;
        private final TreeSelectionModel mySelectionModel;
        private boolean hasWork = false;
        private Object myModelObject;

        SelectionKeeper(TreeSelectionModel selectionModel, DefaultMutableTreeNode changingSubtreeRoot) {
            mySelectionModel = selectionModel;
            myChangingSubtreeRoot = changingSubtreeRoot;
            TreePath selectionPath = mySelectionModel.getSelectionPath();
            if (selectionPath != null
                    && new TreePath(myChangingSubtreeRoot.getPath()).isDescendant(selectionPath)) {
                hasWork = true;
                DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                myModelObject = lastNode.getUserObject();
            }
        }

        void restoreSelection() {
            if (!hasWork) {
                return;
            }
            for (Enumeration subtree = myChangingSubtreeRoot.depthFirstEnumeration(); subtree.hasMoreElements();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) subtree.nextElement();
                if (node.getUserObject().equals(myModelObject)) {
                    mySelectionModel.setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
        }


    }
}
