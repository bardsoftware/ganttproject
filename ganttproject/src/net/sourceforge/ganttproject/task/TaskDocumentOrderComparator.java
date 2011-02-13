package net.sourceforge.ganttproject.task;

import java.util.Comparator;

class TaskDocumentOrderComparator implements Comparator<Task> {
	private final TaskManagerImpl myManager;
	TaskDocumentOrderComparator(TaskManagerImpl taskManager) {
		myManager = taskManager;
	}
	public int compare(Task task1, Task tasl2) {
	    // TODO assert can be removed since it is checked by Java compiler?
		assert (task1 instanceof Task && tasl2 instanceof Task): "I compare only tasks";
		return myManager.getTaskHierarchy().compareDocumentOrder(task1, tasl2);
	}
}
