/*
Copyright 2003-2012 GanttProject Team

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
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.ResourceDefaultColumn;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CSVOptions {
  private static final Set<TaskDefaultColumn> ourIgnoredTaskColumns = ImmutableSet.of(
      TaskDefaultColumn.TYPE, TaskDefaultColumn.PRIORITY, TaskDefaultColumn.INFO);
  private final Map<String, BooleanOption> myTaskOptions = Maps.newLinkedHashMap();
  private final Map<String, BooleanOption> myResourceOptions = Maps.newLinkedHashMap();
  private final BooleanOption myBomOption = new DefaultBooleanOption("write-bom", false);

  public CSVOptions() {
    List<TaskDefaultColumn> orderedColumns = ImmutableList.of(
        TaskDefaultColumn.ID, TaskDefaultColumn.NAME, TaskDefaultColumn.BEGIN_DATE, TaskDefaultColumn.END_DATE,
        TaskDefaultColumn.DURATION, TaskDefaultColumn.COMPLETION, TaskDefaultColumn.COST);
    LinkedHashSet<TaskDefaultColumn> columns = Sets.newLinkedHashSet(Arrays.asList(TaskDefaultColumn.values()));
    columns.removeAll(orderedColumns);
    for (TaskDefaultColumn taskColumn : orderedColumns) {
      createTaskExportOption(taskColumn);
    }
    for (TaskDefaultColumn taskColumn : columns) {
      if (!ourIgnoredTaskColumns.contains(taskColumn)) {
        createTaskExportOption(taskColumn);
      }
    }
    createTaskExportOption("webLink");
    createTaskExportOption("notes");

    myResourceOptions.put("id", new DefaultBooleanOption("id", true));
    for (ResourceDefaultColumn resourceColumn : ResourceDefaultColumn.values()) {
      createResourceExportOption(resourceColumn);
    }
  }

  private BooleanOption createResourceExportOption(ResourceDefaultColumn resourceColumn) {
    DefaultBooleanOption result = new DefaultBooleanOption(resourceColumn.getStub().getID(), true);
    myResourceOptions.put(resourceColumn.getStub().getID(), result);
    return result;
  }

  public BooleanOption createTaskExportOption(TaskDefaultColumn taskColumn) {
    DefaultBooleanOption result = new DefaultBooleanOption(taskColumn.getStub().getID(), true);
    myTaskOptions.put(taskColumn.getStub().getID(), result);
    return result;
  }

  public BooleanOption createTaskExportOption(String id) {
    DefaultBooleanOption result = new DefaultBooleanOption(id, true);
    myTaskOptions.put(id, result);
    return result;
  }

  public Map<String, BooleanOption> getTaskOptions() {
    return myTaskOptions;
  }

  public Map<String, BooleanOption> getResourceOptions() {
    return myResourceOptions;
  }

  public boolean bFixedSize = false;

  public String sSeparatedChar = ",";

  public String sSeparatedTextChar = "\"";

  /**
   * @return a list of the possible separated char.
   */
  public String[] getSeparatedTextChars() {
    String[] charText = {"   \'   ", "   \"   "};
    return charText;
  }

  public BooleanOption getBomOption() {
    return myBomOption;
  }
}
