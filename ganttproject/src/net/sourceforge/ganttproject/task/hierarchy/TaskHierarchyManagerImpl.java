package net.sourceforge.ganttproject.task.hierarchy;

import net.sourceforge.ganttproject.task.Task;

public class TaskHierarchyManagerImpl {
    private TaskHierarchyItem myRootItem = new TaskHierarchyItem(null, null);

    public TaskHierarchyItem getRootItem() {
        return myRootItem;
    }

    public TaskHierarchyItem createItem(Task task) {
        TaskHierarchyItem result = new TaskHierarchyItem(task, myRootItem);
        return result;
    }

}
