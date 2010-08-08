package biz.ganttproject.impex.msproject2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Relation;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.reader.ProjectReader;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.Task.Priority;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class ProjectFileImporter {
    private final IGanttProject myNativeProject;
    private final ProjectReader myReader;
    private final File myForeignFile;

    public ProjectFileImporter(IGanttProject nativeProject, File foreignProjectFile) {
        myNativeProject = nativeProject;
        myReader = new MPPReader();
        myForeignFile = foreignProjectFile;
    }

    public void run() throws MPXJException {
        ProjectFile pf = myReader.read(myForeignFile);
        Map<Integer, GanttTask> foreignId2nativeTask = new HashMap<Integer, GanttTask>();
        importCalendar(pf);
        importResources(pf);
        importTasks(pf, foreignId2nativeTask);
        try {
            importDependencies(pf, foreignId2nativeTask);
        } catch (TaskDependencyException e) {
            e.printStackTrace();
        }
        importResourceAssignments(pf);
    }

    private void importCalendar(ProjectFile pf) {
    }

    private void importResources(ProjectFile pf) {
    }

    private void importTasks(ProjectFile foreignProject, Map<Integer, GanttTask> foreignId2nativeTask) {
        for (Task t: foreignProject.getChildTasks()) {
            importTask(foreignProject, t, myNativeProject.getTaskManager().getRootTask(), foreignId2nativeTask);
        }
    }

    private void importTask(ProjectFile foreignProject,
            Task t, net.sourceforge.ganttproject.task.Task supertask,
            Map<Integer, GanttTask> foreignId2nativeTask) {
        GanttTask nativeTask = myNativeProject.getTaskManager().createTask();
        myNativeProject.getTaskContainment().move(nativeTask, supertask);
        nativeTask.setName(t.getName());
        nativeTask.setStart(new GanttCalendar(t.getStart()));
        nativeTask.setNotes(t.getNotes());
        nativeTask.setWebLink(t.getHyperlink());
        nativeTask.setPriority(convertPriority(t));
        if (t.getChildTasks().isEmpty()) {
            if (t.getPhysicalPercentComplete() != null) {
                nativeTask.setCompletionPercentage(t.getPhysicalPercentComplete());
            }
            nativeTask.setMilestone(t.getMilestone());
            nativeTask.setDuration(convertDuration(t));
        }
        else {
            for (Task child: t.getChildTasks()) {
                importTask(foreignProject, child, nativeTask, foreignId2nativeTask);
            }
        }
        foreignId2nativeTask.put(t.getID(), nativeTask);
    }

    private Priority convertPriority(Task t) {
        net.sf.mpxj.Priority priority = t.getPriority();
        switch (priority.getValue()) {
        case net.sf.mpxj.Priority.HIGHEST:
        case net.sf.mpxj.Priority.VERY_HIGH:
            return Priority.HIGHEST;
        case net.sf.mpxj.Priority.HIGHER:
        case net.sf.mpxj.Priority.HIGH:
            return Priority.HIGH;
        case net.sf.mpxj.Priority.MEDIUM:
            return Priority.NORMAL;
        case net.sf.mpxj.Priority.LOWER:
        case net.sf.mpxj.Priority.LOW:
            return Priority.LOW;
        case net.sf.mpxj.Priority.VERY_LOW:
        case net.sf.mpxj.Priority.LOWEST:
            return Priority.LOWEST;
        default:
            return Priority.NORMAL;
        }
    }

    private TaskLength convertDuration(Task t) {
        if (t.getMilestone()) {
            return myNativeProject.getTaskManager().createLength(1);
        }
        return myNativeProject.getTaskManager().createLength(
            myNativeProject.getTimeUnitStack().getDefaultTimeUnit(), t.getStart(), t.getFinish());
    }

    private void importDependencies(ProjectFile pf, Map<Integer, GanttTask> foreignId2nativeTask) throws TaskDependencyException {
        for (Task t: pf.getAllTasks()) {
            if (t.getPredecessors() == null) {
                continue;
            }
            for (Relation r: t.getPredecessors()) {
                GanttTask dependant = foreignId2nativeTask.get(r.getSourceTask().getID());
                GanttTask dependee = foreignId2nativeTask.get(r.getTargetTask().getID());
                myNativeProject.getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
            }
        }
    }

    private void importResourceAssignments(ProjectFile pf) {
    }


}
