package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface FindPossibleDependeesAlgorithm {
    Task[] run(Task dependant);
}
