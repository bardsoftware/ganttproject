package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;

public class TaskModelModificationListener extends TaskListenerAdapter {
    private IGanttProject myGanttProject;

    TaskModelModificationListener(IGanttProject ganttProject) {
        myGanttProject = ganttProject;
    }

    @Override
    public void taskScheduleChanged(TaskScheduleEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void dependencyAdded(TaskDependencyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void dependencyRemoved(TaskDependencyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void taskAdded(TaskHierarchyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void taskRemoved(TaskHierarchyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void taskMoved(TaskHierarchyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void taskPropertiesChanged(TaskPropertyEvent e) {
        myGanttProject.setModified();
    }

    @Override
    public void taskProgressChanged(TaskPropertyEvent e) {
        myGanttProject.setModified();
        e.getTask().getManager().getAlgorithmCollection()
                .getRecalculateTaskCompletionPercentageAlgorithm().run(
                        e.getTask());
    }
}
