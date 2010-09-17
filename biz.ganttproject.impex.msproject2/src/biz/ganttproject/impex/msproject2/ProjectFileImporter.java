package biz.ganttproject.impex.msproject2;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Relation;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.reader.ProjectReader;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task.Priority;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

public class ProjectFileImporter {
    private final IGanttProject myNativeProject;
    private final ProjectReader myReader;
    private final File myForeignFile;

    public ProjectFileImporter(IGanttProject nativeProject, File foreignProjectFile) {
        myNativeProject = nativeProject;
        myReader = new MSPDIReader();
        myForeignFile = foreignProjectFile;
    }

    private TaskManager getTaskManager() {
        return myNativeProject.getTaskManager();
    }

    public void run() throws MPXJException {
        ProjectFile pf = myReader.read(myForeignFile);
        Map<Integer, GanttTask> foreignId2nativeTask = new HashMap<Integer, GanttTask>();
        Map<Integer, HumanResource> foreignId2nativeResource = new HashMap<Integer, HumanResource>();
        importCalendar(pf);
        importResources(pf, foreignId2nativeResource);
        importTasks(pf, foreignId2nativeTask);
        try {
            importDependencies(pf, foreignId2nativeTask);
        } catch (TaskDependencyException e) {
            e.printStackTrace();
        }
        importResourceAssignments(pf, foreignId2nativeTask, foreignId2nativeResource);
    }

    private void importCalendar(ProjectFile pf) {
        ProjectCalendar defaultCalendar = pf.getCalendar();
        importWeekends(defaultCalendar);
        List<ProjectCalendarException> exceptions = defaultCalendar.getCalendarExceptions();
        for (ProjectCalendarException e: exceptions) {
            if (!e.getWorking()) {
                importHolidays(e);
            }
        }
    }

    private void importWeekends(ProjectCalendar calendar) {
        importDayType(calendar, Day.MONDAY, Calendar.MONDAY);
        importDayType(calendar, Day.TUESDAY, Calendar.TUESDAY);
        importDayType(calendar, Day.WEDNESDAY, Calendar.WEDNESDAY);
        importDayType(calendar, Day.THURSDAY, Calendar.THURSDAY);
        importDayType(calendar, Day.FRIDAY, Calendar.FRIDAY);
        importDayType(calendar, Day.SATURDAY, Calendar.SATURDAY);
        importDayType(calendar, Day.SUNDAY, Calendar.SUNDAY);
    }

    private void importDayType(ProjectCalendar foreignCalendar, Day foreignDay, int nativeDay) {
        getNativeCalendar().setWeekDayType(
                nativeDay, foreignCalendar.isWorkingDay(foreignDay) ? DayType.WORKING : DayType.WEEKEND);
    }

    private GPCalendar getNativeCalendar() {
        return myNativeProject.getActiveCalendar();
    }

    private void importHolidays(ProjectCalendarException e) {
        if (e.getRangeCount() > 0) {
            for (DateRange range : e) {
                importHolidays(range.getStart(), range.getEnd());
            }
        } else {
            importHolidays(e.getFromDate(), e.getToDate());
        }
    }

    private void importHolidays(Date start, Date end) {
        TaskLength oneDay = getTaskManager().createLength(GregorianTimeUnitStack.DAY, 1.0f);
        for (Date dayStart = start; !dayStart.after(end);) {
            myNativeProject.getActiveCalendar().setPublicHoliDayType(dayStart);
            dayStart = getTaskManager().shift(dayStart, oneDay);
        }
    }


    private void importResources(ProjectFile pf, Map<Integer, HumanResource> foreignId2humanResource) {
        for (Resource r: pf.getAllResources()) {
            HumanResource nativeResource = myNativeProject.getHumanResourceManager().newHumanResource();
            nativeResource.setName(r.getName());
            nativeResource.setMail(r.getEmailAddress());
            myNativeProject.getHumanResourceManager().add(nativeResource);
            foreignId2humanResource.put(r.getID(), nativeResource);
        }
    }

    private void importTasks(ProjectFile foreignProject, Map<Integer, GanttTask> foreignId2nativeTask) {
        for (Task t: foreignProject.getChildTasks()) {
            importTask(foreignProject, t, getTaskManager().getRootTask(), foreignId2nativeTask);
        }
    }

    private void importTask(ProjectFile foreignProject,
            Task t, net.sourceforge.ganttproject.task.Task supertask,
            Map<Integer, GanttTask> foreignId2nativeTask) {
        GanttTask nativeTask = getTaskManager().createTask();
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
            return getTaskManager().createLength(1);
        }
        return myNativeProject.getTaskManager().createLength(
            myNativeProject.getTimeUnitStack().getDefaultTimeUnit(), t.getStart(), t.getFinish());
    }

    private void importDependencies(ProjectFile pf, Map<Integer, GanttTask> foreignId2nativeTask)
    throws TaskDependencyException {
        for (Task t: pf.getAllTasks()) {
            if (t.getPredecessors() == null) {
                continue;
            }
            for (Relation r: t.getPredecessors()) {
                GanttTask dependant = foreignId2nativeTask.get(r.getSourceTask().getID());
                GanttTask dependee = foreignId2nativeTask.get(r.getTargetTask().getID());
                TaskDependency dependency = getTaskManager().getDependencyCollection().createDependency(
                        dependant, dependee);
                dependency.setConstraint(convertConstraint(r));
                if (r.getLag().getDuration() != 0.0) {
                    dependency.setDifference((int) r.getLag().convertUnits(
                            TimeUnit.DAYS, pf.getProjectHeader()).getDuration());
                }
            }
        }
    }

    private TaskDependencyConstraint convertConstraint(Relation r) {
        switch (r.getType()) {
        case FINISH_FINISH:
            return new FinishFinishConstraintImpl();
        case FINISH_START:
            return new FinishStartConstraintImpl();
        case START_FINISH:
            return new StartFinishConstraintImpl();
        case START_START:
            return new StartStartConstraintImpl();
        default:
            throw new IllegalStateException("Uknown relation type=" + r.getType());
        }
    }

    private void importResourceAssignments(ProjectFile pf,
            Map<Integer, GanttTask> foreignId2nativeTask, Map<Integer, HumanResource> foreignId2nativeResource) {
        for (ResourceAssignment ra: pf.getAllResourceAssignments()) {
            GanttTask nativeTask = foreignId2nativeTask.get(ra.getTask().getID());
            HumanResource nativeResource = foreignId2nativeResource.get(ra.getResource().getID());
            net.sourceforge.ganttproject.task.ResourceAssignment nativeAssignment =
                nativeTask.getAssignmentCollection().addAssignment(nativeResource);
            nativeAssignment.setLoad(ra.getUnits().floatValue());
        }
    }


}
