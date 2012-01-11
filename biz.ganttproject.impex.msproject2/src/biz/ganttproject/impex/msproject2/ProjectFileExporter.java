/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.Duration;
import net.sf.mpxj.FieldType;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.Priority;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.ResourceType;
import net.sf.mpxj.TaskField;
import net.sf.mpxj.TaskMode;
import net.sf.mpxj.TimeUnit;
import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

/**
 * Creates MPXJ ProjectFile from GanttProject's IGanttProject.
 * @author dbarashev (Dmitry Barashev)
 */
class ProjectFileExporter {
    private IGanttProject myNativeProject;
    private ProjectFile myOutputProject;

    public ProjectFileExporter(IGanttProject nativeProject) {
        myNativeProject = nativeProject;
        myOutputProject = new ProjectFile();
        myOutputProject.setAutoOutlineLevel(true);
        myOutputProject.setAutoWBS(true);
        myOutputProject.setAutoOutlineNumber(true);
    }

    ProjectFile run() throws MPXJException {
        Map<Integer, net.sf.mpxj.Task> id2mpxjTask = new HashMap<Integer, net.sf.mpxj.Task>();
        exportCalendar();
        exportTasks(id2mpxjTask);
        exportDependencies(id2mpxjTask);

        Map<Integer, Resource> id2mpxjResource = new HashMap<Integer, Resource>();
        exportResources(id2mpxjResource);

        exportAssignments(id2mpxjTask, id2mpxjResource);
        return myOutputProject;
    }

    private void exportCalendar() {
        ProjectCalendar calendar = myOutputProject.addDefaultBaseCalendar();
        exportWeekends(calendar);
        exportHolidays(calendar);
    }

    private void exportWeekends(ProjectCalendar calendar) {
        ProjectCalendarHours workingDayHours = calendar.getCalendarHours(Day.MONDAY);
        calendar.setWorkingDay(Day.MONDAY, getCalendar().getWeekDayType(Calendar.MONDAY) == DayType.WORKING);
        calendar.setWorkingDay(Day.TUESDAY, getCalendar().getWeekDayType(Calendar.TUESDAY) == DayType.WORKING);
        calendar.setWorkingDay(Day.WEDNESDAY, getCalendar().getWeekDayType(Calendar.WEDNESDAY) == DayType.WORKING);
        calendar.setWorkingDay(Day.THURSDAY, getCalendar().getWeekDayType(Calendar.THURSDAY) == DayType.WORKING);
        calendar.setWorkingDay(Day.FRIDAY, getCalendar().getWeekDayType(Calendar.FRIDAY) == DayType.WORKING);
        calendar.setWorkingDay(Day.SATURDAY, getCalendar().getWeekDayType(Calendar.SATURDAY) == DayType.WORKING);
        if (calendar.isWorkingDay(Day.SATURDAY)) {
            copyHours(workingDayHours, calendar.addCalendarHours(Day.SATURDAY));
        }
        calendar.setWorkingDay(Day.SUNDAY, getCalendar().getWeekDayType(Calendar.SUNDAY) == DayType.WORKING);
        if (calendar.isWorkingDay(Day.SUNDAY)) {
            copyHours(workingDayHours, calendar.addCalendarHours(Day.SUNDAY));
        }
    }

    private void copyHours(ProjectCalendarHours from, ProjectCalendarHours to) {
        for (DateRange range : from) {
            to.addRange(range);
        }
    }

    private void exportHolidays(ProjectCalendar calendar) {
        for (Date d : getCalendar().getPublicHolidays()) {
            ProjectCalendarException calendarException = calendar.addCalendarException(d, d);
            calendarException.addRange(new DateRange(d, d));
        }
    }

