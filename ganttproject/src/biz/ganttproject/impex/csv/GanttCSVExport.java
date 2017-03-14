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

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.util.StringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class to export the project in CSV text format
 *
 * @author athomas
 */
public class GanttCSVExport {
  private static final Predicate<ResourceAssignment> COORDINATOR_PREDICATE = new Predicate<ResourceAssignment>() {
    public boolean apply(ResourceAssignment arg) {
      return arg.isCoordinator();
    }
  };

  private final CSVOptions csvOptions;
  private final TaskManager myTaskManager;
  private final CustomPropertyManager myTaskCustomPropertyManager;
  private final HumanResourceManager myHumanResourceManager;
  private final CustomPropertyManager myHumanResourceCustomPropertyManager;

  private int iMaxSize = 0;

  public GanttCSVExport(IGanttProject project, CSVOptions csvOptions) {
    this(project.getTaskManager(),
        project.getHumanResourceManager(), csvOptions);
  }

  GanttCSVExport(TaskManager taskManager, HumanResourceManager resourceManager, CSVOptions csvOptions) {
    myTaskManager = Preconditions.checkNotNull(taskManager);
    myTaskCustomPropertyManager = Preconditions.checkNotNull(taskManager.getCustomPropertyManager());
    myHumanResourceManager = Preconditions.checkNotNull(resourceManager);
    myHumanResourceCustomPropertyManager = Preconditions.checkNotNull(resourceManager.getCustomPropertyManager());
    this.csvOptions = Preconditions.checkNotNull(csvOptions);
  }
  /**
   * Save the project as CSV on a stream
   *
   * @throws IOException
   */
  public void save(OutputStream stream) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);
    CSVFormat format = CSVFormat.DEFAULT.withEscape('\\');
    if (csvOptions.sSeparatedChar.length() == 1) {
      format = format.withDelimiter(csvOptions.sSeparatedChar.charAt(0));
    }
    if (csvOptions.sSeparatedTextChar.length() == 1) {
      format = format.withQuote(csvOptions.sSeparatedTextChar.charAt(0));
    }

    CSVPrinter csvPrinter = new CSVPrinter(writer, format);

//    if (csvOptions.bFixedSize) {
//      // TODO The CVS library we use is lacking support for fixed size
//      getMaxSize();
//    }

    writeTasks(csvPrinter);

    if (myHumanResourceManager.getResources().size() > 0) {
      csvPrinter.println();
      csvPrinter.println();
      writeResources(csvPrinter);
    }
    writer.flush();
    writer.close();
  }

  private List<CustomPropertyDefinition> writeTaskHeaders(CSVPrinter writer) throws IOException {
    List<CustomPropertyDefinition> defs = myTaskCustomPropertyManager.getDefinitions();
    for (Map.Entry<String, BooleanOption> entry : csvOptions.getTaskOptions().entrySet()) {
      TaskDefaultColumn defaultColumn = TaskDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
        continue;
      }
      if (defaultColumn == null) {
        writer.print(i18n(entry.getKey()));
      } else {
        writer.print(defaultColumn.getName());
      }
    }
    for (CustomPropertyDefinition def : defs) {
      writer.print(def.getName());
    }
    writer.println();
    return defs;
  }

  private String i18n(String key) {
    return GanttLanguage.getInstance().getText(key);
  }

  /** Write all tasks.
   * @throws IOException */
  private void writeTasks(CSVPrinter writer) throws IOException {
    List<CustomPropertyDefinition> customFields = writeTaskHeaders(writer);
    for (Task task : myTaskManager.getTasks()) {
      for (Map.Entry<String, BooleanOption> entry : csvOptions.getTaskOptions().entrySet()) {
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
            writer.print(String.valueOf(task.getTaskID()));
            break;
          case NAME:
            writer.print(getName(task));
            break;
          case BEGIN_DATE:
            writer.print(task.getStart().toString());
            break;
          case END_DATE:
            writer.print(task.getDisplayEnd().toString());
            break;
          case DURATION:
            writer.print(String.valueOf(task.getDuration().getLength()));
            break;
          case COMPLETION:
            writer.print(String.valueOf(task.getCompletionPercentage()));
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
            break;
          case COST:
            writer.print(task.getCost().getValue().toPlainString());
            break;
          case INFO:
          case PRIORITY:
          case TYPE:
            break;
          }
        }
      }
      writeCustomPropertyValues(writer, customFields, task.getCustomValues().getCustomProperties());
    }
  }

  private List<CustomPropertyDefinition> writeResourceHeaders(CSVPrinter writer) throws IOException {
    for (Map.Entry<String, BooleanOption> entry : csvOptions.getResourceOptions().entrySet()) {
      ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
      if (!entry.getValue().isChecked()) {
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
    for (int i = 0; i < customFieldDefs.size(); i++) {
      CustomPropertyDefinition nextDef = customFieldDefs.get(i);
      writer.print(nextDef.getName());
    }
    writer.println();
    return customFieldDefs;
  }

  /** write the resources.
   * @throws IOException */
  private void writeResources(CSVPrinter writer) throws IOException {
    List<CustomPropertyDefinition> customPropDefs = writeResourceHeaders(writer);
    // parse all resources
    for (HumanResource p : myHumanResourceManager.getResources()) {
      for (Map.Entry<String, BooleanOption> entry : csvOptions.getResourceOptions().entrySet()) {
        if (!entry.getValue().isChecked()) {
          continue;
        }
        ResourceDefaultColumn defaultColumn = ResourceDefaultColumn.find(entry.getKey());
        if (defaultColumn == null) {
          if ("id".equals(entry.getKey())) {
            writer.print(String.valueOf(p.getId()));
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
            String sRoleID = role == null ? "0" : role.getPersistentID();
            writer.print(sRoleID);
            break;
          case ROLE_IN_TASK:
            writer.print("");
            break;
          case STANDARD_RATE:
            writer.print(p.getStandardPayRate().toPlainString());
            break;
          }
        }
      }
      writeCustomPropertyValues(writer, customPropDefs, p.getCustomProperties());
    }
  }

  private void writeCustomPropertyValues(CSVPrinter writer,
                                         List<CustomPropertyDefinition> defs, List<CustomProperty> values) throws IOException {
    Map<String, CustomProperty> definedProps = Maps.newHashMap();
    for (CustomProperty prop : values) {
      definedProps.put(prop.getDefinition().getID(), prop);
    }
    for (CustomPropertyDefinition def : defs) {
      CustomProperty value = definedProps.get(def.getID());
      String valueAsString = value == null ? null : Strings.nullToEmpty(value.getValueAsString());
      writer.print(valueAsString);
    }
    writer.println();

  }
  /** @return the name of task with the correct level. */
  private String getName(Task task) {
    if (csvOptions.bFixedSize) {
      return task.getName();
    }
    int depth = task.getManager().getTaskHierarchy().getDepth(task) - 1;
    return StringUtils.padLeft(task.getName(), depth * 2);
  }

  /** @return the link of the task. */
  private String getWebLink(GanttTask task) {
    return (task.getWebLink() == null || task.getWebLink().equals("http://") ? "" : task.getWebLink());
  }

  /** @return the list of the assignment for the resources. */
  private String getAssignments(Task task) {
    String res = "";
    ResourceAssignment[] assignment = task.getAssignments();
    for (int i = 0; i < assignment.length; i++) {
      res += (assignment[i].getResource() + (i == assignment.length - 1 ? ""
          : csvOptions.sSeparatedChar.equals(";") ? "," : ";"));
    }
    return res;
  }
}
