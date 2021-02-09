/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2004-2012 Thomas Alexandre, GanttProject Team

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
package biz.ganttproject.impex.csv;

import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.util.ColorConvertion;
import net.sourceforge.ganttproject.util.StringUtils;
import org.apache.commons.csv.CSVFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Class to export the project in CSV text format
 *
 * @author athomas
 */
public class GanttCSVExport {
  private static final Predicate<ResourceAssignment> COORDINATOR_PREDICATE = arg -> arg.isCoordinator();


  private CSVOptions myCsvOptions;
  private final TaskManager myTaskManager;
  private final CustomPropertyManager myTaskCustomPropertyManager;
  private final HumanResourceManager myHumanResourceManager;
  private final CustomPropertyManager myHumanResourceCustomPropertyManager;
  private final RoleManager myRoleManager;

  public GanttCSVExport(IGanttProject project, CSVOptions csvOptions) {
    this(project.getTaskManager(), project.getHumanResourceManager(), project.getRoleManager(), csvOptions);
  }

  GanttCSVExport(TaskManager taskManager, HumanResourceManager resourceManager, RoleManager roleManager, CSVOptions csvOptions) {
    myTaskManager = Preconditions.checkNotNull(taskManager);
    myTaskCustomPropertyManager = Preconditions.checkNotNull(taskManager.getCustomPropertyManager());
    myHumanResourceManager = Preconditions.checkNotNull(resourceManager);
    myHumanResourceCustomPropertyManager = Preconditions.checkNotNull(resourceManager.getCustomPropertyManager());
    myRoleManager = Preconditions.checkNotNull(roleManager);
    myCsvOptions = Preconditions.checkNotNull(csvOptions);
  }

  private CSVFormat getCSVFormat() {
    CSVFormat format = CSVFormat.DEFAULT.withEscape('\\');
    if (myCsvOptions.sSeparatedChar.length() == 1) {
      format = format.withDelimiter(myCsvOptions.sSeparatedChar.charAt(0));
    }
    if (myCsvOptions.sSeparatedTextChar.length() == 1) {
      format = format.withQuote(myCsvOptions.sSeparatedTextChar.charAt(0));
    }

    return format;
  }

  public SpreadsheetWriter createWriter(OutputStream stream, SpreadsheetFormat format) throws IOException {
    format = Preconditions.checkNotNull(format);

    switch (format) {
      case CSV:
        return getCsvWriter(stream);
      case XLS:
        return getXlsWriter(stream);
      default:
        throw new IllegalArgumentException("Unsupported format == " + format + "!");
    }
  }


  private SpreadsheetWriter getCsvWriter(OutputStream stream) throws IOException {
    return new CsvWriterImpl(stream, getCSVFormat(), myCsvOptions.getBomOption().getValue());
  }

  private SpreadsheetWriter getXlsWriter(OutputStream stream) {
    return new XlsWriterImpl(stream);
  }

  public void save(SpreadsheetWriter writer) throws IOException {
    writeTasks(writer);

    if (myHumanResourceManager.getResources().size() > 0) {
      writer.println();
      writer.println();
      writeResources(writer);
    }
  }