    private void exportTasks(Map<Integer, net.sf.mpxj.Task> id2mpxjTask) {
        Map<CustomPropertyDefinition, FieldType> customProperty_fieldType = new HashMap<CustomPropertyDefinition, FieldType>();
        collectCustomProperties(getTaskManager().getCustomPropertyManager(), customProperty_fieldType, TaskField.class);
        for (Entry<CustomPropertyDefinition, FieldType> e : customProperty_fieldType.entrySet()) {
            myOutputProject.setTaskFieldAlias((TaskField)e.getValue(), e.getKey().getName());
        }
        net.sf.mpxj.Task rootTask = myOutputProject.addTask();
        rootTask.setID(0);
        rootTask.setUniqueID(0);
        rootTask.setOutlineLevel(0);
        rootTask.setWBS("0");
        rootTask.setOutlineNumber("0");
        rootTask.setStart(convertStartTime(getTaskManager().getProjectStart()));
        rootTask.setFinish(convertFinishTime(getTaskManager().getProjectEnd()));
        rootTask.setDuration(convertDuration(getTaskManager().createLength(
                getTaskManager().getRootTask().getDuration().getTimeUnit(),
                getTaskManager().getProjectStart(), getTaskManager().getProjectEnd())));
        //rootTask.setDurationFormat(TimeUnit.DAYS);
        rootTask.setTaskMode(TaskMode.AUTO_SCHEDULED);

        int i = 0;
        for (Task t : getTaskHierarchy().getNestedTasks(getTaskHierarchy().getRootTask())) {
            exportTask(t, null, 1, ++i, id2mpxjTask, customProperty_fieldType);
        }
    }

    private void exportTask(Task t, net.sf.mpxj.Task mpxjParentTask,
            int outlineLevel, int ordinalNum,
            Map<Integer, net.sf.mpxj.Task> id2mpxjTask, Map<CustomPropertyDefinition, FieldType> customProperty_fieldType) {
        final net.sf.mpxj.Task mpxjTask = mpxjParentTask == null ? myOutputProject.addTask() : mpxjParentTask.addTask();
        mpxjTask.setOutlineLevel(outlineLevel);
        String wbs = (mpxjParentTask==null ? "" : mpxjParentTask.getWBS() + ".") + String.valueOf(ordinalNum);
        mpxjTask.setWBS(wbs);
        mpxjTask.setOutlineNumber(wbs);
        mpxjTask.setUniqueID(convertTaskId(t.getTaskID()));
        mpxjTask.setID(id2mpxjTask.size() + 1);
        mpxjTask.setName(t.getName());
        mpxjTask.setNotes(t.getNotes());
        mpxjTask.setMilestone(t.isMilestone());
        mpxjTask.setPercentageComplete(t.getCompletionPercentage());
        mpxjTask.setHyperlink(((GanttTask)t).getWebLink());
        mpxjTask.setIgnoreResourceCalendar(true);

        Task[] nestedTasks = getTaskHierarchy().getNestedTasks(t);
        if (nestedTasks.length > 0) {
            mpxjTask.setSummary(true);
//            mpxjTask.setTaskMode(TaskMode.AUTO_SCHEDULED);
        } else {
        }
        mpxjTask.setTaskMode(TaskMode.MANUALLY_SCHEDULED);

        mpxjTask.setStart(convertStartTime(t.getStart().getTime()));
        mpxjTask.setFinish(convertFinishTime(t.getEnd().getTime()));
        mpxjTask.setDuration(convertDuration(t.getDuration()));
        //mpxjTask.setDurationFormat(TimeUnit.DAYS);
        Duration[] durations = getActualAndRemainingDuration(mpxjTask);
        mpxjTask.setActualDuration(durations[0]);
        mpxjTask.setRemainingDuration(durations[1]);
        mpxjTask.setPriority(convertPriority(t));

        exportCustomProperties(t.getCustomValues(), customProperty_fieldType, new CustomPropertySetter() {
            @Override
            public void set(FieldType ft, Object value) {
                mpxjTask.set(ft, value);
            }
        });
        id2mpxjTask.put(mpxjTask.getUniqueID(), mpxjTask);

        int i = 0;
        for (Task child : nestedTasks) {
            exportTask(child, mpxjTask, outlineLevel + 1, ++i, id2mpxjTask, customProperty_fieldType);
        }

    }

    private Date convertStartTime(Date gpStartDate) {
        Date startTime = myOutputProject.getCalendar().getStartTime(gpStartDate);
        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(gpStartDate);
        c.set(Calendar.HOUR, startTime.getHours());
        c.set(Calendar.MINUTE, startTime.getMinutes());
        return c.getTime();
    }

    private Date convertFinishTime(Date gpFinishDate) {
        Calendar c = (Calendar) Calendar.getInstance().clone();
        c.setTime(gpFinishDate);
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date finishTime = myOutputProject.getCalendar().getFinishTime(c.getTime());
        c.set(Calendar.HOUR, finishTime.getHours());
        c.set(Calendar.MINUTE, finishTime.getMinutes());
        return c.getTime();

    }
    private Duration convertDuration(TaskLength duration) {
        return Duration.getInstance(duration.getLength() * 8, TimeUnit.HOURS);
    }

