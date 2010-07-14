package net.sourceforge.ganttproject.task;

import java.util.Comparator;

class TaskDocumentOrderComparator implements Comparator {
	private final TaskManagerImpl myManager;
	TaskDocumentOrderComparator(TaskManagerImpl taskManager) {
		myManager = taskManager;
	}
	public int compare(Object object1, Object object2) {
		assert (object1 instanceof Task && object2 instanceof Task): "I compare only tasks";
		return myManager.getTaskHierarchy().compareDocumentOrder((Task)object1, (Task)object2);
	}
}
