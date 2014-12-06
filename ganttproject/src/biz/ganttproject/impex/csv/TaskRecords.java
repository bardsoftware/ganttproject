/*
Copyright 2014 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.impex.csv;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import org.apache.commons.csv.CSVRecord;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.time.TimeUnitStack;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class responsible for processing task records in CSV import
 *
 * @author dbarashev (Dmitry Barashev)
 */
class TaskRecords extends RecordGroup {
  /** List of known (and supported) Task attributes */
  static enum TaskFields {
    ID(TaskDefaultColumn.ID.getNameKey()),
    NAME("tableColName"), BEGIN_DATE("tableColBegDate"), END_DATE("tableColEndDate"), WEB_LINK("webLink"),
    NOTES("notes"), COMPLETION("tableColCompletion"), RESOURCES("resources"), DURATION("tableColDuration"),
    PREDECESSORS(TaskDefaultColumn.PREDECESSORS.getNameKey()), OUTLINE_NUMBER(TaskDefaultColumn.OUTLINE_NUMBER.getNameKey());

    private final String text;

    private TaskFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return GanttLanguage.getInstance().getText(text);
    }
  }
  private final Map<Task, String> myAssignmentMap = Maps.newHashMap();
  private final Map<Task, String> myPredecessorMap = Maps.newHashMap();
  private final SortedMap<String, Task> myWbsMap = Maps.newTreeMap(new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
      return - (s1.compareTo(s2));
    }
  });
  private final Map<String, Task> myTaskIdMap = Maps.newHashMap();
  private final TaskManager taskManager;
  private final HumanResourceManager resourceManager;
  private final TimeUnitStack myTimeUnitStack;

  TaskRecords(TaskManager taskManager, HumanResourceManager resourceManager, TimeUnitStack timeUnitStack) {
    super("Task group",
      Sets.newHashSet(GanttCSVOpen.getFieldNames(TaskFields.values())),
      Sets.newHashSet(GanttCSVOpen.getFieldNames(TaskFields.NAME, TaskFields.BEGIN_DATE)));
    this.taskManager = taskManager;
    this.resourceManager = resourceManager;
    myTimeUnitStack = timeUnitStack;
  }

  @Override
  public void setHeader(List<String> header) {
    super.setHeader(header);
    GanttCSVOpen.createCustomProperties(getCustomFields(), taskManager.getCustomPropertyManager());
  }

  @Override
  protected boolean doProcess(CSVRecord record) {
    if (!super.doProcess(record)) {
      return false;
    }
    if (!hasMandatoryFields(record)) {
      return false;
    }
    // Create the task
    TaskManager.TaskBuilder builder = taskManager.newTaskBuilder()
        .withName(getOrNull(record, TaskFields.NAME.toString()))
        .withStartDate(GanttCSVOpen.language.parseDate(getOrNull(record, TaskFields.BEGIN_DATE.toString())))
        .withWebLink(getOrNull(record, TaskFields.WEB_LINK.toString()))
        .withNotes(getOrNull(record, TaskFields.NOTES.toString()));
    if (record.isSet(TaskDefaultColumn.DURATION.getName())) {
      builder = builder.withDuration(taskManager.createLength(record.get(TaskDefaultColumn.DURATION.getName())));
    }
    if (record.isSet(TaskFields.END_DATE.toString())) {
      if (record.isSet(TaskDefaultColumn.DURATION.getName())) {
        if (Objects.equal(record.get(TaskFields.BEGIN_DATE.toString()), record.get(TaskFields.END_DATE.toString()))
            && "0".equals(record.get(TaskDefaultColumn.DURATION.getName()))) {
          builder = builder.withLegacyMilestone();
        }
      } else {
        Date endDate = GanttCSVOpen.language.parseDate(getOrNull(record, TaskFields.END_DATE.toString()));
        builder = builder.withEndDate(myTimeUnitStack.getDefaultTimeUnit().adjustRight(endDate));
      }
    }
    if (record.isSet(TaskFields.COMPLETION.toString())) {
      String completion = record.get(TaskFields.COMPLETION.toString());
      if (!Strings.isNullOrEmpty(completion)) {
        builder = builder.withCompletion(Integer.parseInt(completion));
      }
    }
    if (record.isSet(TaskDefaultColumn.COST.getName())) {
      try {
        builder = builder.withCost(new BigDecimal(record.get(TaskDefaultColumn.COST.getName())));
      } catch (NumberFormatException e) {
        GPLogger.logToLogger(e);
        GPLogger.log(String.format("Failed to parse %s as cost value", record.get(TaskDefaultColumn.COST.getName())));
      }
    }
    Task task = builder.build();

    if (record.isSet(TaskDefaultColumn.ID.getName())) {
      myTaskIdMap.put(record.get(TaskDefaultColumn.ID.getName()), task);
    }
    myAssignmentMap.put(task, getOrNull(record, TaskFields.RESOURCES.toString()));
    myPredecessorMap.put(task, getOrNull(record, TaskDefaultColumn.PREDECESSORS.getName()));
    String outlineNumber = getOrNull(record, TaskDefaultColumn.OUTLINE_NUMBER.getName());
    if (outlineNumber != null) {
      myWbsMap.put(outlineNumber, task);
    }
    for (String customField : getCustomFields()) {
      String value = getOrNull(record, customField);
      if (value == null) {
        continue;
      }
      CustomPropertyDefinition def = taskManager.getCustomPropertyManager().getCustomPropertyDefinition(customField);
      if (def == null) {
        GPLogger.logToLogger("Can't find custom field with name=" + customField + " value=" + value);
        continue;
      }
      task.getCustomValues().addCustomProperty(def, value);
    }
    return true;
  }

  @Override
  protected void postProcess() {
    for (Map.Entry<String, Task> wbsEntry : myWbsMap.entrySet()) {
      String outlineNumber = wbsEntry.getKey();
      List<String> components = Arrays.asList(outlineNumber.split("\\."));
      if (components.size() <= 1) {
        continue;
      }
      String parentOutlineNumber = Joiner.on('.').join(components.subList(0,  components.size() - 1));
      Task parentTask = myWbsMap.get(parentOutlineNumber);
      if (parentTask == null) {
        continue;
      }
      taskManager.getTaskHierarchy().move(wbsEntry.getValue(), parentTask, 0);
    }
    if (resourceManager != null) {
      Map<String, HumanResource> resourceMap = Maps.uniqueIndex(resourceManager.getResources(), new Function<HumanResource, String>() {
        @Override
        public String apply(HumanResource input) {
          return input.getName();
        }
      });
      for (Entry<Task, String> assignment : myAssignmentMap.entrySet()) {
        if (assignment.getValue() == null) {
          continue;
        }
        String[] names = assignment.getValue().split(";");
        for (String name : names) {
          HumanResource resource = resourceMap.get(name);
          if (resource != null) {
            assignment.getKey().getAssignmentCollection().addAssignment(resource);
          }
        }
      }
    }
    for (Entry<Task, String> predecessor : myPredecessorMap.entrySet()) {
      if (predecessor.getValue() == null) {
        continue;
      }
      String[] ids = predecessor.getValue().split(";");
      for (String id : ids) {
        Task dependee = myTaskIdMap.get(id);
        if (dependee != null) {
          try {
            taskManager.getDependencyCollection().createDependency(predecessor.getKey(), dependee);
          } catch (TaskDependencyException e) {
            GPLogger.logToLogger(e);
          }
        }
      }
    }
  }
}

