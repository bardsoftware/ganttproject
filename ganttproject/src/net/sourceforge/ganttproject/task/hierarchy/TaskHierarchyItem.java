package net.sourceforge.ganttproject.task.hierarchy;

import java.util.ArrayList;

import net.sourceforge.ganttproject.task.Task;

public class TaskHierarchyItem {
    private Task myTask;

    private TaskHierarchyItem myContainerItem;

    private TaskHierarchyItem myFirstNestedItem;

    private TaskHierarchyItem myNextSiblingItem;

    private static final TaskHierarchyItem[] EMPTY_ARRAY = new TaskHierarchyItem[0];

    public TaskHierarchyItem(Task myTask, TaskHierarchyItem containerItem) {
        this.myTask = myTask;
        this.myContainerItem = containerItem;
        if (myContainerItem != null)
        {
            myContainerItem.addNestedItem(this);
        }
    }

    public Task getTask() {
        return myTask;
    }

    public TaskHierarchyItem getContainerItem() {
        return myContainerItem;
    }

    public TaskHierarchyItem[] getNestedItems() {
        TaskHierarchyItem[] result;
        if (myFirstNestedItem == null) {
            result = EMPTY_ARRAY;
        } else {
            ArrayList tempList = new ArrayList();
            for (TaskHierarchyItem nested = myFirstNestedItem; nested != null; nested = nested.myNextSiblingItem) {
                tempList.add(nested);
            }
            result = (TaskHierarchyItem[]) tempList.toArray(EMPTY_ARRAY);
        }
        return result;
    }

    public void addNestedItem(TaskHierarchyItem nested) {
        nested.myNextSiblingItem = myFirstNestedItem;
        nested.myContainerItem = this;
        myFirstNestedItem = nested;
    }

    public void delete() {
        if (myContainerItem != null) {
            TaskHierarchyItem previousSibling = myContainerItem.myFirstNestedItem;
            if (this == previousSibling) {
                myContainerItem.myFirstNestedItem = myNextSiblingItem;
            } else {
                for (; previousSibling.myNextSiblingItem != this; previousSibling = previousSibling.myNextSiblingItem)
                    ;
                previousSibling.myNextSiblingItem = myNextSiblingItem;
            }
            myContainerItem = null;
        }
        myNextSiblingItem = null;
    }
}
