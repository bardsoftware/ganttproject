package net.sourceforge.ganttproject.task.dependency.constraint;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class ConstraintImpl implements Cloneable{
    private final int myID;

    private final String myName;

    private TaskDependency myDependency;

    public ConstraintImpl(int myID, String myName) {
        this.myID = myID;
        this.myName = myName;
    }

    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    protected TaskDependency getDependency() {
        return myDependency;
    }

    public void setTaskDependency(TaskDependency dependency) {
        myDependency = dependency;
    }

    public String getName() {
        return myName;
    }

    public int getID() {
        return myID;
    }

    public String toString() {
        return getName();
    }

    protected void shift(GanttCalendar calendar, int shift) {
        if (shift != 0) {
            Date shifted = shift(calendar.getTime(), shift);        
            calendar.setTime(shifted);
        }
    }
    
    protected Date shift(Date date, int shift) {
        if (shift != 0) {
            return myDependency.getDependant().getManager().getCalendar().shiftDate(
                    date, 
                    myDependency.getDependant().getManager().createLength(shift));        
        } 
        else {
            return date;
        }
    }
    
    protected void addDelay(GanttCalendar calendar) {
        shift(calendar, myDependency.getDifference());
//        calendar.add(difference);f
//        GanttCalendar solutionStart = calendar.Clone();
//        solutionStart.add(-1 * myDependency.getDifference());
//        for (int i = 0; i <= difference; i++) {
//            if ((myDependency.getDependant()
//                    .getManager().getCalendar()).isNonWorkingDay(solutionStart
//                    .getTime())) {
//                calendar.add(1);
//                difference++;
//            }
//            solutionStart.add(1);
//        }
    }
    
    public TaskDependencyConstraint.Collision getBackwardCollision(Date dependantStart) {
        return null;
    }
    
    public abstract TaskDependencyConstraint.Collision getCollision();
}
