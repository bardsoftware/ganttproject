package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 15:28:59 To
 * change this template use File | Settings | File Templates.
 */
public class TaskDependencyImpl implements TaskDependency {
    private TaskDependencyConstraint myConstraint;

    private int myDifference;

    private final Task myDependant;

    private final Task myDependee;

    private Hardness myHardness;
    
    private TaskDependencyCollectionImpl myCollection;

    public TaskDependencyImpl(Task dependant, Task dependee,
            TaskDependencyCollectionImpl collection) {
        myDependant = dependant;
        myDependee = dependee;
        myCollection = collection;
        if (dependee == null || dependant == null) {
            throw new IllegalArgumentException(
                    "invalid participants of dependency: dependee=" + dependee
                            + " dependant=" + dependant);
        }
        myHardness = Hardness.STRONG;
    }

    public Task getDependant() {
        return myDependant;
    }

    public Task getDependee() {
        return myDependee;
    }

    public void setConstraint(TaskDependencyConstraint constraint) {
        myConstraint = constraint;
        constraint.setTaskDependency(this);
        myCollection.fireChanged(this);
    }

    public TaskDependencyConstraint getConstraint() {
        return myConstraint;
    }

    public ActivityBinding getActivityBinding() {
        return getConstraint().getActivityBinding();
    }

    public void delete() {
        myCollection.delete(this);
    }

    public boolean equals(Object obj) {
        boolean result = obj instanceof TaskDependency;
        if (result) {
            TaskDependency rvalue = (TaskDependency) obj;
            result = myDependant.equals(rvalue.getDependant())
                    && myDependee.equals(rvalue.getDependee());
        }
        return result;
    }

    public int hashCode() {
        return 7 * myDependant.hashCode() + 9 * myDependee.hashCode();
    }

    public void setDifference(int difference) {
        myDifference = difference;
    }

    public int getDifference() {
        return myDifference;
    }

	public Hardness getHardness() {
		return myHardness;
	}

	public void setHardness(Hardness hardness) {
		myHardness = hardness;
	}

    /**
     * @return a String with the dependee at the start of the 'arrow' and the
     *         dependant at the end of the 'arrow' (same as the graphical
     *         representation)
     */
	public String toString() {
        return myDependee + "->" + myDependant;
	}
}
