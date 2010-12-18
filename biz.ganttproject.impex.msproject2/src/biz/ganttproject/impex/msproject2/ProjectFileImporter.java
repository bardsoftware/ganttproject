/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package biz.ganttproject.impex.msproject2;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.FieldType;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Rate;
import net.sf.mpxj.Relation;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskField;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.reader.ProjectReader;
import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.calendar.walker.WorkingUnitCounter;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumnsException;
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

class ProjectFileImporter {
    private final IGanttProject myNativeProject;
    private final ProjectReader myReader;
    private final File myForeignFile;
	private Map<ResourceField, CustomPropertyDefinition> myResourceCustomPropertyMapping;
	private Map<TaskField, CustomPropertyDefinition> myTaskCustomPropertyMapping;

    private static ProjectReader createReader(File file) {
        int lastDot = file.getName().lastIndexOf('.');
        if (lastDot == file.getName().length() - 1) {
            return null;
        }
        String fileExt = file.getName().substring(lastDot+1).toLowerCase();
        if ("mpp".equals(fileExt)) {
            return new MPPReader();
        } else if ("xml".equals(fileExt)) {
            return new MSPDIReader();
        } else if ("mpx".equals(fileExt)) {
            return new MPXReader();
        }
        return null;
    }

    private static interface HolidayAdder {
        void addHoliday(Date date);
    }

