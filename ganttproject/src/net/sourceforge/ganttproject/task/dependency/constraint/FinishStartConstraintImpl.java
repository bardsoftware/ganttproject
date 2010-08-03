package net.sourceforge.ganttproject.task.dependency.constraint;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.ActivityBinding;

/**
 * Dependant task starts not earlier than dependee finishes Created by IntelliJ
 * IDEA. User: bard
 */
public class FinishStartConstraintImpl extends ConstraintImpl implements
        TaskDependencyConstraint {
    public FinishStartConstraintImpl() {
        super(GanttTaskRelationship.FS, GanttLanguage.getInstance().getText(
                "finishstart"));
    }

    public TaskDependencyConstraint.Collision getCollision() {
        TaskDependencyConstraint.Collision result = null;
        Task dependee = getDependency().getDependee();
        Task dependant = getDependency().getDependant();
        GanttCalendar dependeeEnd = dependee.getEnd().Clone();
        // GanttCalendar dependeeEnd = dependee.getEnd();
        GanttCalendar dependantStart = dependant.getStart();

        addDelay(dependeeEnd);
//        int difference = getDependency().getDifference();
        GanttCalendar comparisonDate = dependantStart.Clone();
//        comparisonDate.add(difference);

        boolean isActive = getDependency().getHardness()==TaskDependency.Hardness.RUBBER ? 
                dependeeEnd.compareTo(comparisonDate) > 0 
                : dependeeEnd.compareTo(comparisonDate) != 0;
        // new Exception("[FinishStartConstraint] isActive="+isActive+"
        // dependdee="+dependee+" end="+dependeeEnd+"
        // start="+dependantStart).printStackTrace();
        result = new TaskDependencyConstraint.DefaultCollision(
                dependeeEnd,
                TaskDependencyConstraint.Collision.START_LATER_VARIATION,
                isActive);
        return result;
    }

    public Collision getBackwardCollision(Date dependantStart) {
        if (dependantStart==null) {
            System.err.println();
        }
        Task dependee = getDependency().getDependee();
        GanttCalendar dependeeEnd = dependee.getEnd().Clone();

        Date barrier = shift(dependantStart, -getDependency().getDifference());
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
        TaskActivity theDependant = dependantActivities[0];
        TaskActivity theDependee = dependeeActivities[dependeeActivities.length - 1];
        return new DependencyActivityBindingImpl(theDependant, theDependee,
                new Date[] { theDependant.getStart(), theDependee.getEnd() });
    }

}
