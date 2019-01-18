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

import biz.ganttproject.core.time.TimeUnitStack;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sourceforge.ganttproject.GPLogger.debug;

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

  private CSVOptions myCsvOptions;

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

  public GanttCSVOpen(Supplier<Reader> inputSupplier, final TaskManager taskManager,
      final HumanResourceManager resourceManager, RoleManager roleManager, TimeUnitStack timeUnitStack) {
    this(inputSupplier, createTaskRecordGroup(taskManager, resourceManager, timeUnitStack),
        createResourceRecordGroup(resourceManager, roleManager));
  }

  public GanttCSVOpen(final File file, final TaskManager taskManager, final HumanResourceManager resourceManager,
                      final RoleManager roleManager, TimeUnitStack timeUnitStack) {
    this(new Supplier<Reader>() {
      @Override
      public Reader get() {
        try {
          return new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }, taskManager, resourceManager, roleManager, timeUnitStack);
  }

  private static RecordGroup createTaskRecordGroup(final TaskManager taskManager,
      final HumanResourceManager resourceManager, TimeUnitStack timeUnitStack) {
    return new TaskRecords(taskManager, resourceManager, timeUnitStack);
  }

  protected static void createCustomProperties(Collection<String> customFields, CustomPropertyManager customPropertyManager) {
    for (String name : customFields) {
      customPropertyManager.createDefinition(name, CustomPropertyClass.TEXT.getID(), name, null);
    }
  }

  private static RecordGroup createResourceRecordGroup(HumanResourceManager resourceManager, RoleManager roleManager) {
    return resourceManager == null ? null : new ResourceRecords(resourceManager, roleManager);
  }

  private static boolean isEmpty(CSVRecord record) {
    if (record.size() == 0) {
      return true;
    }
    for (int i = 0; i < record.size(); i++) {
      if (!Strings.isNullOrEmpty(record.get(i))) {
        return false;
      }
    }
    return true;
  }

  private int doLoad(CSVParser parser, int numGroup, int linesToSkip) {
    final Logger logger = GPLogger.getLogger(GanttCSVOpen.class);
    int lineCounter = 0;
    RecordGroup currentGroup = myRecordGroups.get(numGroup);
    boolean searchHeader = currentGroup.getHeader() == null;
    if (searchHeader) {
      debug(logger, "[CSV] Searching for a header of %s", currentGroup);
    } else {
      debug(logger, "[CSV] Expecting to read records of group %s", currentGroup);
      numGroup++;
    }

    for (Iterator<CSVRecord> it = parser.iterator(); it.hasNext();) {
      CSVRecord record = it.next();

      lineCounter++;
      if (linesToSkip-- > 0) {
        continue;
      }
      if (isEmpty(record)) {
        // If line is empty then current record group is probably finished.
        // Let's search for the next group header.
        searchHeader = true;
        continue;
      }
      if (searchHeader) {
        if (numGroup < myRecordGroups.size()) {
          debug(logger, "%s\n", record);
          RecordGroup nextGroup = myRecordGroups.get(numGroup);
          // Record is not empty and we're searching for header.
          if (nextGroup.isHeader(record)) {
            debug(logger, "[CSV] ^^^ This seems to be a header");

            List<String> headerCells = Lists.newArrayList(record.iterator());
            for (int i = headerCells.size() - 1; i >= 0; i--) {
              if (Strings.isNullOrEmpty(headerCells.get(i))) {
                headerCells.remove(i);
              }
            }
            nextGroup.setHeader(headerCells);
            return lineCounter;
          }
        }
      }
      if (currentGroup.doProcess(record)) {
        searchHeader = false;
      } else {
        mySkippedLine++;
      }
    }
    return 0;
  }
  /**
   * Create tasks from file.
   *
   * @throws IOException
   *           on parse error or input read-failure
   */
  public List<Pair<Level, String>> load() throws IOException {
    final Logger logger = GPLogger.getLogger(GanttCSVOpen.class);
    final List<Pair<Level, String>> errors = Lists.newArrayList();
    for (RecordGroup group : myRecordGroups) {
      group.setErrorOutput(errors);
    }
    int idxCurrentGroup = 0;
    int idxNextGroup;
    int skipHeadLines = 0;
    do {
      idxNextGroup = idxCurrentGroup;
      CSVFormat format = CSVFormat.DEFAULT.withIgnoreEmptyLines(false).withIgnoreSurroundingSpaces(true);
      if (myCsvOptions != null) {
        format = format.withDelimiter(myCsvOptions.sSeparatedChar.charAt(0)).withQuote(myCsvOptions.sSeparatedTextChar.charAt(0));
      }
      RecordGroup currentGroup = myRecordGroups.get(idxCurrentGroup);
      if (currentGroup.getHeader() != null) {
        format = format.withHeader(currentGroup.getHeader().toArray(new String[0]));
        idxNextGroup++;
      }
      try (Reader reader = myInputSupplier.get()) {
        CSVParser parser = new CSVParser(reader, format);
        skipHeadLines = doLoad(parser, idxCurrentGroup, skipHeadLines);
      }
      idxCurrentGroup = idxNextGroup;
    } while (skipHeadLines > 0);
    for (RecordGroup group : myRecordGroups) {
      group.postProcess();
    }
    return errors;
  }

  int getSkippedLineCount() {
    return mySkippedLine;
  }

  public void setOptions(CSVOptions csvOptions) {
    myCsvOptions = csvOptions;
  }
}
