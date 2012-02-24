/*
GanttProject is an opensource project management tool. License: GPL3
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.calendar.walker.WorkingUnitCounter;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.Task.Priority;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;
import net.sourceforge.ganttproject.util.collect.Pair;

import com.google.common.collect.Lists;

class ProjectFileImporter {
    private final IGanttProject myNativeProject;
    private final ProjectReader myReader;
    private final File myForeignFile;
    private Map<ResourceField, CustomPropertyDefinition> myResourceCustomPropertyMapping;
    private Map<TaskField, CustomPropertyDefinition> myTaskCustomPropertyMapping;
    private Map<String, Object> myCustomPropertyUniqueValueMapping = new HashMap<String, Object>();
    private TableHeaderUIFacade myTaskFields;
    private List<String> myErrors = new ArrayList<String>();

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

    public ProjectFileImporter(IGanttProject nativeProject, TaskTreeUIFacade taskTreeUIFacade, File foreignProjectFile) {
        myNativeProject = nativeProject;
        myTaskFields = taskTreeUIFacade.getVisibleFields();
        myReader = ProjectFileImporter.createReader(foreignProjectFile);
        myForeignFile = foreignProjectFile;
    }

    private TaskManager getTaskManager() {
        return myNativeProject.getTaskManager();
    }

    private static InputStream createPatchedStream(final File inputFile)
            throws TransformerConfigurationException, TransformerFactoryConfigurationError, IOException {
        final Transformer transformer = SAXTransformerFactory.newInstance().newTransformer(
                new StreamSource(ProjectFileImporter.class.getResourceAsStream("/mspdi_fix.xsl")));
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        ByteArrayOutputStream transformationOut = new ByteArrayOutputStream();
        try {
            transformer.transform(new StreamSource(inputFile), new StreamResult(transformationOut));
        } catch (TransformerException e) {
            GPLogger.log(new RuntimeException("Failed to transform file=" + inputFile.getAbsolutePath(), e));
        }

        return new ByteArrayInputStream(transformationOut.toByteArray());
    }

    @SuppressWarnings("unused")
    private List<String> debugTransformation() throws MPXJException {
        try {
        BufferedReader is = new BufferedReader(new InputStreamReader(createPatchedStream(myForeignFile)));
        for (String s = is.readLine(); s!= null; s = is.readLine()) {
            System.out.println(s);
        }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<String> run() throws MPXJException {
        ProjectFile pf;
        try {
            pf = (myReader instanceof MSPDIReader) ? myReader.read(createPatchedStream(myForeignFile)) : myReader.read(myForeignFile);
        } catch (TransformerConfigurationException e) {
            throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath() + "<br>" + e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new MPXJException("Failed to create a transformer factory");
        } catch (IOException e) {
            throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath(), e);
        }
        Map<Integer, GanttTask> foreignId2nativeTask = new HashMap<Integer, GanttTask>();
        Map<Integer, HumanResource> foreignId2nativeResource = new HashMap<Integer, HumanResource>();
        importCalendar(pf);
        importResources(pf, foreignId2nativeResource);
        importTasks(pf, foreignId2nativeTask);
        hideCustomProperties();
        try {
            importDependencies(pf, foreignId2nativeTask);
            List<net.sourceforge.ganttproject.task.Task> leafTasks = Lists.newArrayList();
            for (GanttTask task : foreignId2nativeTask.values()) {
                if (!getTaskManager().getTaskHierarchy().hasNestedTasks(task)) {
                    leafTasks.add(task);
                }
            }
            myNativeProject.getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(leafTasks);
        } catch (TaskDependencyException e) {
            throw new MPXJException("Failed to import dependencies", e);
        }
        importResourceAssignments(pf, foreignId2nativeTask, foreignId2nativeResource);

        return myErrors;
    }

    private void hideCustomProperties() {
        for (Map.Entry<String, Object> it : myCustomPropertyUniqueValueMapping.entrySet()) {
            if (it.getValue() != null) {
                hideCustomColumn(it.getKey());
            }
        }
    }

    private void hideCustomColumn(String key) {
        for (int i = 0; i<myTaskFields.getSize(); i++) {
            if (key.equals(myTaskFields.getField(i).getName())) {
                myTaskFields.getField(i).setVisible(false);
            }
        }
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
                    @Override
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
            nativeResource.setCustomField(def, convertDataValue(rf, r.getCurrentValue(rf)));
        }
    }

    private void importDaysOff(Resource r, final HumanResource nativeResource) {
        ProjectCalendar c = r.getResourceCalendar();
        if (c == null) {
            return;
        }
        for (ProjectCalendarException e: c.getCalendarExceptions()) {
            importHolidays(e, new HolidayAdder() {
                @Override
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

        if ((t.getStart() == null || t.getFinish() == null) && t.getChildTasks().isEmpty()) {
            myErrors.add("Failed to import leaf task=" + t + " because its start or end date was null");
            return;
        }

        GanttTask nativeTask = getTaskManager().createTask();
        myNativeProject.getTaskContainment().move(nativeTask, supertask);
        nativeTask.setName(t.getName());
        nativeTask.setNotes(t.getNotes());
        nativeTask.setWebLink(t.getHyperlink());
        if (t.getPriority() != null) {
            nativeTask.setPriority(convertPriority(t.getPriority()));
        }
        if (t.getChildTasks().isEmpty()) {
            nativeTask.setStart(convertStartTime(t.getStart()));
            if (t.getPercentageComplete() != null) {
                nativeTask.setCompletionPercentage(t.getPercentageComplete().intValue());
            }
            nativeTask.setMilestone(t.getMilestone());
            Pair<TaskLength, TaskLength> durations = convertDuration(t);

            TaskLength workingDuration = durations.first();
            TaskLength nonWorkingDuration = durations.second();
            TaskLength defaultDuration = myNativeProject.getTaskManager().createLength(
                    myNativeProject.getTimeUnitStack().getDefaultTimeUnit(), 1.0f);

            if (!t.getMilestone()) {
                if (workingDuration.getLength() > 0) {
                    nativeTask.setDuration(workingDuration);
                } else if (nonWorkingDuration.getLength() > 0){
                    myErrors.add(MessageFormat.format(
                            "Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                            + "We set its duration to {6}",
                            t.getID(), t.getName(), t.getStart(), t.getFinish(), t.getMilestone(), workingDuration, nonWorkingDuration));
                    nativeTask.setDuration(nonWorkingDuration);
                } else {
                    myErrors.add(MessageFormat.format(
                            "Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                            + "We set its duration to default={7}",
                            t.getID(), t.getName(), t.getStart(), t.getFinish(), t.getMilestone(), workingDuration, nonWorkingDuration, defaultDuration));
                    nativeTask.setDuration(defaultDuration);
                }
            } else {
                nativeTask.setDuration(defaultDuration);
            }
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

                def = myNativeProject.getTaskCustomColumnManager().createDefinition(typeAsString, name, null);
                myTaskCustomPropertyMapping.put(tf, def);
            }
            try {
                Object value = convertDataValue(tf, t.getCurrentValue(tf));
                if (!myCustomPropertyUniqueValueMapping.containsKey(def.getName())) {
                    myCustomPropertyUniqueValueMapping.put(def.getName(), value);
                } else {
                    if (!value.equals(myCustomPropertyUniqueValueMapping.get(def.getName()))) {
                        myCustomPropertyUniqueValueMapping.put(def.getName(), null);
                    }
                }
                nativeTask.getCustomValues().setValue(def, value);
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

    private Pair<TaskLength, TaskLength> convertDuration(Task t) {
        if (t.getMilestone()) {
            return Pair.create(getTaskManager().createLength(1), null);
        }
        WorkingUnitCounter unitCounter = new WorkingUnitCounter(
                getNativeCalendar(), myNativeProject.getTimeUnitStack().getDefaultTimeUnit());
        TaskLength workingDuration = unitCounter.run(t.getStart(), t.getFinish());
        TaskLength nonWorkingDuration = unitCounter.getNonWorkingTime();
        return Pair.create(workingDuration, nonWorkingDuration);
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
                if (dependant == null) {
                    myErrors.add("Failed to import relation=" + t + " because source task=" + r.getSourceTask().getID() + " was not found");
                    continue;
                }
                if (dependee == null) {
                    myErrors.add("Failed to import relation=" + t + " because target task=" + r.getTargetTask().getID() + " was not found");
                    continue;
                }
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
            if (nativeTask == null) {
                myErrors.add("Failed to import resource assignment=" + ra
                        + " because its task with ID=" + ra.getTask().getID()
                        + " and name=" + ra.getTask().getName()
                        + " was not found or not imported");
                continue;
            }
            Resource resource = ra.getResource();
            if (resource == null) {
                continue;
            }
            HumanResource nativeResource = foreignId2nativeResource.get(resource.getUniqueID());
            if (nativeResource == null) {
                myErrors.add("Failed to import resource assignment=" + ra
                        + " because its resource with ID=" + resource.getUniqueID()
                        + " and name=" + resource.getName()
                        + " was not found or not imported");
                continue;

            }
            net.sourceforge.ganttproject.task.ResourceAssignment nativeAssignment =
                nativeTask.getAssignmentCollection().addAssignment(nativeResource);
            nativeAssignment.setLoad(ra.getUnits().floatValue());
        }
    }
}