    private static Duration[] getActualAndRemainingDuration(net.sf.mpxj.Task mpxjTask) {
        TimeUnit durationUnits = mpxjTask.getDuration().getUnits();
        double actualWork = (mpxjTask.getDuration().getDuration() * mpxjTask.getPercentageComplete().doubleValue()) / 100;
        double remainingWork = mpxjTask.getDuration().getDuration() - actualWork;

        return new Duration[] {
                Duration.getInstance(actualWork, durationUnits),
                Duration.getInstance(remainingWork, durationUnits)};

    }
    private void exportDependencies(Map<Integer, net.sf.mpxj.Task> id2mpxjTask) {
        for (Task t : getTaskManager().getTasks()) {
            net.sf.mpxj.Task mpxjTask = id2mpxjTask.get(convertTaskId(t.getTaskID()));

            TaskDependencySlice dependencies = t.getDependenciesAsDependant();
            for (TaskDependency dep : dependencies.toArray()) {
                net.sf.mpxj.Task mpxjPredecessor = id2mpxjTask.get(convertTaskId(dep.getDependee().getTaskID()));
                assert mpxjPredecessor != null : "Can't find mpxj task for id=" + dep.getDependee().getTaskID();
                mpxjTask.addPredecessor(mpxjPredecessor, convertConstraint(dep), convertLag(dep));
            }
        }
    }

    private RelationType convertConstraint(TaskDependency dep) {
        switch (TaskDependencyConstraint.Type.getType(dep.getConstraint())) {
        case finishstart:
            return RelationType.FINISH_START;
        case startfinish:
            return RelationType.START_FINISH;
        case finishfinish:
            return RelationType.FINISH_FINISH;
        case startstart:
            return RelationType.START_START;
        default:
            assert false : "Should not be here";
            return null;
        }
    }

    private static Duration convertLag(TaskDependency dep) {
        // TODO(dbarashev): Get rid of days
        return Duration.getInstance(dep.getDifference(), TimeUnit.DAYS);
    }

    private Priority convertPriority(Task t) {
        switch (t.getPriority()) {
        case LOWEST:
            return Priority.getInstance(Priority.LOWEST);
        case LOW:
            return Priority.getInstance(Priority.LOW);
        case NORMAL:
            return Priority.getInstance(Priority.MEDIUM);
        case HIGH:
            return Priority.getInstance(Priority.HIGH);
        case HIGHEST:
            return Priority.getInstance(Priority.HIGHEST);
        default:
            assert false : "Should not be here";
            return Priority.getInstance(Priority.MEDIUM);
        }
    }

    private int convertTaskId(int taskId) {
        return taskId == 0 ? getMaxTaskID() + 1 : taskId;
    }

    private int getMaxTaskID() {
        int maxID = 0;
        for (Task t : getTaskManager().getTasks()) {
            if (t.getTaskID() > maxID) {
                maxID = t.getTaskID();
            }
        }
        return maxID;
    }
    private void exportResources(Map<Integer, Resource> id2mpxjResource) throws MPXJException {
        Map<CustomPropertyDefinition, FieldType> customProperty_fieldType = new HashMap<CustomPropertyDefinition, FieldType>();
        collectCustomProperties(getResourceManager().getCustomPropertyManager(), customProperty_fieldType, ResourceField.class);
        for (Entry<CustomPropertyDefinition, FieldType> e : customProperty_fieldType.entrySet()) {
            myOutputProject.setResourceFieldAlias((ResourceField)e.getValue(), e.getKey().getName());
        }
        for (HumanResource hr : getResourceManager().getResources()) {
            exportResource(hr, id2mpxjResource, customProperty_fieldType);
        }
    }

    private void exportResource(HumanResource hr, Map<Integer, Resource> id2mpxjResource,
            Map<CustomPropertyDefinition, FieldType> customProperty_fieldType) throws MPXJException {
        final Resource mpxjResource = myOutputProject.addResource();
        mpxjResource.setUniqueID(hr.getId());
        mpxjResource.setID(id2mpxjResource.size() + 1);
        mpxjResource.setName(hr.getName());
        mpxjResource.setEmailAddress(hr.getMail());
        mpxjResource.setType(ResourceType.WORK);

        exportDaysOff(hr, mpxjResource);
        exportCustomProperties(hr, customProperty_fieldType, new CustomPropertySetter() {
            @Override
            public void set(FieldType ft, Object value) {
                mpxjResource.set(ft, value);
            }
        });
        id2mpxjResource.put(hr.getId(), mpxjResource);
    }

