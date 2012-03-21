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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Handles opening CSV files.
 *
 * Currently, the class assumes that the fields are in the same order as
 * GanttProject exports them:
 *
 * ID,Name,Begin date,End date,Duration,Completion,Web Link,Resources,Notes
 */
public class GanttCSVOpen {
  private final TaskManager myTaskManager;

  private final File myFile;

  private static final GanttLanguage language = GanttLanguage.getInstance();

  /** Separator character used to parse the CSV file */
  private char separator = 0;

  /** When true, a series to separator characters are processed as one */
  private boolean allowMultipleSeparators = false;

  public GanttCSVOpen(File file, TaskManager taskManager) {
    myFile = file;
    myTaskManager = taskManager;
  }

  /** Create tasks from file. */
  public boolean load() {
    try {
      // Open a stream
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(myFile)));

      if (!br.ready()) {
        // We cannot read the file?!
        return false;
      }

      // First read header (and ignore for now)
      getHeader(br);

      // TODO Check what header fields are present,
      // and use them for the correct mapping while reading the tasks

      // Read lines unit the stream is at its end
      while (br.ready()) {
        String line = br.readLine();

        // Skip white lines
        if (!"".equals(line)) {
          List<String> fields = splitLine(line);
          if (fields != null && fields.size() == 9) {
            // Create the task
            GanttTask task = myTaskManager.createTask();
            task.setName(fields.get(1));
            task.setStart(new GanttCalendar(language.parseDate(fields.get(2))));
            task.setEnd(new GanttCalendar(language.parseDate(fields.get(3))));
            task.setWebLink(fields.get(6));
            task.setNotes(fields.get(8));
            myTaskManager.registerTask(task);
            myTaskManager.getTaskHierarchy().move(task, myTaskManager.getRootTask());
          } else {
            GPLogger.log("Could not parse '" + line + "', skipped!");
          }
        }
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * <p>
   * Reads the header and returns the fields. It also:
   * <ul>
   * <li>tries to find the correct separator char if it is not yet set</li>
   * </ul>
   * </p>
   * <p>
   * <em>Note:</em> This method assumes that the BufferedReader is still at the
   * begin of the file!
   * </p>
   *
   * @param br
   *          BufferedReader to read the header from
   * @return an array of strings with the header fields
   * @throws IOException
   *           when something went wrong while reading from the BufferedReader
   */
  public List<String> getHeader(BufferedReader br) throws IOException {
    final String header = br.readLine();
    if (separator == 0) {
      // Try to determine separator char used in this CSV file
      // This simple method check which known separator char is encountered
      // first...
      int firstComma = findFirst(header, ',');
      int firstSpace = findFirst(header, ' ');
      int firstTab = findFirst(header, '\t');
      if (firstSpace < firstComma && firstSpace < firstTab) {
        setSeparator(' ');
      } else if (firstTab < firstComma && firstTab < firstSpace) {
        setSeparator('\t');
      } else {
        setSeparator(',');
      }
    }
    // TODO try to find a mapping between known Task fields and the fields in the header
    return splitLine(header);
  }

  /**
   * @returns the separator char that is (going to be) used, or 0 if it is not
   *          set yet
   */
  public char getSeparetor() {
    return separator;
  }

  /** Sets (override) the field separator to use */
  public void setSeparator(char separator) {
    this.separator = separator;
    allowMultipleSeparators = separator == ' ' || separator == '\t';
  }

  private List<String> splitLine(String line) {
    boolean sQuoteOpen = false;
    boolean dQuoteOpen = false;
    boolean previousSlash = false;
    boolean previousSeparator = false;

    final List<String> result = new ArrayList<String>();
    StringBuilder field = new StringBuilder();
    char[] bytes = line.toCharArray();

    for (char b : bytes) {
      if (previousSlash) {
        // Always add this char
        field.append(b);
        previousSlash = false;
      } else if (b == '"' && !sQuoteOpen) {
        // Encountered an opening/closing double quote
        dQuoteOpen = !dQuoteOpen;
        continue;
      }
      if (b == '\'' && !dQuoteOpen) {
        // Encountered an opening/closing single quote
        sQuoteOpen = !sQuoteOpen;
        continue;
      }
      if (b == '\\') {
        // Skip slash, but always add next char
        previousSlash = true;
        continue;
      }
      if (b == separator && !dQuoteOpen && !sQuoteOpen && (!allowMultipleSeparators || !previousSeparator)) {
        // End of field!
        result.add(field.toString().trim());
        field = new StringBuilder();
        previousSeparator = true;
        continue;
      }

      // Add byte to field string
      field.append(b);
      previousSeparator = false;
    }
    return result;
  }

  /** @return the first index of c in str, or str length if not found */
  private int findFirst(String str, char c) {
    int index = str.indexOf(c);
    if (index == -1) {
      return str.length();
    }
    return index;
  }
}