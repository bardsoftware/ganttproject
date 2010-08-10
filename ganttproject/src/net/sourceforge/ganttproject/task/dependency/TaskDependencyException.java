package net.sourceforge.ganttproject.task.dependency;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskDependencyException extends Exception {
    public TaskDependencyException() {
    }

    public TaskDependencyException(String message) {
        super(message);
    }

    public TaskDependencyException(Throwable cause) {
        super(cause);
    }

    public TaskDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