    private static <T extends Enum<T>> void collectCustomProperties(CustomPropertyManager customPropertyManager,
            Map<CustomPropertyDefinition, FieldType> customProperty_fieldType, Class<T> fieldTypeClass) {
        Map<String, Integer> typeCounter = new HashMap<String, Integer>();
        for (CustomPropertyDefinition def : customPropertyManager.getDefinitions()) {
            Integer count = typeCounter.get(def.getTypeAsString());
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            typeCounter.put(def.getTypeAsString(), count);
            FieldType ft = getFieldType(fieldTypeClass, def, count);
            customProperty_fieldType.put(def, ft);
        }
    }

    private static <T extends Enum<T>> FieldType getFieldType(Class<T> enumClass, CustomPropertyDefinition def, Integer count) {
        String name;
        switch (def.getPropertyClass()) {
        case BOOLEAN:
            name = "FLAG";
            break;
        case INTEGER:
        case DOUBLE:
            name = "NUMBER";
            break;
        case TEXT:
            name = "TEXT";
            break;
        case DATE:
            name = "DATE";
            break;
        default:
            assert false : "Should not be here";
            name = "TEXT";
        }
        try {
            return (FieldType) Enum.valueOf(enumClass, name + count);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static interface CustomPropertySetter {
        void set(FieldType ft, Object value);
    }

    private static void exportCustomProperties(
            CustomPropertyHolder holder, Map<CustomPropertyDefinition, FieldType> customProperty_fieldType, CustomPropertySetter setter) {
        for (CustomProperty cp : holder.getCustomProperties()) {
            FieldType ft = customProperty_fieldType.get(cp.getDefinition());
            if (ft != null) {
                setter.set(ft, convertValue(cp));
            }
        }
    }

    private static Object convertValue(CustomProperty cp) {
        if (cp.getDefinition().getPropertyClass() == CustomPropertyClass.DATE) {
            GanttCalendar value = (GanttCalendar) cp.getValue();
            return value.getTime();
        }
        return cp.getValue();
    }

    private void exportDaysOff(HumanResource hr, Resource mpxjResource) throws MPXJException {
        DefaultListModel daysOff = hr.getDaysOff();
        if (!daysOff.isEmpty()) {
            ProjectCalendar resourceCalendar = mpxjResource.addResourceCalendar();
            resourceCalendar.setBaseCalendar(myOutputProject.getCalendar());
//            resourceCalendar.setUniqueID(hr.getId());
            for (int i = 0; i < daysOff.size(); i++) {
                GanttDaysOff dayOff = (GanttDaysOff) daysOff.get(i);
                resourceCalendar.addCalendarException(dayOff.getStart().getTime(), dayOff.getFinish().getTime());
            }
        }
    }

    private void exportAssignments(Map<Integer, net.sf.mpxj.Task> id2mpxjTask, Map<Integer, Resource> id2mpxjResource) {
        for (Task t : getTaskManager().getTasks()) {
            net.sf.mpxj.Task mpxjTask = id2mpxjTask.get(convertTaskId(t.getTaskID()));
            for (ResourceAssignment ra : t.getAssignments()) {
                Resource mpxjResource = id2mpxjResource.get(ra.getResource().getId());
                net.sf.mpxj.ResourceAssignment mpxjAssignment = mpxjTask.addResourceAssignment(mpxjResource);
                mpxjAssignment.setUnits(ra.getLoad());
                mpxjAssignment.setStart(mpxjTask.getStart());
                mpxjAssignment.setFinish(mpxjTask.getFinish());

                mpxjAssignment.setWork(mpxjTask.getDuration());
                Duration[] durations = getActualAndRemainingDuration(mpxjTask);
                mpxjAssignment.setActualWork(durations[0]);
                mpxjAssignment.setRemainingWork(durations[1]);
            }
        }
    }

    private TaskManager getTaskManager() {
        return myNativeProject.getTaskManager();
    }

    private TaskContainmentHierarchyFacade getTaskHierarchy() {
        return getTaskManager().getTaskHierarchy();
    }

    private HumanResourceManager getResourceManager() {
        return myNativeProject.getHumanResourceManager();
    }

    private GPCalendar getCalendar() {
        return getTaskManager().getCalendar();
    }
}
