package net.sourceforge.ganttproject.delay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.UndoableEditEvent;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.undo.GPUndoListener;

/**
 * The DelayManager manages delays. It has all DelayObservers and notify each of
 * them when a delay has be calculated.
 *
 * @author bbaranne
 *
 */
public class DelayManager implements GPUndoListener {

    private boolean ourCriticProcess = false;

    private Date myToday;

    private List myObservers;

    private TaskManager myTaskManager;

    private TaskNode myRoot;

	private GanttTree2 myTree;

    public DelayManager(TaskManager taskManager, GanttTree2 tree) {
        myObservers = new ArrayList();
        myTaskManager = taskManager;
        myRoot = (TaskNode) tree.getRoot();
		myTree = tree;
		myTaskManager.addTaskListener(new TaskListenerImpl());
        Mediator.getUndoManager().addUndoableEditListener(this);
    }

    public void addObserver(DelayObserver observer) {
        myObservers.add(observer);
    }

    public void removeObserver(DelayObserver observer) {
        myObservers.remove(observer);
    }

    public void fireDelayObservation() {
        // System.err.println("fireDelayObservation");
        myToday = new Date();
        if (ourCriticProcess) {
        	ourCriticProcess = false;
			myTaskManager.processCriticalPath(myRoot);
			ArrayList projectTasks = myTree.getProjectTasks();
	        if (projectTasks.size() != 0)
				for (int i = 0 ; i < projectTasks.size() ; i++)
					myTaskManager.processCriticalPath((TaskNode) projectTasks.get(i));

//            System.out.println("critical path processed");
        }
        Iterator itTasks = Arrays.asList(myTaskManager.getTasks()).iterator();
        while (itTasks.hasNext()) {
            Task task = (Task) itTasks.next();
            Delay delay = calculateDelay(task);
            Iterator itObservers = myObservers.iterator();
            while (itObservers.hasNext()) {
                DelayObserver observer = (DelayObserver) itObservers.next();
                observer.setDelay(task, delay);
//                System.out.println("delay " + delay.getType() + " (critical = "+delay.CRITICAL+")");
            }
        }
    }

    /**
     * The delay is calculated as follow : The reference date is today. The
     * check is performed on the end date of the task. There could be a delay
     * only if percentage completion is not equal to 100%. If task end date <
     * today && completion < 100 there is a delay. The delay is critical is the
     * task is critical.
     *
     * @param t
     *            The task.
     * @return The calculated delay
     */
    private Delay calculateDelay(Task t) {
        Delay res = Delay.getDelay(Delay.NONE);
        int completionPercentage = t.getCompletionPercentage();
        if (t.isMilestone() || completionPercentage == 100)
            return res;
        Date endDate = t.getEnd().getTime();
        if (endDate.before(myToday))
            if (t.isCritical())
                res.setType(Delay.CRITICAL);
            else
                res.setType(Delay.NORMAL);
        return res;
    }

    private class TaskListenerImpl extends TaskListenerAdapter {
        public void taskScheduleChanged(TaskScheduleEvent e) {
            if (!e.getNewFinishDate().equals(e.getOldFinishDate())) {
                fireDelayObservation();
            }
        }
        public void dependencyAdded(TaskDependencyEvent e) {
            fireDelayObservation();
        }
        public void dependencyRemoved(TaskDependencyEvent e) {
            fireDelayObservation();
        }
        public void taskAdded(TaskHierarchyEvent e) {
            fireDelayObservation();
        }
        public void taskRemoved(TaskHierarchyEvent e) {
            fireDelayObservation();
        }
        public void taskProgressChanged(TaskPropertyEvent e) {
            fireDelayObservation();
        }
    }
    public void undoOrRedoHappened() {
//        System.out.println("undoOrRedoHappened");
        ourCriticProcess = true;
        fireDelayObservation();

    }

    public void undoableEditHappened(UndoableEditEvent arg0) {
        // TODO Auto-generated method stub
    }

}
