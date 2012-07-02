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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * Handles opening CSV files.
 */
public class GanttCSVOpen {
  public static abstract class RecordGroup {
    private final Set<String> myFields;

    public RecordGroup(Set<String> fields) {
      myFields = fields;
    }

    boolean isHeader(CSVRecord record) {
      return Sets.newHashSet(record.iterator()).containsAll(myFields);
    }

    protected abstract void process(CSVRecord record);
  }
  /** List of known (and supported) Task attributes */
  public enum TaskFields {
    NAME("tableColName"), BEGIN_DATE("tableColBegDate"), END_DATE("tableColEndDate"), WEB_LINK("webLink"), NOTES(
        "notes");

    private final String text;

    private TaskFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return language.getText(text);
    }
  }

  public enum ResourceFields {
    ID("tableColID"), NAME("tableColResourceName"), EMAIL("tableColResourceEMail"), PHONE("tableColResourcePhone"), ROLE("tableColResourceRole");

    private final String text;

    private ResourceFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return language.getText(text);
    }
  }

  private static Collection<String> getFieldNames(Enum[] fieldsEnum) {
    return Collections2.transform(Arrays.asList(fieldsEnum), new Function<Enum, String>() {
      @Override
      public String apply(Enum input) {
        return input.toString();
      }
    });
  }

  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final List<RecordGroup> myRecordGroups;

  private final Supplier<Reader> myInputSupplier;

  public GanttCSVOpen(Supplier<Reader> inputSupplier, RecordGroup group) {
    myInputSupplier = inputSupplier;
    myRecordGroups = ImmutableList.of(group);
  }
  public GanttCSVOpen(Supplier<Reader> inputSupplier, RecordGroup... groups) {
    myInputSupplier = inputSupplier;
    myRecordGroups = Arrays.asList(groups);
  }

  private static RecordGroup createTaskRecordGroup(final TaskManager taskManager) {
    return new RecordGroup(Sets.newHashSet(getFieldNames(TaskFields.values()))) {
      @Override
      protected void process(CSVRecord record) {
        assert record.size() > 0;
        // Create the task
        TaskManager.TaskBuilder builder = taskManager.newTaskBuilder().withName(record.get(TaskFields.NAME.toString())).withStartDate(
            language.parseDate(record.get(TaskFields.BEGIN_DATE.toString()))).withEndDate(
            language.parseDate(record.get(TaskFields.END_DATE.toString()))).withWebLink(
            record.get(TaskFields.WEB_LINK.toString())).withNotes(record.get(TaskFields.NOTES.toString()));
        Task task = builder.build();
      }
    };
  }

  private static RecordGroup createResourceRecordGroup(final HumanResourceManager resourceManager) {
    return new RecordGroup(Sets.newHashSet(getFieldNames(ResourceFields.values()))) {
      @Override
      protected void process(CSVRecord record) {
        assert record.size() > 0;
        HumanResource hr = resourceManager.newResourceBuilder().withName(record.get(ResourceFields.NAME.toString())).withID(
            record.get(ResourceFields.ID.toString())).withEmail(record.get(ResourceFields.EMAIL.toString())).withPhone(
            record.get(ResourceFields.PHONE.toString())).withRole(record.get(ResourceFields.ROLE.toString())).build();
      }
    };
  }

  public GanttCSVOpen(final File file, final TaskManager taskManager, final HumanResourceManager resourceManager) {
    this(new Supplier<Reader>() {
      @Override
      public Reader get() {
        try {
          return new InputStreamReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }, createTaskRecordGroup(taskManager), createResourceRecordGroup(resourceManager));
  }

  /**
   * Create tasks from file.
   *
   * @throws IOException
   *           on parse error or input read-failure
   */
  public boolean load() throws IOException {
    CSVParser parser = new CSVParser(myInputSupplier.get(),
        CSVFormat.DEFAULT.withEmptyLinesIgnored(false).withSurroundingSpacesIgnored(true));
    int numGroup = 0;
    RecordGroup currentGroup = null;
    boolean searchHeader = true;
    List<CSVRecord> records = parser.getRecords();
    for (CSVRecord record : records) {
      if (record.size() == 0) {
        // If line is empty then current record group is probably finished.
        // Let's search for the next group header.
        searchHeader = true;
        continue;
      }
      if (searchHeader) {
        // Record is not empty and we're searching for header.
        if (numGroup < myRecordGroups.size() && myRecordGroups.get(numGroup).isHeader(record)) {
          // If next group acknowledges the header, then we give it the turn,
          // otherwise it was just an empty line in the current group
          searchHeader = false;
          currentGroup = myRecordGroups.get(numGroup);
          parser.readHeader(record);
          numGroup++;
          continue;
        }
        searchHeader = false;
      }
      assert currentGroup != null;
      currentGroup.process(record);
    }
    // Succeeded
    return true;
  }
}
