package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 31.01.2004
 */
public interface TaskLength {
    float getLength(TimeUnit unit);

    long getLength();

    TimeUnit getTimeUnit();

    float getValue();
    
    TaskLength reverse();

	TaskLength translate(TimeUnit timeUnit);
}
