package net.sourceforge.ganttproject.task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface TaskContainmentHierarchyFacade {
	Task[] getNestedTasks(Task container);
	Task[] getDeepNestedTasks(Task container);

	boolean hasNestedTasks(Task container);

	Task getRootTask();

	Task getContainer(Task nestedTask);

	void move(Task whatMove, Task whereMove);

	boolean areUnrelated(Task dependant, Task dependee);

	int getDepth(Task task);

    int compareDocumentOrder(Task next, Task dependeeTask);

    boolean contains(Task task);

    interface Factory {
        TaskContainmentHierarchyFacade createFacede();
    }
    
	TaskContainmentHierarchyFacade STUB = new TaskContainmentHierarchyFacade() {
		public Task[] getNestedTasks(Task container) {
			return new Task[0];
		}
        public Task[] getDeepNestedTasks(Task container) {
            // TODO Auto-generated method stub
            return null;
        }
		public boolean hasNestedTasks(Task container) {
			return false;
		}
		public Task getRootTask() {
			return null;
		}
		public Task getContainer(Task nestedTask) {
			return null;
		}
		public void move(Task whatMove, Task whereMove) {
		}
		public boolean areUnrelated(Task dependant, Task dependee) {
			return false;
		}
		public int getDepth(Task task) {
			return 0;
		}
        public int compareDocumentOrder(Task next, Task dependeeTask) {
            throw new UnsupportedOperationException();
        }
        public boolean contains(Task task) {
            throw new UnsupportedOperationException();
        }
	};
}
