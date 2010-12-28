package net.sourceforge.ganttproject.task.dependency;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 2:35:20 To change
 * this template use File | Settings | File Templates.
 */
public interface TaskDependencyConstraint extends Cloneable {
    enum Type {
        startstart, finishstart, finishfinish, startfinish;

        public static Type getType(TaskDependencyConstraint constraint) {
            return getType(constraint.getID());
        }

        public static Type getType(int constraintID) {
            for (Type t : Type.values()) {
                if (t.ordinal()+1 == constraintID) {
                    return t;
                }
            }
            return null;
        }
    }
    void setTaskDependency(TaskDependency dependency);

    // boolean isFulfilled();
    // void fulfil();
    Collision getCollision();

    Collision getBackwardCollision(Date depedantStart);

    String getName();

    int getID();

    TaskDependency.ActivityBinding getActivityBinding();

    interface Collision {
        GanttCalendar getAcceptableStart();

        int getVariation();

        int NO_VARIATION = 0;

        int START_EARLIER_VARIATION = -1;

        int START_LATER_VARIATION = 1;

        boolean isActive();
    }

    class DefaultCollision implements Collision {
        private final GanttCalendar myAcceptableStart;

        private final int myVariation;

        private final boolean isActive;

        public DefaultCollision(GanttCalendar myAcceptableStart,
                int myVariation, boolean isActive) {
            this.myAcceptableStart = myAcceptableStart;
            this.myVariation = myVariation;
            this.isActive = isActive;
        }

        public GanttCalendar getAcceptableStart() {
            return myAcceptableStart;
        }

        public int getVariation() {
            return myVariation;
        }

        public boolean isActive() {
            return isActive;
        }

    }

    Object clone() throws CloneNotSupportedException;
}
