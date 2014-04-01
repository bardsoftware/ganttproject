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
package biz.ganttproject.impex.csv;

import static net.sourceforge.ganttproject.GPLogger.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.TaskManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Handles opening CSV files.
 */
public class GanttCSVOpen {
  static Collection<String> getFieldNames(Enum... fieldsEnum) {
    return Collections2.transform(Arrays.asList(fieldsEnum), new Function<Enum, String>() {
      @Override
      public String apply(Enum input) {
        return input.toString();
      }
    });
  }

  static final GanttLanguage language = GanttLanguage.getInstance();

  private final List<RecordGroup> myRecordGroups;

  private final Supplier<Reader> myInputSupplier;

  private int mySkippedLine;

  public GanttCSVOpen(Supplier<Reader> inputSupplier, RecordGroup group) {
    myInputSupplier = inputSupplier;
    myRecordGroups = ImmutableList.of(group);
  }

  public GanttCSVOpen(Supplier<Reader> inputSupplier, RecordGroup... groups) {
    myInputSupplier = inputSupplier;
    myRecordGroups = Lists.newArrayList();
    for (RecordGroup group : groups) {
      if (group != null) {
        myRecordGroups.add(group);
      }
    }
  }

  public GanttCSVOpen(Supplier<Reader> inputSupplier, final TaskManager taskManager, final HumanResourceManager resourceManager) {
    this(inputSupplier, createTaskRecordGroup(taskManager, resourceManager), createResourceRecordGroup(resourceManager));
  }

  public GanttCSVOpen(final File file, final TaskManager taskManager, final HumanResourceManager resourceManager) {
    this(new Supplier<Reader>() {
      @Override
      public Reader get() {
        try {
          return new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }, taskManager, resourceManager);
  }

  private static RecordGroup createTaskRecordGroup(final TaskManager taskManager, final HumanResourceManager resourceManager) {
    return new TaskRecords(taskManager, resourceManager);
  }

  protected static void createCustomProperties(Collection<String> customFields, CustomPropertyManager customPropertyManager) {
    for (String name : customFields) {
      customPropertyManager.createDefinition(name, CustomPropertyClass.TEXT.getID(), name, null);
    }
  }

  private static RecordGroup createResourceRecordGroup(HumanResourceManager resourceManager) {
    return resourceManager == null ? null : new ResourceRecords(resourceManager);
  }

  /**
   * Create tasks from file.
   *
   * @throws IOException
   *           on parse error or input read-failure
   */
  public boolean load() throws IOException {
    final Logger logger = GPLogger.getLogger(GanttCSVOpen.class);
    CSVParser parser = new CSVParser(myInputSupplier.get(),
        CSVFormat.DEFAULT.withIgnoreEmptyLines(false).withIgnoreSurroundingSpaces(true));
    int numGroup = 0;
    RecordGroup currentGroup = null;
    boolean searchHeader = true;
    List<CSVRecord> records = parser.getRecords();
    debug(logger, "[CSV] read %d records. Searching for a header of %s", records.size(), myRecordGroups.get(numGroup));
    for (CSVRecord record : records) {
      if (record.size() == 0) {
        // If line is empty then current record group is probably finished.
        // Let's search for the next group header.
        searchHeader = true;
        continue;
      }
      if (searchHeader) {
        debug(logger, "%s\n", record);
        // Record is not empty and we're searching for header.
        if (numGroup < myRecordGroups.size() && myRecordGroups.get(numGroup).isHeader(record)) {
          debug(logger, "[CSV] ^^^ This seems to be a header");
          // If next group acknowledges the header, then we give it the turn,
          // otherwise it was just an empty line in the current group
          searchHeader = false;
          currentGroup = myRecordGroups.get(numGroup);
          //parser.readHeader(record);
          currentGroup.setHeader(Lists.newArrayList(record.iterator()));
          numGroup++;
          continue;
        }
      }
      if (currentGroup != null && currentGroup.doProcess(record)) {
        searchHeader = false;
      } else {
        mySkippedLine++;
      }
    }
    for (RecordGroup group : myRecordGroups) {
      group.postProcess();
    }
    // Succeeded
    return true;
  }

  int getSkippedLineCount() {
    return mySkippedLine;
  }
}
