/*
Copyright 2012 GanttProject Team

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVParser;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Handles opening CSV files.
 * 
 * A mapping is used to find the correct CSV field that belong to a known Task
 * attribute
 */
public class GanttCSVOpen {
  /** List of known (and supported) Task attributes */
  public enum TaskFields {
    NAME, BEGIN_DATE, END_DATE, WEB_LINK, NOTES
  }

  private final TaskManager myTaskManager;

  private final File myFile;

  private static final GanttLanguage language = GanttLanguage.getInstance();

  /** Separator character used to parse the CSV file */
  private char separator = 0;

  /**
   * Map containing a relation between the known task attributes and the fields
   * in the CSV file
   */
  private Map<TaskFields, Integer> fieldsMap = null;

  public GanttCSVOpen(File file, TaskManager taskManager) {
    myFile = file;
    myTaskManager = taskManager;
  }

  /**
   * Create tasks from file.
   * 
   * @throws IOException
   *           on parse error or input read-failure
   */
  public boolean load() throws IOException {
    boolean hasHeader = getHeader();
    CSVParser parser = new CSVParser(new InputStreamReader(new FileInputStream(myFile)));

    if (hasHeader) {
      // Ignore header
      parser.getLine();
    }

    String[] line;
    while ((line = parser.getLine()) != null) {
      // Create the task
      GanttTask task = myTaskManager.createTask();
      // and fill in the mapped fields
      for (Entry<TaskFields, Integer> entry : getFieldsMap().entrySet()) {
        Integer fieldIndex = entry.getValue();
        switch (entry.getKey()) {
        case NAME:
          task.setName(line[fieldIndex]);
          break;
        case BEGIN_DATE:
          task.setStart(new GanttCalendar(language.parseDate(line[fieldIndex])));
          break;
        case END_DATE:
          task.setEnd(new GanttCalendar(language.parseDate(line[fieldIndex])));
          break;
        case WEB_LINK:
          task.setWebLink(line[fieldIndex]);
          break;
        case NOTES:
          task.setNotes(line[fieldIndex]);
          break;
        default:
          // Should not happen, although it is not too serious...
          GPLogger.log("Found unknown task field: " + entry.getKey());
        }
      }
      myTaskManager.registerTask(task);
      myTaskManager.getTaskHierarchy().move(task, myTaskManager.getRootTask());
    }

    // Succeeded
    return true;
  }

  /**
   * Try to find a mapping between the fields in the CSV file and the
   * known/supported task attributes
   * 
   * @return true when the CSV file has an (assumed) header
   * @throws IOException
   *           when something went wrong while reading from the BufferedReader
   */
  public boolean getHeader() throws IOException {
    CSVParser parser = new CSVParser(new InputStreamReader(new FileInputStream(myFile)));

    String[] fields = parser.getLine();
    if (getFieldsMap().size() > 0) {
      fieldsMap.clear();
    }

    // Determine/guess the required mapping
    for (TaskFields knownField : TaskFields.values()) {
      String fieldName = knownField.name().toLowerCase().replace('_', ' ');
      for (int i = 0; i < fields.length; i++) {
        String testFieldName = fields[i].toLowerCase();
        if (testFieldName.equals(fieldName)) {
          // Found a match to a known field!
          fieldsMap.put(knownField, i);
          // No need to check other fields
          break;
        }
      }
    }

    // We assume the file has a header when there is at least one match with
    // known fieldnames
    return fieldsMap.size() > 0;
  }

  /**
   * @returns the separator char that is (going to be) used, or 0 if it is not
   *          set yet
   */
  public char getSeparetor() {
    return separator;
  }

  /**
   * @return the mapping of the CSV fields. (So it could be used to manually
   *         override the found mapping)
   */
  public Map<TaskFields, Integer> getFieldsMap() {
    if (fieldsMap == null) {
      fieldsMap = new HashMap<TaskFields, Integer>();
    }
    return fieldsMap;
  }
}