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
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
import net.sf.mpxj.Duration;
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
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.Task.Priority;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;
import net.sourceforge.ganttproject.util.collect.Pair;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.calendar.walker.WorkingUnitCounter;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import biz.ganttproject.core.time.impl.GregorianTimeUnitStack;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

class ProjectFileImporter {
  private final IGanttProject myNativeProject;
  private final ProjectReader myReader;
  private final File myForeignFile;
  private Map<ResourceField, CustomPropertyDefinition> myResourceCustomPropertyMapping;
  private Map<TaskField, CustomPropertyDefinition> myTaskCustomPropertyMapping;
  private Map<String, Object> myCustomPropertyUniqueValueMapping = new HashMap<String, Object>();
  private ColumnList myTaskFields;
  private List<Pair<Level, String>> myErrors = Lists.newArrayList();
  private ProjectFile myProjectFile;

  private static ProjectReader createReader(File file) {
    int lastDot = file.getName().lastIndexOf('.');
    if (lastDot == file.getName().length() - 1) {
      return null;
    }
    String fileExt = file.getName().substring(lastDot + 1).toLowerCase();
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

  private static InputStream createPatchedStream(final File inputFile) throws TransformerConfigurationException,
      TransformerFactoryConfigurationError, IOException {
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
      for (String s = is.readLine(); s != null; s = is.readLine()) {
        System.out.println(s);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  public List<Pair<Level, String>> run() throws MPXJException {
    ProjectFile pf;
    try {
      pf = (myReader instanceof MSPDIReader) ? myReader.read(createPatchedStream(myForeignFile))
          : myReader.read(myForeignFile);
    } catch (TransformerConfigurationException e) {
      throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath() + "<br>" + e.getMessage(),
          e);
    } catch (TransformerFactoryConfigurationError e) {
      throw new MPXJException("Failed to create a transformer factory");
    } catch (IOException e) {
      throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath(), e);
    } catch (RuntimeException e) {
      throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath(), e);
    }
    myProjectFile = pf;
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
    for (int i = 0; i < myTaskFields.getSize(); i++) {
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
    for (ProjectCalendarException e : exceptions) {
      if (!e.getWorking()) {
        final List<CalendarEvent> holidays = Lists.newArrayList();
        importHolidays(e, new HolidayAdder() {
          @Override
          public void addHoliday(Date date) {
            holidays.add(CalendarEvent.newEvent(date, false, CalendarEvent.Type.HOLIDAY, null));
          }
        });
        getNativeCalendar().setPublicHolidays(holidays);
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
    getNativeCalendar().setWeekDayType(nativeDay,
        foreignCalendar.isWorkingDay(foreignDay) ? DayType.WORKING : DayType.WEEKEND);
  }

  private GPCalendarCalc getNativeCalendar() {
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
    TimeDuration oneDay = getTaskManager().createLength(GregorianTimeUnitStack.DAY, 1.0f);
    for (Date dayStart = start; !dayStart.after(end);) {
      // myNativeProject.getActiveCalendar().setPublicHoliDayType(dayStart);
      adder.addHoliday(dayStart);
      dayStart = GPCalendarCalc.PLAIN.shiftDate(dayStart, oneDay);
    }
  }

  private void importResources(ProjectFile pf, Map<Integer, HumanResource> foreignId2humanResource) {
    myResourceCustomPropertyMapping = new HashMap<ResourceField, CustomPropertyDefinition>();
    for (Resource r : pf.getAllResources()) {
      HumanResource nativeResource = myNativeProject.getHumanResourceManager().newHumanResource();
      nativeResource.setId(r.getUniqueID());
      nativeResource.setName(r.getName());
      nativeResource.setMail(r.getEmailAddress());
      Rate standardRate = r.getStandardRate();
      if (standardRate != null && standardRate.getAmount() != 0.0 && r.getStandardRateUnits() == TimeUnit.DAYS) {
        nativeResource.setStandardPayRate(new BigDecimal(standardRate.getAmount()));
      }
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
        def = myNativeProject.getResourceCustomPropertyManager().createDefinition(typeAsString, name, null);
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
    for (ProjectCalendarException e : c.getCalendarExceptions()) {
      importHolidays(e, new HolidayAdder() {
        @Override
        public void addHoliday(Date date) {
          nativeResource.addDaysOff(new GanttDaysOff(date, GregorianTimeUnitStack.DAY.adjustRight(date)));
        }
      });
    }
  }

  private void importTasks(ProjectFile foreignProject, Map<Integer, GanttTask> foreignId2nativeTask) {
    myTaskCustomPropertyMapping = new HashMap<TaskField, CustomPropertyDefinition>();
    for (Task t : foreignProject.getChildTasks()) {
      importTask(foreignProject, t, getTaskManager().getRootTask(), foreignId2nativeTask);
    }
  }

  private Function<Task, Pair<TimeDuration, TimeDuration>> findDurationFunction(Task t, StringBuilder reportBuilder) {
    if (t.getStart() != null && t.getFinish() != null) {
      return DURATION_FROM_START_FINISH;
    }
    reportBuilder.append("start+finish not found");
    if (t.getStart() != null && t.getDuration() != null) {
      return DURATION_FROM_START_AND_DURATION;
    }
    reportBuilder.append(", start+duration not found");
    return null;
  }


  private void importTask(ProjectFile foreignProject, Task t, net.sourceforge.ganttproject.task.Task supertask,
      Map<Integer, GanttTask> foreignId2nativeTask) {
    if (t.getNull()) {
      myErrors.add(Pair.create(Level.INFO,
          MessageFormat.format("Task with id={0} is blank task. Skipped", foreignId(t))));
      return;
    }
    if (t.getUniqueID() == 0) {
      boolean isRealTask = t.getName() != null && !t.getChildTasks().isEmpty();
      if (!isRealTask) {
        for (Task child : t.getChildTasks()) {
          importTask(foreignProject, child, getTaskManager().getRootTask(), foreignId2nativeTask);
        }
        return;
      }
    }

    StringBuilder report = new StringBuilder();
    Function<Task, Pair<TimeDuration, TimeDuration>> getDuration = findDurationFunction(t, report);
    if (getDuration == null) {
      myErrors.add(Pair.create(Level.SEVERE,
          String.format("Can't determine the duration  of task %s (%s). Skipped", t, report)));
      return;
    }

    TaskBuilder taskBuilder = getTaskManager().newTaskBuilder()
        .withParent(supertask)
        .withName(t.getName())
        .withNotes(t.getNotes())
        .withWebLink(t.getHyperlink());
    if (t.getPriority() != null) {
      taskBuilder = taskBuilder.withPriority(convertPriority(t.getPriority()));
    }
    if (t.getChildTasks().isEmpty()) {
      taskBuilder.withStartDate(convertStartTime(t.getStart()));
      if (t.getPercentageComplete() != null) {
        taskBuilder.withCompletion(t.getPercentageComplete().intValue());
      }
      if (t.getMilestone()) {
        taskBuilder.withLegacyMilestone();
      }
      Pair<TimeDuration, TimeDuration> durations = getDuration.apply(t);

      TimeDuration workingDuration = durations.first();
      TimeDuration nonWorkingDuration = durations.second();
      TimeDuration defaultDuration = myNativeProject.getTaskManager().createLength(
          myNativeProject.getTimeUnitStack().getDefaultTimeUnit(), 1.0f);

      if (!t.getMilestone()) {
        if (workingDuration.getLength() > 0) {
          taskBuilder.withDuration(workingDuration);
        } else if (nonWorkingDuration.getLength() > 0) {
          myErrors.add(Pair.create(Level.INFO, MessageFormat.format(
              "[FYI] Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                  + "We set its duration to {6}", foreignId(t), t.getName(), t.getStart(), t.getFinish(),
              t.getMilestone(), workingDuration, nonWorkingDuration)));
          taskBuilder.withDuration(nonWorkingDuration);
        } else {
          myErrors.add(Pair.create(Level.INFO, MessageFormat.format(
              "[FYI] Task with id={0}, name={1}, start date={2}, end date={3}, milestone={4} has working time={5} and non working time={6}.\n"
                  + "We set its duration to default={7}", foreignId(t), t.getName(), t.getStart(), t.getFinish(),
              t.getMilestone(), workingDuration, nonWorkingDuration, defaultDuration)));
          taskBuilder.withDuration(defaultDuration);
        }
      } else {
        taskBuilder.withDuration(defaultDuration);
      }
    }
    GanttTask nativeTask = (GanttTask) taskBuilder.build();
    if (t.getCost() != null) {
      nativeTask.getCost().setCalculated(false);
      nativeTask.getCost().setValue(BigDecimal.valueOf(t.getCost().doubleValue()));
    }
    if (!t.getChildTasks().isEmpty()) {
      for (Task child : t.getChildTasks()) {
        importTask(foreignProject, child, nativeTask, foreignId2nativeTask);
      }
    }
    importCustomFields(t, nativeTask);
    foreignId2nativeTask.put(foreignId(t), nativeTask);
  }

  private Date convertStartTime(Date start) {
    return myNativeProject.getTimeUnitStack().getDefaultTimeUnit().adjustLeft(start);
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
      return CalendarFactory.createGanttCalendar((Date) value);
    case CURRENCY:
    case NUMERIC:
    case PERCENTAGE:
      assert value instanceof Number;
      return ((Number) value).doubleValue();
    case RATE:
      assert value instanceof Rate;
      return ((Rate) value).getAmount();
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

  private Pair<TimeDuration, TimeDuration> getDurations(Date start, Date end) {
    WorkingUnitCounter unitCounter = new WorkingUnitCounter(getNativeCalendar(),
        myNativeProject.getTimeUnitStack().getDefaultTimeUnit());
    TimeDuration workingDuration = unitCounter.run(start, end);
    TimeDuration nonWorkingDuration = unitCounter.getNonWorkingTime();
    return Pair.create(workingDuration, nonWorkingDuration);

  }
  private final Function<Task, Pair<TimeDuration, TimeDuration>> DURATION_FROM_START_FINISH =
      new Function<Task, Pair<TimeDuration,TimeDuration>>() {
        @Override
        public Pair<TimeDuration, TimeDuration> apply(Task t) {
          if (t.getMilestone()) {
            return Pair.create(getTaskManager().createLength(1), null);
          }
          return getDurations(t.getStart(), myNativeProject.getTimeUnitStack().getDefaultTimeUnit().adjustRight(t.getFinish()));
        }
      };

  private final Function<Task, Pair<TimeDuration, TimeDuration>> DURATION_FROM_START_AND_DURATION =
      new Function<Task, Pair<TimeDuration,TimeDuration>>() {
        @Override
        public Pair<TimeDuration, TimeDuration> apply(Task t) {
          if (t.getMilestone()) {
            return Pair.create(getTaskManager().createLength(1), null);
          }
          Duration dayUnits = t.getDuration().convertUnits(TimeUnit.DAYS, myProjectFile.getProjectHeader());
          TimeDuration gpDuration = getTaskManager().createLength(GPTimeUnitStack.DAY, (float) dayUnits.getDuration());
          Date endDate = getTaskManager().shift(t.getStart(), gpDuration);
          return getDurations(t.getStart(), endDate);
        }
      };

  private static Integer foreignId(Task mpxjTask) {
    Integer result = mpxjTask.getID();
    if (result != null) {
      return result;
    }
    result = mpxjTask.getUniqueID();
    if (result != null) {
      return result;
    }
    throw new IllegalStateException("No ID found in task=" + mpxjTask);
  }
  private void importDependencies(ProjectFile pf, Map<Integer, GanttTask> foreignId2nativeTask)
      throws TaskDependencyException {
    for (Task t : pf.getAllTasks()) {
      if (t.getPredecessors() == null) {
        continue;
      }
      for (Relation r : t.getPredecessors()) {
        GanttTask dependant = foreignId2nativeTask.get(foreignId(r.getSourceTask()));
        GanttTask dependee = foreignId2nativeTask.get(foreignId(r.getTargetTask()));
        if (dependant == null) {
          myErrors.add(Pair.create(Level.SEVERE, String.format(
              "Failed to import relation=%s because source task=%s was not found", r, foreignId(r.getSourceTask()))));
          continue;
        }
        if (dependee == null) {
          myErrors.add(Pair.create(Level.SEVERE, String.format(
              "Failed to import relation=%s because target task=%s", t, foreignId(r.getTargetTask()))));
          continue;
        }
        TaskDependency dependency = getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
        dependency.setConstraint(convertConstraint(r));
        if (r.getLag().getDuration() != 0.0) {
          // TODO(dbarashev): get rid of days
          dependency.setDifference((int) r.getLag().convertUnits(TimeUnit.DAYS, pf.getProjectHeader()).getDuration());
        }
        dependency.setHardness(TaskDependency.Hardness.parse(getTaskManager().getDependencyHardnessOption().getValue()));
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

  private void importResourceAssignments(ProjectFile pf, Map<Integer, GanttTask> foreignId2nativeTask,
      Map<Integer, HumanResource> foreignId2nativeResource) {
    for (ResourceAssignment ra : pf.getAllResourceAssignments()) {
      GanttTask nativeTask = foreignId2nativeTask.get(foreignId(ra.getTask()));
      if (nativeTask == null) {
        myErrors.add(Pair.create(Level.SEVERE, String.format(
            "Failed to import resource assignment=%s because its task with ID=%d  and name=%s was not found or not imported", ra, foreignId(ra.getTask()), ra.getTask().getName())));
        continue;
      }
      Resource resource = ra.getResource();
      if (resource == null) {
        continue;
      }
      HumanResource nativeResource = foreignId2nativeResource.get(resource.getUniqueID());
      if (nativeResource == null) {
        myErrors.add(Pair.create(Level.SEVERE, String.format(
            "Failed to import resource assignment=%s because its resource with ID=%d and name=%s was not found or not imported", ra, resource.getUniqueID(), resource.getName())));
        continue;

      }
      net.sourceforge.ganttproject.task.ResourceAssignment nativeAssignment = nativeTask.getAssignmentCollection().addAssignment(
          nativeResource);
      Preconditions.checkNotNull(nativeAssignment);
      if (ra.getUnits() == null) {
        myErrors.add(Pair.create(Level.INFO, String.format(
            "Units not found in resource assignment=%s. Replaced with 100", ra)));
        nativeAssignment.setLoad(100f);
      } else {
        nativeAssignment.setLoad(ra.getUnits().floatValue());
      }
    }
  }
}
