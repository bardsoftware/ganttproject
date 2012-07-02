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
import java.util.Collection;
import java.util.List;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.Lists;

/**
 * Handles opening CSV files.
 *
 * A mapping is used to find the correct CSV field that belong to a known Task
 * attribute
 */
public class GanttCSVOpen {
  /** List of known (and supported) Task attributes */
  public enum TaskFields {
    NAME("tableColName"), BEGIN_DATE("tableColBegDate"), END_DATE("tableColEndDate"), WEB_LINK("webLink"), NOTES(
        "notes");

    private static List<String> ourNames;
    private final String text;

    private TaskFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return language.getText(text);
    }

    public static Collection<String> getAllFields() {
      if (ourNames == null) {
        ourNames = Lists.newArrayList();
        for (TaskFields tf : values()) {
          ourNames.add(tf.toString());
        }
      }
      return ourNames;
    }
  }

  private final TaskManager myTaskManager;

  /** The CSV file that is going to be imported */
  private final File myFile;

  private static final GanttLanguage language = GanttLanguage.getInstance();

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
    CSVParser parser = new CSVParser(new InputStreamReader(new FileInputStream(myFile)),
        CSVFormat.DEFAULT.withHeader().withEmptyLinesIgnored(false).withSurroundingSpacesIgnored(true));
    List<CSVRecord> records = parser.getRecords();
    for (CSVRecord record : records) {
      if (record.size() > 0) {
      // Create the task
        TaskManager.TaskBuilder builder = myTaskManager.newTaskBuilder()
            .withName(record.get(TaskFields.NAME.toString()))
            .withStartDate(language.parseDate(record.get(TaskFields.BEGIN_DATE.toString())))
            .withEndDate(language.parseDate(record.get(TaskFields.END_DATE.toString())))
            .withWebLink(record.get(TaskFields.WEB_LINK.toString()))
            .withNotes(record.get(TaskFields.NOTES.toString()));
        Task task = builder.build();
      }
    }
    // Succeeded
    return true;
  }
}