    public ProjectFileImporter(IGanttProject nativeProject, File foreignProjectFile) {
        myNativeProject = nativeProject;
        myReader = ProjectFileImporter.createReader(foreignProjectFile);
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
        if (defaultCalendar == null) {
        	return;
        }
        importWeekends(defaultCalendar);
        List<ProjectCalendarException> exceptions = defaultCalendar.getCalendarExceptions();
        for (ProjectCalendarException e: exceptions) {
            if (!e.getWorking()) {
                importHolidays(e, new HolidayAdder() {
                    public void addHoliday(Date date) {
                        getNativeCalendar().setPublicHoliDayType(date);
                    }
                });
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

    private void importHolidays(ProjectCalendarException e, HolidayAdder adder) {
        if (e.getRangeCount() > 0) {
            for (DateRange range : e) {
                importHolidays(range.getStart(), range.getEnd(), adder);
            }
        } else {
            importHolidays(e.getFromDate(), e.getToDate(), adder);
        }
    }

    private void importHolidays(Date start, Date end, HolidayAdder adder) {
        TaskLength oneDay = getTaskManager().createLength(GregorianTimeUnitStack.DAY, 1.0f);
        for (Date dayStart = start; !dayStart.after(end);) {
            //myNativeProject.getActiveCalendar().setPublicHoliDayType(dayStart);
            adder.addHoliday(dayStart);
            dayStart = GPCalendar.PLAIN.shiftDate(dayStart, oneDay);
        }
    }

    private void importResources(ProjectFile pf, Map<Integer, HumanResource> foreignId2humanResource) {
        myResourceCustomPropertyMapping = new HashMap<ResourceField, CustomPropertyDefinition>();
        for (Resource r: pf.getAllResources()) {
            HumanResource nativeResource = myNativeProject.getHumanResourceManager().newHumanResource();
            nativeResource.setId(r.getUniqueID());
            nativeResource.setName(r.getName());
            nativeResource.setMail(r.getEmailAddress());
            myNativeProject.getHumanResourceManager().add(nativeResource);
            importDaysOff(r, nativeResource);
            importCustomProperties(r, nativeResource);
            foreignId2humanResource.put(r.getUniqueID(), nativeResource);
        }
    }

    private void importCustomProperties(Resource r, HumanResource nativeResource) {
        for (ResourceField rf : ResourceField.values()) {
            if (r.getCurrentValue(rf) == null || !isCustomField(rf)) {
                continue;
            }
            CustomPropertyDefinition def = myResourceCustomPropertyMapping.get(rf);
            if (def == null) {
                String typeAsString = convertDataType(rf);
                String name = r.getParentFile().getResourceFieldAlias(rf);
                if (name == null) {
                    name = rf.getName();
                }
                def = myNativeProject.getResourceCustomPropertyManager().createDefinition(
                        typeAsString, name, null);
                myResourceCustomPropertyMapping.put(rf, def);
            }
            nativeResource.setCustomField(def.getName(), convertDataValue(rf, r.getCurrentValue(rf)));
        }
	}

	private void importDaysOff(Resource r, final HumanResource nativeResource) {
        ProjectCalendar c = r.getResourceCalendar();
        if (c == null) {
            return;
        }
        for (ProjectCalendarException e: c.getCalendarExceptions()) {
            importHolidays(e, new HolidayAdder() {
                public void addHoliday(Date date) {
                    nativeResource.addDaysOff(new GanttDaysOff(
                            date, GregorianTimeUnitStack.DAY.adjustRight(date)));
                }
            });
        }
    }

    private void importTasks(ProjectFile foreignProject, Map<Integer, GanttTask> foreignId2nativeTask) {
        myTaskCustomPropertyMapping = new HashMap<TaskField, CustomPropertyDefinition>();
        for (Task t: foreignProject.getChildTasks()) {
      		importTask(foreignProject, t, getTaskManager().getRootTask(), foreignId2nativeTask);
        }
    }

    private void importTask(ProjectFile foreignProject,
            Task t, net.sourceforge.ganttproject.task.Task supertask,
            Map<Integer, GanttTask> foreignId2nativeTask) {
    	if (t.getUniqueID() == 0) { 
            for (Task child: t.getChildTasks()) {
                importTask(foreignProject, child, getTaskManager().getRootTask(), foreignId2nativeTask);
            }
            return;
    	}
    		
        GanttTask nativeTask = getTaskManager().createTask();
        myNativeProject.getTaskContainment().move(nativeTask, supertask);
        nativeTask.setName(t.getName());
        nativeTask.setStart(convertStartTime(t.getStart()));
        nativeTask.setNotes(t.getNotes());
        nativeTask.setWebLink(t.getHyperlink());
        nativeTask.setPriority(convertPriority(t.getPriority()));
        if (t.getChildTasks().isEmpty()) {
            if (t.getPercentageComplete() != null) {
                nativeTask.setCompletionPercentage(t.getPercentageComplete().intValue());
            }
            nativeTask.setMilestone(t.getMilestone());
            nativeTask.setDuration(convertDuration(t));
        }
        else {
            for (Task child: t.getChildTasks()) {
                importTask(foreignProject, child, nativeTask, foreignId2nativeTask);
            }
        }
        importCustomFields(t, nativeTask);
        foreignId2nativeTask.put(t.getID(), nativeTask);
    }

    private GanttCalendar convertStartTime(Date start) {
		return new GanttCalendar(myNativeProject.getTimeUnitStack().getDefaultTimeUnit().adjustLeft(start));
	}

	private void importCustomFields(Task t, GanttTask nativeTask) {
        for (TaskField tf : TaskField.values()) {
            if (!isCustomField(tf) || t.getCurrentValue(tf) == null) {
                continue;
            }
            CustomPropertyDefinition def = myTaskCustomPropertyMapping.get(tf);
            if (def == null) {
                String typeAsString = convertDataType(tf);
                String name = t.getParentFile().getTaskFieldAlias(tf);
                if (name == null) {
                    name = tf.getName();
                }
                def = myNativeProject.getTaskCustomColumnManager().createDefinition(
                        typeAsString, name, null);
                myTaskCustomPropertyMapping.put(tf, def);
            }
            try {
                nativeTask.getCustomValues().setValue(
                        def.getName(), convertDataValue(tf, t.getCurrentValue(tf)));
            } catch (CustomColumnsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static Pattern CUSTOM_FIELD_NAME = Pattern.compile("^\\p{Lower}+\\p{Digit}+$");
    private boolean isCustomField(FieldType tf) {
        return tf != null && tf.getName() != null 
            && ProjectFileImporter.CUSTOM_FIELD_NAME.matcher(tf.getName().toLowerCase()).matches();
    }

    private String convertDataType(FieldType tf) {
        switch (tf.getDataType()) {
        case ACCRUE:
        case CONSTRAINT:
        case DURATION:
        case PRIORITY:
        case RELATION_LIST:
        case RESOURCE_TYPE:
        case STRING:
        case TASK_TYPE:
        case UNITS:
            return CustomPropertyClass.TEXT.name().toLowerCase();
        case BOOLEAN:
            return CustomPropertyClass.BOOLEAN.name().toLowerCase();
        case DATE:
            return CustomPropertyClass.DATE.name().toLowerCase();
        case CURRENCY:
        case NUMERIC:
        case PERCENTAGE:
        case RATE:
            return CustomPropertyClass.DOUBLE.name().toLowerCase();
        }
        return null;
    }

    private Object convertDataValue(FieldType tf, Object value) {
        switch (tf.getDataType()) {
        case ACCRUE:
        case CONSTRAINT:
        case DURATION:
        case PRIORITY:
        case RELATION_LIST:
        case RESOURCE_TYPE:
        case STRING:
        case TASK_TYPE:
        case UNITS:
            return String.valueOf(value);
        case BOOLEAN:
            assert value instanceof Boolean;
            return value;
        case DATE:
            assert value instanceof Date;
            return new GanttCalendar((Date)value);
        case CURRENCY:
        case NUMERIC:
        case PERCENTAGE:
            assert value instanceof Number;
            return ((Number)value).doubleValue();
        case RATE:
            assert value instanceof Rate;
            return ((Rate)value).getAmount();
        }
        return null;
    }

    private Priority convertPriority(net.sf.mpxj.Priority priority) {
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
        WorkingUnitCounter unitCounter = new WorkingUnitCounter(
                getNativeCalendar(), myNativeProject.getTimeUnitStack().getDefaultTimeUnit());
        return unitCounter.run(t.getStart(), t.getFinish());
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
                	// TODO(dbarashev): get rid of days
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
            HumanResource nativeResource = foreignId2nativeResource.get(ra.getResource().getUniqueID());
            net.sourceforge.ganttproject.task.ResourceAssignment nativeAssignment =
                nativeTask.getAssignmentCollection().addAssignment(nativeResource);
            nativeAssignment.setLoad(ra.getUnits().floatValue());
        }
    }
}
