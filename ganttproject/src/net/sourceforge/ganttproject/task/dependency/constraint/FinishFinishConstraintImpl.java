package net.sourceforge.ganttproject.task.dependency.constraint;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.ActivityBinding;

/**
 * Dependant task finishes not earlier than dependee finishes Created by
 * IntelliJ IDEA. User: bard
 */
public class FinishFinishConstraintImpl extends ConstraintImpl implements
        TaskDependencyConstraint {
    public FinishFinishConstraintImpl() {
        super(GanttTaskRelationship.FF, GanttLanguage.getInstance().getText(
                "finishfinish"));
    }

    public TaskDependencyConstraint.Collision getCollision() {
        TaskDependencyConstraint.Collision result = null;
        Task dependee = getDependency().getDependee();
        Task dependant = getDependency().getDependant();
        GanttCalendar dependeeEnd = dependee.getEnd();
        GanttCalendar dependantEnd = dependant.getEnd();

        int difference = getDependency().getDifference();
        
        GanttCalendar comparisonDate = dependeeEnd.Clone();
        comparisonDate.add(difference);

        boolean isActive = getDependency().getHardness()==TaskDependency.Hardness.RUBBER ? dependantEnd
                .compareTo(comparisonDate) < 0 : dependantEnd
                .compareTo(comparisonDate) != 0;

        GanttCalendar acceptableStart = dependant.getStart().Clone();

        // GanttCalendar acceptableStart = dependant.getStart();
        if (isActive) {
            Task clone = dependee.unpluggedClone();
            TaskMutator mutator = clone.createMutator();
            mutator.shift(-dependant.getDuration().getLength());
            acceptableStart = clone.getEnd();
        }
        addDelay(acceptableStart);
        result = new TaskDependencyConstraint.DefaultCollision(acceptableStart,
                TaskDependencyConstraint.Collision.START_LATER_VARIATION,
                isActive);

        return result;
    }

    public Collision getBackwardCollision(Date dependantStart) {
        Task dependee = getDependency().getDependee();
        GanttCalendar dependeeEnd = dependee.getEnd().Clone();

        Date barrier = shift(
                dependantStart, 
                (int)(getDependency().getDependant().getDuration().getLength() 
                        - getDependency().getDifference()));
        boolean isActive = getDependency().getHardness()==TaskDependency.Hardness.RUBBER ?
                dependeeEnd.getTime().compareTo(barrier) > 0
                : dependeeEnd.getTime().compareTo(barrier) != 0;

        return new TaskDependencyConstraint.DefaultCollision(
                new GanttCalendar(barrier),
                TaskDependencyConstraint.Collision.START_EARLIER_VARIATION,
                isActive);
        
    }

    public ActivityBinding getActivityBinding() {
        TaskActivity[] dependantActivities = getDependency().getDependant()
                .getActivities();
        TaskActivity[] dependeeActivities = getDependency().getDependee()
                .getActivities();
        TaskActivity theDependant = dependantActivities[dependantActivities.length - 1];
        TaskActivity theDependee = dependeeActivities[dependeeActivities.length - 1];
        return new DependencyActivityBindingImpl(theDependant, theDependee,
                new Date[] { theDependant.getEnd(), theDependee.getEnd() });
    }

}
