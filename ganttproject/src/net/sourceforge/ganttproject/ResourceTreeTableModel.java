package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceColumn;
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


    public static final int INDEX_RESOURCE_NAME = 0;

    public static final int INDEX_RESOURCE_ROLE = 1;

    public static final int INDEX_RESOURCE_EMAIL = 2;

    public static final int INDEX_RESOURCE_PHONE = 3;

    public static final int INDEX_RESOURCE_ROLE_TASK = 4;

    /** all the columns */
//    private final Map<Integer, ResourceColumn> columns = new LinkedHashMap<Integer, ResourceColumn>();

    /** Column indexer */
    private static int index = -1;

    private DefaultMutableTreeNode root = null;

    private final HumanResourceManager myResourceManager;

    private final TaskManager myTaskManager;

    private TreeSelectionModel mySelectionModel;

    private final CustomPropertyManager myCustomPropertyManager;

    private String[] myDefaultColumnTitles;

    public ResourceTreeTableModel(HumanResourceManager resMgr, TaskManager taskManager, CustomPropertyManager customPropertyManager) {
        super();
        myCustomPropertyManager = customPropertyManager;
        myResourceManager = resMgr;
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
                    resourceAssignmentsChanged(new HumanResource[] {
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

    public ResourceNode getNodeForResource(HumanResource resource) {
        Enumeration<ResourceNode> childs = root.children();
        while (childs.hasMoreElements()) {
            ResourceNode rn = childs.nextElement();
            if (resource.equals(rn.getUserObject())) {
                return rn;
            }
        }
        return null;
    }

    public AssignmentNode getNodeForAssigment(ResourceAssignment assignement) {
        Enumeration<AssignmentNode> childs = getNodeForResource(
                assignement.getResource()).children();
        while (childs.hasMoreElements()) {
            AssignmentNode an = (AssignmentNode) childs.nextElement();
            if (assignement.equals(an.getUserObject())) {
                return an;
            }
        }
        return null;
    }

    private ResourceNode buildTree() {

        ResourceNode root = new ResourceNode(null);
        List<HumanResource> listResources = myResourceManager.getResources();
        Iterator<HumanResource> itRes = listResources.iterator();

        while (itRes.hasNext()) {
            HumanResource hr = itRes.next();
            ResourceNode rnRes = new ResourceNode(hr); // the first for the resource
            root.add(rnRes);
        }
        return root;
    }

    public void updateResources() {
        HumanResource[] listResources = myResourceManager.getResourcesArray();

        for (int idxResource = 0; idxResource < listResources.length; idxResource++) {
            HumanResource hr = listResources[idxResource];

            ResourceNode rnRes = exists(hr);
            if (rnRes == null) {
                rnRes = new ResourceNode(hr);
            }
            buildAssignmentsSubtree(rnRes);
//            for (int i = 0; i < tra.length; i++) {
//                AssignmentNode an = exists(rnRes, tra[i]);
//                if (an == null) {
//                    an = new AssignmentNode(tra[i]);
//                    rnRes.add(an);
//                }
//            }
            if (exists(hr) == null) {
                root.add(rnRes);
            }
            this.nodeStructureChanged(rnRes);
        }
        // this.setRoot(root);

    }

    ResourceNode exists(HumanResource hr) {
        Enumeration<ResourceNode> en = root.children();
        while (en.hasMoreElements()) {
            ResourceNode rn = en.nextElement();
            if (rn.getUserObject().equals(hr)) {
                return rn;
            }
        }
        return null;
    }

    /**
     * Changes the language.
     *
     * @param ganttLanguage
     *            New language to use.
     */
    public void changeLanguage(GanttLanguage ganttLanguage) {
        myDefaultColumnTitles = new String[] {
            language.getText("tableColResourceName"),
            language.getText("tableColResourceRole"),
            language.getText("tableColResourceEMail"),
            language.getText("tableColResourcePhone"),
            language.getText("tableColResourceRoleForTask")
        };
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

    public void changePeople(List<HumanResource> people) {
        Iterator<HumanResource> it = people.iterator();
        while (it.hasNext()) {
            addResource(it.next());
        }
    }

    public DefaultMutableTreeNode addResource(HumanResource people) {
        DefaultMutableTreeNode result = new ResourceNode(people);
        insertNodeInto(result, root, root.getChildCount());
        myResourceManager.toString();
        return result;
    }

    public void deleteResources(HumanResource[] peoples) {
        for (int i = 0; i < peoples.length; i++) {
            deleteResource(peoples[i]);
        }
    }

    public void deleteResource(HumanResource people) {
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

    public List<HumanResource> getAllResouces() {
        return myResourceManager.getResources();
    }

    public int getColumnCount() {
        return myDefaultColumnTitles.length + myCustomPropertyManager.getDefinitions().size();
    }

//    public ArrayList<ResourceColumn> getColumns()
//    {
//        return new ArrayList<ResourceColumn>(columns.values());
//    }
//
//    /** @return the ResourceColumn associated to the given index */
//    public ResourceColumn getColumn(int index) {
//        return columns.get(new Integer(index));
//    }

    private CustomPropertyDefinition getCustomProperty(int columnIndex) {
        return myCustomPropertyManager.getDefinitions().get(columnIndex - myDefaultColumnTitles.length);
    }

    public Class<?> getColumnClass(int colIndex) {
        if (colIndex == 0) {
            return hierarchicalColumnClass;
        }
        if (colIndex < myDefaultColumnTitles.length) {
            return String.class;
        }
        return getCustomProperty(colIndex).getType();
    }

    public String getColumnName(int column) {
        if (column < myDefaultColumnTitles.length) {
            return myDefaultColumnTitles[column];
        }
        CustomPropertyDefinition customColumn = getCustomProperty(column);
        return customColumn.getName();
    }

    public boolean isCellEditable(Object node, int column) {
        return (node instanceof ResourceNode && (column == INDEX_RESOURCE_EMAIL
                || column == INDEX_RESOURCE_NAME
                || column == INDEX_RESOURCE_PHONE || column == INDEX_RESOURCE_ROLE))
                || (node instanceof AssignmentNode && (column == INDEX_RESOURCE_ROLE_TASK)
                /* assumes the INDEX_RESOURCE_ROLE_TASK is the last mandatory column */
                || column > INDEX_RESOURCE_ROLE_TASK);
    }

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

//    /** Adds a column that cannot be removed afterwards. */
//    public void addMandatoryColumn(ResourceColumn col) {
//        columns.put(new Integer(col.getIndex()), col);
//    }
//
//    /** Adds a custom column (which is removable) to the datamodel */
//    public void addCustomColumn(String title, ResourceColumn col) throws Exception{
//        if (myResourceManager.checkCustomField(title)) {
//            throw new Exception(language.getText("columnExists"));
//        }
//        myResourceManager.addCustomField(col);
//        columns.put(new Integer(index), col);
//    }
//
//    /** deletes a custom column from the datamodel */
//    public ResourceColumn deleteCustomColumn(String name){
//        ResourceColumn toDel = null;
//        Collection<ResourceColumn> vals = columns.values();
//        Iterator<ResourceColumn> i = vals.iterator();
//
//        while (i.hasNext()) {
//            toDel = i.next();
//            if (name.equals( toDel.getTitle() )) {
//                myResourceManager.removeCustomField(toDel.getTitle());
//                /* this deletes the object from the HashTable too */
//                vals.remove(toDel);
//                return toDel;
//            }
//        }
//        return null;
//    }

    public void resourceChanged(HumanResource resource) {
        ResourceNode node = getNodeForResource(resource);
        if (node == null) {
            return;
        }
        TreeNode parent = node.getParent();
        int index = parent.getIndex(node);
        assert index >= 0;
        nodesChanged(parent, new int[] {index});
    }

    public void resourceAssignmentsChanged(HumanResource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            ResourceNode nextNode = exists(resources[i]);
            SelectionKeeper selectionKeeper = new SelectionKeeper(mySelectionModel, nextNode);
            buildAssignmentsSubtree(nextNode);
            selectionKeeper.restoreSelection();
        }
    }

    private void buildAssignmentsSubtree(ResourceNode resourceNode) {
        HumanResource resource = resourceNode.getResource();
        resourceNode.removeAllChildren();
        ResourceAssignment[] assignments = resource.getAssignments();
        int[] indices = new int[assignments.length];
        TreeNode[] children = new TreeNode[assignments.length];
        if (assignments.length > 0) {
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
            for (Enumeration<DefaultMutableTreeNode> subtree = myChangingSubtreeRoot
                    .depthFirstEnumeration(); subtree.hasMoreElements();) {
                DefaultMutableTreeNode node = subtree.nextElement();
                if (node.getUserObject().equals(myModelObject)) {
                    mySelectionModel.setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
        }
    }
}
