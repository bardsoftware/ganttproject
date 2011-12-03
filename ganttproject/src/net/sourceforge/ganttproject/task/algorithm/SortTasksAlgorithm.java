package net.sourceforge.ganttproject.task.algorithm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskLength;

/**
 * @author bard
 */
public class SortTasksAlgorithm {
    private Comparator<TaskActivity> mySortActivitiesByStartDateComparator = new Comparator<TaskActivity>() {
        @Override
        public int compare(TaskActivity leftTask, TaskActivity rightTask) {
            int result = 0;
            if (!leftTask.equals(rightTask)) {
                result = leftTask.getStart().compareTo(rightTask.getStart());
                if (result == 0) {
                    float longResult = 0;
                    TaskLength leftLength = leftTask.getDuration();
                    TaskLength rightLength = rightTask.getDuration();
                    if (leftLength.getTimeUnit().isConstructedFrom(
                            rightLength.getTimeUnit())) {
                        longResult = leftLength.getLength(rightLength
                                .getTimeUnit())
                                - rightLength.getLength();
                    } else if (rightLength.getTimeUnit().isConstructedFrom(
                            leftLength.getTimeUnit())) {
                        longResult = leftLength.getLength()
                                - rightLength.getLength(leftLength
                                        .getTimeUnit());
                    } else {
                        throw new IllegalArgumentException("Lengths="
                                + leftLength + " and " + rightLength
                                + " are not compatible");
                    }
                    if (longResult != 0) {
                        result = (int) (longResult / Math.abs(longResult));
                    }
                }
            }
            return result;
        }

    };

    private Comparator<Task> mySortTasksByStartDateComparator = new Comparator<Task>() {
        @Override
        public int compare(Task leftTask, Task rightTask) {
            int result = 0;
            if (!leftTask.equals(rightTask)) {
                result = leftTask.getStart().compareTo(rightTask.getStart());
                if (result == 0) {
                    float longResult = 0;
                    TaskLength leftLength = leftTask.getDuration();
                    TaskLength rightLength = rightTask.getDuration();
                    if (leftLength.getTimeUnit().isConstructedFrom(
                            rightLength.getTimeUnit())) {
                        longResult = leftLength.getLength(rightLength
                                .getTimeUnit())
                                - rightLength.getLength();
                    } else if (rightLength.getTimeUnit().isConstructedFrom(
                            leftLength.getTimeUnit())) {
                        longResult = leftLength.getLength()
                                - rightLength.getLength(leftLength
                                        .getTimeUnit());
                    } else {
                        throw new IllegalArgumentException("Lengths="
                                + leftLength + " and " + rightLength
                                + " are not compatible");
                    }
                    if (longResult != 0) {
                        result = (int) (longResult / Math.abs(longResult));
                    }
                }
            }
            return result;
        }

    };

    public void sortByStartDate(List<TaskActivity> tasks) {
        Collections.sort(tasks, mySortActivitiesByStartDateComparator);
    }

    public void sortTasksByStartDate(List<Task> tasks) {
        Collections.sort(tasks, mySortTasksByStartDateComparator);
    }

}
