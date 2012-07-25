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
package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.TaskDefaultColumn;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.util.StringUtils;

/**
 * Class to export the project in CSV text format
 *
 * @author athomas
 */
public class GanttCSVExport {
  private final IGanttProject myProject;

  private final CSVOptions csvOptions;

  private int iMaxSize = 0;

  public GanttCSVExport(IGanttProject project, CSVOptions csvOptions) {
    myProject = project;
    this.csvOptions = csvOptions;
  }

  /**
   * Save the project as CSV on a stream
   *
   * @throws IOException
   */
  public void save(OutputStream stream) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(stream);
    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

    if (csvOptions.bFixedSize) {
      // TODO The CVS library we use is lacking support for fixed size
      getMaxSize();
    }

    writeTasks(csvPrinter);

    if (myProject.getHumanResourceManager().getResources().size() > 0) {
      csvPrinter.println();
      csvPrinter.println();
      writeResources(csvPrinter);
    }
    writer.flush();
    writer.close();
  }

  private void writeTaskHeaders(CSVPrinter writer) throws IOException {
    if (csvOptions.bExportTaskID) {
      writer.print(TaskDefaultColumn.ID.getName());
    }
    if (csvOptions.bExportTaskName) {
      writer.print(TaskDefaultColumn.NAME.getName());
    }
    if (csvOptions.bExportTaskStartDate) {
      writer.print(TaskDefaultColumn.BEGIN_DATE.getName());
    }
    if (csvOptions.bExportTaskEndDate) {
      writer.print(TaskDefaultColumn.END_DATE.getName());
    }
    if (csvOptions.bExportTaskDuration) {
      writer.print(TaskDefaultColumn.DURATION.getName());
    }
    if (csvOptions.bExportTaskPercent) {
      writer.print(TaskDefaultColumn.COMPLETION.getName());
    }
    if (csvOptions.bExportTaskWebLink) {
      writer.print(i18n("webLink"));
    }
    if (csvOptions.bExportTaskResources) {
      writer.print(i18n("resources"));
    }
    if (csvOptions.bExportTaskNotes) {
      writer.print(i18n("notes"));
    }
    writer.print(TaskDefaultColumn.PREDECESSORS.getName());
    for (CustomPropertyDefinition def : myProject.getTaskCustomColumnManager().getDefinitions()) {
      writer.print(def.getName());
    }
    writer.println();
    writer.println();
  }

  private String i18n(String key) {
    return GanttLanguage.getInstance().getText(key);
  }

  /** Write all tasks.
   * @throws IOException */
  private void writeTasks(CSVPrinter writer) throws IOException {
    writeTaskHeaders(writer);
    List<CustomPropertyDefinition> customFields = myProject.getTaskCustomColumnManager().getDefinitions();
    for (Task task : myProject.getTaskManager().getTasks()) {
      // ID
      if (csvOptions.bExportTaskID) {
        writer.print(String.valueOf(task.getTaskID()));
      }
      // Name
      if (csvOptions.bExportTaskName) {
        writer.print(getName(task));
      }
      // Start Date
      if (csvOptions.bExportTaskStartDate) {
        writer.print(task.getStart().toString());
      }
      // End Date
      if (csvOptions.bExportTaskEndDate) {
        writer.print(task.getEnd().toString());
      }
      // Duration
      if (csvOptions.bExportTaskDuration) {
        writer.print(String.valueOf(task.getDuration().getLength()));
      }
      // Percent complete
      if (csvOptions.bExportTaskPercent) {
        writer.print(String.valueOf(task.getCompletionPercentage()));
      }
      // Web Link
      if (csvOptions.bExportTaskWebLink) {
        writer.print(getWebLink((GanttTask) task));
      }
      // associated resources
      if (csvOptions.bExportTaskResources) {
        writer.print(getAssignments(task));
      }
      // Notes
      if (csvOptions.bExportTaskNotes) {
        writer.print(task.getNotes());
      }
      writer.print(Joiner.on(';').join(Lists.transform(
          Arrays.asList(task.getDependenciesAsDependant().toArray()),
          new Function<TaskDependency, String>() {
            @Override
            public String apply(TaskDependency input) {
              return "" + input.getDependee().getTaskID();
            }
          })));
      CustomColumnsValues customValues = task.getCustomValues();
      for (int j = 0; j < customFields.size(); j++) {
        Object nextCustomFieldValue = customValues.getValue(customFields.get(j));
        writer.print(String.valueOf(nextCustomFieldValue));
      }
      writer.println();
    }
  }

  private void writeResourceHeaders(CSVPrinter writer) throws IOException {
    if (csvOptions.bExportResourceID) {
      writer.print(i18n("tableColID"));
    }
    if (csvOptions.bExportResourceName) {
      writer.print(i18n("tableColResourceName"));
    }
    if (csvOptions.bExportResourceMail) {
      writer.print(i18n("tableColResourceEMail"));
    }
    if (csvOptions.bExportResourcePhone) {
      writer.print(i18n("tableColResourcePhone"));
    }
    if (csvOptions.bExportResourceRole) {
      writer.print(i18n("tableColResourceRole"));
    }
    List<CustomPropertyDefinition> customFieldDefs = myProject.getResourceCustomPropertyManager().getDefinitions();
    for (int i = 0; i < customFieldDefs.size(); i++) {
      CustomPropertyDefinition nextDef = customFieldDefs.get(i);
      writer.print(nextDef.getName());
    }
    writer.println();
    writer.println();
  }

  /** write the resources.
   * @throws IOException */
  private void writeResources(CSVPrinter writer) throws IOException {
    writeResourceHeaders(writer);
    // parse all resources
    for (HumanResource p : myProject.getHumanResourceManager().getResources()) {
      // ID
      if (csvOptions.bExportResourceID) {
        writer.print(String.valueOf(p.getId()));
      }
      // Name
      if (csvOptions.bExportResourceName) {
        writer.print(p.getName());
      }
      // Mail
      if (csvOptions.bExportResourceMail) {
        writer.print(p.getMail());
      }
      // Phone
      if (csvOptions.bExportResourcePhone) {
        writer.print(p.getPhone());
      }
      // Role
      if (csvOptions.bExportResourceRole) {
        Role role = p.getRole();
        String sRoleID = role == null ? "0" : role.getPersistentID();
        writer.print(sRoleID);
      }
      List<CustomProperty> customProps = p.getCustomProperties();
      for (int j = 0; j < customProps.size(); j++) {
        CustomProperty nextProperty = customProps.get(j);
        writer.print(nextProperty.getValueAsString());
      }
      writer.println();
    }
  }

  /** set the maximum size for all strings. */
  private void getMaxSize() {
    List<CustomPropertyDefinition> customFields = myProject.getTaskCustomColumnManager().getDefinitions();
    iMaxSize = 0;
    // Check widths of the tasks fields
    for (Task task : myProject.getTaskManager().getTasks()) {

      if (csvOptions.bExportTaskID) {
        String s = String.valueOf(task.getTaskID());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskName) {
        String s = getName(task);
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskStartDate) {
        String s = String.valueOf(task.getStart());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskEndDate) {
        String s = String.valueOf(task.getEnd());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskDuration) {
        String s = String.valueOf(task.getDuration().getLength());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskPercent) {
        String s = String.valueOf(task.getCompletionPercentage());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskWebLink) {
        String s = getWebLink((GanttTask) task);
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskResources) {
        String s = getAssignments(task);
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      if (csvOptions.bExportTaskNotes) {
        String s = task.getNotes();
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }

      CustomColumnsValues customValues = task.getCustomValues();
      for (int j = 0; j < customFields.size(); j++) {
        Object nextCustomFieldValue = customValues.getValue(customFields.get(j));
        String nextValueAsString = String.valueOf(nextCustomFieldValue);
        if (nextValueAsString.length() > iMaxSize) {
          iMaxSize = nextValueAsString.length();
        }
      }
    }

    // Check widths of the resources fields
    for (HumanResource p : myProject.getHumanResourceManager().getResources()) {
      if (csvOptions.bExportResourceID) {
        String s = String.valueOf(p.getId());
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }
      if (csvOptions.bExportResourceName) {
        String s = p.getName();
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }
      if (csvOptions.bExportResourceMail) {
        String s = p.getMail();
        if (s.length() > iMaxSize)
          iMaxSize = s.length();
      }
      if (csvOptions.bExportResourcePhone) {
        String s = p.getPhone();
        if (s.length() > iMaxSize) {
          iMaxSize = s.length();
        }
      }
      if (csvOptions.bExportResourceRole) {
        Role role = p.getRole();
        String sRoleID;
        if (role != null) {
          sRoleID = role.getPersistentID();
        } else {
          sRoleID = "0";
        }
        if (sRoleID.length() > iMaxSize) {
          iMaxSize = sRoleID.length();
        }
      }
      List<CustomProperty> customProps = p.getCustomProperties();
      for (int j = 0; j < customProps.size(); j++) {
        CustomProperty nextProperty = customProps.get(j);
        if (nextProperty.getValueAsString().length() > iMaxSize) {
          iMaxSize = nextProperty.getValueAsString().length();
        }
      }
    }
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
