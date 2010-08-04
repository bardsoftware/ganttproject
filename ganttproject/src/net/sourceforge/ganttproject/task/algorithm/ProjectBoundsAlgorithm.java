package net.sourceforge.ganttproject.task.algorithm;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import net.sourceforge.ganttproject.task.Task;

public class ProjectBoundsAlgorithm {
    public static class Result {
        public final Date lowerBound;

        public final Date upperBound;

        private Result(Date lowerBound, Date upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }

    public Result getBounds(Collection/* <Task> */tasks) {
        Date lowerBound = null;
        Date upperBound = null;
        for (Iterator it = tasks.iterator(); it.hasNext();) {
            Task next = (Task) it.next();
            Date start = next.getStart().getTime();
            Date end = next.getEnd().getTime();
            if (lowerBound == null || lowerBound.after(start)) {
                lowerBound = start;
            }
            if (upperBound == null || upperBound.before(end)) {
                upperBound = end;
            }
        }
        return new Result(lowerBound, upperBound);
    }
}
