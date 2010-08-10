package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: This class model the four relationships between different tasks.
 * <p>
 * Start-start: As soon as the predecessor task starts, the successor task can
 * start.
 * </p>
 * <p>
 * start to finish: As soon as the predecessor task starts, the successor task
 * can finish. This type of link is rarely used, but still available if you need
 * it.
 * </p>
 * <p>
 * finish to start relationship: As soon as the predecessor task finishes, the
 * successor task can start
 * </p>
 * <p>
 * finish to finish relationship: As soon as the predecessor task finishes, the
 * successor task can finish
 * </p>
 * </p>
 */

public class GanttTaskRelationship {
    public static final int SS = 1; // start to start

    public static final int FS = 2; // Finish to start

    public static final int FF = 3; // Finish to finish

    public static final int SF = 4; // start to finsih

    // private GanttTask predecessorTask; //predecessor;
    // private GanttTask successorTask; //successor;
    private int predecessorTaskID = -1;

    private int successorTaskID = -1;

    private int relationshipType;

    private int difference;

    private final TaskManager myTaskManager;

    /** Constructor, do nothing */
    public GanttTaskRelationship() {
        this(null);
    }

    public GanttTaskRelationship(TaskManager taskManager) {
        myTaskManager = taskManager;
    }

    /** Constructor */
    public GanttTaskRelationship(int predecessorTaskID, int successorTaskID,
            int relationshipType, int difference, TaskManager taskManager) {
        this(taskManager);
        this.predecessorTaskID = predecessorTaskID;
        this.successorTaskID = successorTaskID;
        this.relationshipType = relationshipType;
        this.difference = difference;
    }

    // getters and setters
    /** get the predecessor task */
    public GanttTask getPredecessorTask() {
        if (predecessorTaskID != -1) {
            return getManager().getTask(predecessorTaskID);
        } else {
            return null;
        }
    }

    /** get the predecessor task ID or -1 if there is no such ID */
    public int getPredecessorTaskID() {
        return predecessorTaskID;
    }

    /** set the predecessor task by GanttTask object */
    public void setPredecessorTask(Task predecessorTask) {
        this.predecessorTaskID = predecessorTask.getTaskID();
        // this.predecessorTask=predecessorTask;
    }

    /**
     * set the predecessor task ID by integer
     * 
     * @param predecessorTaskID
     *            ID of predecessor task
     */
    public void setPredecessorTask(int predecessorTaskID) {
        this.predecessorTaskID = predecessorTaskID;
    }

    /** get the successor task */
    public Task getSuccessorTask() {
        if (successorTaskID != -1) {
            return getManager().getTask(successorTaskID);
        } else {
            return null;
        }
    }

    /**
     * @return id of successor task
     */
    public int getSuccessorTaskID() {
        return successorTaskID;
    }

    /**
     * set the successor task by GanttTask object
     * 
     * @param successorTask
     *            GanttTask object of successor
     */
    public void setSuccessorTask(Task successorTask) {
        this.successorTaskID = successorTask.getTaskID();
        // this.successorTask = successorTask;
    }

    /**
     * set the successor task ID by the integer
     * 
     * @param seccessorTaskID
     *            id of the successor
     */
    public void setSuccessorTask(int seccessorTaskID) {
        this.successorTaskID = seccessorTaskID;
    }

    /** get the relationship type */
    public int getRelationshipType() {
        return relationshipType;
    }

    /** get the difference */
    public int getDifference() {
        return difference;
    }

    /** set the relationship type */
    public void setRelationshipType(int relationshipType) {
        this.relationshipType = relationshipType;
    }

    public boolean equals(GanttTaskRelationship compareRel) {
        return relationshipType == compareRel.relationshipType
                && predecessorTaskID == compareRel.predecessorTaskID
                && successorTaskID == compareRel.successorTaskID;
    }

    public Object clone() {
        GanttTaskRelationship copyRel = new GanttTaskRelationship(myTaskManager);
        copyRel.relationshipType = relationshipType;
        copyRel.predecessorTaskID = predecessorTaskID;
        copyRel.successorTaskID = successorTaskID;
        return copyRel;
    }

    public String toString() {
        String res = "Relation ";
        res += (relationshipType == SS) ? "(SS) "
                : (relationshipType == SF) ? "(SF) "
                        : (relationshipType == FS) ? "(FS) " : "(FF) ";
        res += getSuccessorTask() + " (" + getSuccessorTaskID() + ") "
                + getPredecessorTask() + " (" + getPredecessorTaskID() + ")";
        return res;
    }

    private TaskManager getManager() {
        return myTaskManager;
    }
}
