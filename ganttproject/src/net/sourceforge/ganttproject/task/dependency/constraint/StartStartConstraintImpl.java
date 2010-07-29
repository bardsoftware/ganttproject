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
 * Dependant task starts not earlier than dependee starts Created by IntelliJ
 * IDEA. User: bard
 */
public class StartStartConstraintImpl extends ConstraintImpl implements
        TaskDependencyConstraint {
    public StartStartConstraintImpl() {
        super(GanttTaskRelationship.SS, GanttLanguage.getInstance().getText(
                "startstart"));
    }

    public TaskDependencyConstraint.Collision getCollision() {
        TaskDependencyConstraint.Collision result = null;
        Task dependee = getDependency().getDependee();
        Task dependant = getDependency().getDependant();
        GanttCalendar dependeeStart = dependee.getStart();
        GanttCalendar dependantStart = dependant.getStart();
        //
        int difference = getDependency().getDifference();
       GanttCalendar comparisonDate = dependeeStart.Clone();
       comparisonDate.add(difference);

        boolean isActive = getDependency().getHardness()==TaskDependency.Hardness.RUBBER ? dependantStart
                .compareTo(comparisonDate) < 0 : dependantStart
                .compareTo(comparisonDate) != 0;
        // GanttCalendar acceptableStart = dependee.getStart();
        GanttCalendar acceptableStart = dependee.getStart().Clone();
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
                (int)(dependee.getDuration().getLength() - getDependency().getDifference()));
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
        TaskActivity theDependee = dependeeActivities[0];
        return new DependencyActivityBindingImpl(theDependant, theDependee,
                new Date[] { theDependant.getStart(), theDependee.getStart() });
    }

}
