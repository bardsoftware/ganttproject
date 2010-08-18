package net.sourceforge.ganttproject.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.gui.TaskSelectionContext;

/**
 * This class manages the selected tasks.
 *
 * @author bbaranne
 */
public class TaskSelectionManager implements TaskSelectionContext {
    public interface Listener {
        void selectionChanged(List<Task> currentSelection);
		void userInputConsumerChanged(Object newConsumer);
    }
    /**
     * List of the selected tasks.
     */
    private final List<Task> selectedTasks = new ArrayList<Task>();
    private final List<Listener> myListeners = new ArrayList<Listener>();
	private Object myUserInputConsumer;
    /**
     * Creates an instance of TaskSelectionManager
     */
    public TaskSelectionManager() {
    }

    public void setUserInputConsumer(Object consumer) {
    	if (consumer!=myUserInputConsumer) {
    		fireUserInputConsumerChanged();
    	}
    	myUserInputConsumer = consumer;
    }

	/**
     * Adds <code>task</code> to the selected tasks.
     *
     * @param task
     *            A task to add to the selected tasks.
     */
    public void addTask(Task task) {
        if (!selectedTasks.contains(task)) {
            selectedTasks.add(task);
            fireSelectionChanged();
        }
    }

    /**
     * Removes <code>task</code> from the selected tasks;
     *
     * @param task
     *            A task to remove from the selected tasks.
     */
    public void removeTask(Task task) {
        if (selectedTasks.contains(task)) {
            selectedTasks.remove(task);
            fireSelectionChanged();
        }
    }

    /**
     * Returns <code>true</code> if <code>task</code> is selected,
     * <code>false</code> otherwise.
     *
     * @param task
     *            The task to test.
     * @return <code>true</code> if <code>task</code> is selected,
     *         <code>false</code> otherwise.
     */
    public boolean isTaskSelected(Task task) {
        return selectedTasks.contains(task);
    }

    /**
     * Returns the selected tasks list.
     *
     * @return The selected tasks list.
     */
    public List<Task> getSelectedTasks() {
        return Collections.unmodifiableList(selectedTasks);
    }

    /**
     * Returns the earliest start date.
     *
     * @return The earliest start date.
     */
    public Date getEarliestStart() {
        Date res = null;
        Iterator<Task> it = selectedTasks.iterator();
        while (it.hasNext()) {

            Task task = it.next();
            Date d = task.getStart().getTime();
            if (res == null) {
                res = d;
                continue;
            }
            if (d.before(res))
                res = d;
        }
        return res;
    }

    /**
     * Returns the latest end date.
     *
     * @return The latest end date.
     */
    public Date getLatestEnd() {
        Date res = null;
        Iterator<Task> it = selectedTasks.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            Date d = task.getEnd().getTime();
            if (res == null) {
                res = d;
                continue;
            }
            if (d.after(res))
                res = d;
        }
        return res;
    }

    /**
     * Clears the selected tasks list.
     */
    public void clear() {
        selectedTasks.clear();
        fireSelectionChanged();
    }

    public void addSelectionListener(Listener listener) {
        myListeners.add(listener);
    }

    public void removeSelectionListener(Listener listener) {
        myListeners.remove(listener);
    }

    private void fireSelectionChanged() {
        for (int i=0; i<myListeners.size(); i++) {
            Listener next = (Listener) myListeners.get(i);
            next.selectionChanged(Collections.unmodifiableList(selectedTasks));
        }
    }
    private void fireUserInputConsumerChanged() {
        for (int i=0; i<myListeners.size(); i++) {
            Listener next = (Listener) myListeners.get(i);
            next.userInputConsumerChanged(myUserInputConsumer);
        }
	}
}