  private List<CustomPropertyDefinition> writeTaskHeaders(SpreadsheetWriter writer) throws IOException {
    List<CustomPropertyDefinition> defs = myTaskCustomPropertyManager.getDefinitions();
    for (Map.Entry<String, BooleanOption> entry : myCsvOptions.getTaskOptions().entrySet()) {
      TaskDefaultColumn defaultColumn = TaskDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
        continue;
      }
      if (defaultColumn == null) {
        writer.print(i18n(entry.getKey()));
      } else {
        writer.print(defaultColumn.getName());
        if (defaultColumn == TaskDefaultColumn.RESOURCES) {
          writer.print(TaskRecords.TaskFields.ASSIGNMENTS.toString());
        }
      }
    }
    for (CustomPropertyDefinition def : defs) {
      writer.print(def.getName());
    }
    writer.println();
    return defs;
  }

  private String i18n(String key) {
    return InternationalizationKt.getRootLocalizer().formatText(key);
  }

  private void writeTasks(SpreadsheetWriter writer) throws IOException {
    List<CustomPropertyDefinition> customFields = writeTaskHeaders(writer);
    for (Task task : myTaskManager.getTasks()) {
      for (Map.Entry<String, BooleanOption> entry : myCsvOptions.getTaskOptions().entrySet()) {
        if (!entry.getValue().isChecked()) {
          continue;
        }
        TaskDefaultColumn defaultColumn = TaskDefaultColumn.find(entry.getKey());
        if (defaultColumn == null) {
          if ("webLink".equals(entry.getKey())) {
            writer.print(getWebLink((GanttTask) task));
            continue;
          }
          if ("notes".equals(entry.getKey())) {
            writer.print(task.getNotes());
            continue;
          }
        } else {
          switch (defaultColumn) {
            case ID:
              writer.print(task.getTaskID());
              break;
            case NAME:
              writer.print(getName(task));
              break;
            case BEGIN_DATE:
              writer.print(task.getStart());
              break;
            case END_DATE:
              writer.print(task.getDisplayEnd());
              break;
            case DURATION:
              writer.print(task.getDuration().getLength());
              break;
            case COMPLETION:
              writer.print(task.getCompletionPercentage());
              break;
            case OUTLINE_NUMBER:
              List<Integer> outlinePath = task.getManager().getTaskHierarchy().getOutlinePath(task);
              writer.print(Joiner.on('.').join(outlinePath));
              break;
            case COORDINATOR:
              ResourceAssignment coordinator = Iterables.tryFind(Arrays.asList(task.getAssignments()), COORDINATOR_PREDICATE).orNull();
              writer.print(coordinator == null ? "" : coordinator.getResource().getName());
              break;
            case PREDECESSORS:
              writer.print(TaskProperties.formatPredecessors(task, ";", true));
              break;
            case RESOURCES:
              writer.print(getAssignments(task));
              writer.print(buildAssignmentSpec(task));
              break;
            case COST:
              writer.print(task.getCost().getValue());
              break;
            case COLOR:
              if (!Objects.equal(task.getColor(), task.getManager().getTaskDefaultColorOption().getValue())) {
                writer.print(ColorConvertion.getColor(task.getColor()));
              } else {
                writer.print("");
              }
              break;
            case INFO:
            case PRIORITY:
            case TYPE:
              break;
          }
        }
      }
      CSVExportKt.writeCustomPropertyValues(writer, customFields, task.getCustomValues().getCustomProperties());
    }
  }

  private List<CustomPropertyDefinition> writeResourceHeaders(SpreadsheetWriter writer) throws IOException {
    for (Map.Entry<String, BooleanOption> entry : myCsvOptions.getResourceOptions().entrySet()) {
      ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
        continue;
      }
      if (defaultColumn == ResourceDefaultColumn.ROLE_IN_TASK) {
        // There's not too much sense in exporting role in task not in the assignment context.
        continue;
      }
      if (defaultColumn == null) {
        if ("id".equals(entry.getKey())) {
          writer.print(i18n("tableColID"));
        } else {
          writer.print(i18n(entry.getKey()));
        }
      } else {
        writer.print(defaultColumn.getName());
      }
    }
    List<CustomPropertyDefinition> customFieldDefs = myHumanResourceCustomPropertyManager.getDefinitions();
    for (CustomPropertyDefinition nextDef : customFieldDefs) {
      writer.print(nextDef.getName());
    }
    writer.println();
    return customFieldDefs;
  }

  private void writeResources(SpreadsheetWriter writer) throws IOException {
    Set<Role> projectRoles = Sets.newHashSet(myRoleManager.getProjectLevelRoles());
    List<CustomPropertyDefinition> customPropDefs = writeResourceHeaders(writer);
    // parse all resources
    for (HumanResource p : myHumanResourceManager.getResources()) {
      for (Map.Entry<String, BooleanOption> entry : myCsvOptions.getResourceOptions().entrySet()) {
        if (!entry.getValue().isChecked()) {
          continue;
        }
        ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
        if (defaultColumn == null) {
          if ("id".equals(entry.getKey())) {
            writer.print(p.getId());
            continue;
          }
        } else {
          switch (defaultColumn) {
            case NAME:
              writer.print(p.getName());
              break;
            case EMAIL:
              writer.print(p.getMail());
              break;
            case PHONE:
              writer.print(p.getPhone());
              break;
            case ROLE:
              Role role = p.getRole();
              final String sRoleID;
              if (role == null) {
                sRoleID = "0";
              } else if (projectRoles.contains(role)) {
                sRoleID = role.getName();
              } else {
                sRoleID = role.getPersistentID();
              }
              writer.print(sRoleID);
              break;
            case ROLE_IN_TASK:
              // There's not too much sense in exporting role in task not in the assignment context.
              break;
            case STANDARD_RATE:
              writer.print(p.getStandardPayRate());
              break;
            case TOTAL_COST:
              writer.print(p.getTotalCost());
              break;
            case TOTAL_LOAD:
              writer.print(p.getTotalLoad());
              break;
          }
        }
      }
      CSVExportKt.writeCustomPropertyValues(writer, customPropDefs, p.getCustomProperties());
    }
  }


  /**
   * @return the name of task with the correct level.
   */
  private String getName(Task task) {
    if (myCsvOptions.bFixedSize) {
      return task.getName();
    }
    int depth = task.getManager().getTaskHierarchy().getDepth(task) - 1;
    return StringUtils.padLeft(task.getName(), depth * 2);
  }

  /**
   * @return the link of the task.
   */
  private String getWebLink(GanttTask task) {
    return (task.getWebLink() == null || task.getWebLink().equals("http://") ? "" : task.getWebLink());
  }

  /**
   * @return the list of the assignment for the resources.
   */
  private String getAssignments(Task task) {
    StringBuilder res = new StringBuilder();
    ResourceAssignment[] assignment = task.getAssignments();
    for (int i = 0; i < assignment.length; i++) {
      String assignmentDelimiter = i == assignment.length - 1
              ? ""
              : myCsvOptions.sSeparatedChar.equals(";") ? "," : ";";
      res.append(assignment[i].getResource()).append(assignmentDelimiter);
    }
    return res.toString();
  }

  static String buildAssignmentSpec(Task task) {
    List<String> loads = Lists.newArrayList();
    for (ResourceAssignment ra : task.getAssignments()) {
      loads.add(String.format(Locale.ROOT, "%d:%.2f", ra.getResource().getId(), ra.getLoad()));
    }
    return Joiner.on(';').join(loads);
  }


}
