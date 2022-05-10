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
import biz.ganttproject.customproperty.CustomPropertyClass;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import biz.ganttproject.customproperty.CustomPropertyManager;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.apache.commons.csv.CSVFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sourceforge.ganttproject.GPLogger.debug;
import static net.sourceforge.ganttproject.util.FileUtil.getExtension;

/**
 * Handles opening CSV and XLS files.
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

  private final Supplier<InputStream> myInputSupplier;

  private final SpreadsheetFormat myFormat;

  private final List<RecordGroup> myRecordGroups;

  private int mySkippedLine;

  private CSVOptions myCsvOptions;

  public GanttCSVOpen(Supplier<InputStream> inputSupplier, SpreadsheetFormat format, RecordGroup... groups) {
    myInputSupplier = inputSupplier;
    myRecordGroups = Lists.newArrayList();
    for (RecordGroup group : groups) {
      if (group != null) {
        myRecordGroups.add(group);
      }
    }
    myFormat = format;
  }

  public GanttCSVOpen(Supplier<InputStream> inputSupplier, SpreadsheetFormat format, final TaskManager taskManager,
                      final HumanResourceManager resourceManager, RoleManager roleManager, TimeUnitStack timeUnitStack) {
    this(inputSupplier, format, createTaskRecordGroup(taskManager, resourceManager, timeUnitStack),
        createResourceRecordGroup(resourceManager, roleManager));
  }

  public GanttCSVOpen(final File file, final TaskManager taskManager, final HumanResourceManager resourceManager,
                      final RoleManager roleManager, TimeUnitStack timeUnitStack) {
    this(() -> {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }, createSpreadsheetFormat(file), taskManager, resourceManager, roleManager, timeUnitStack);
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

  private static boolean isEmpty(SpreadsheetRecord record) {
    if (record.size() == 0) {
      return true;
    }
    return record.isEmpty();
  }

  private int doLoad(SpreadsheetReader reader, int numGroup, int linesToSkip) {
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

    for (Iterator<SpreadsheetRecord> it = reader.iterator(); it.hasNext(); ) {
      SpreadsheetRecord record = it.next();
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

//            List<String> headerCells = Lists.newArrayList(record.iterator());
//            for (int i = headerCells.size() - 1; i >= 0; i--) {
//              if (Strings.isNullOrEmpty(headerCells.get(i))) {
//                headerCells.remove(i);
//              }
//            }
            nextGroup.setHeader(record);
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
   * @throws IOException on parse error or input read-failure
   */
  public List<Pair<Level, String>> load() throws IOException {
    final List<Pair<Level, String>> errors = Lists.newArrayList();
    for (RecordGroup group : myRecordGroups) {
      group.setErrorOutput(errors);
    }
    int idxCurrentGroup = 0;
    int idxNextGroup;
    int skipHeadLines = 0;
    SpreadsheetRecord headers;
    do {
      idxNextGroup = idxCurrentGroup;
      RecordGroup currentGroup = myRecordGroups.get(idxCurrentGroup);
      headers = currentGroup.getHeader();
      if (headers != null) {
        idxNextGroup++;
      }

      try (SpreadsheetReader reader = createReader(myInputSupplier.get(), headers == null ? null : headers.notBlankValues())) {
        skipHeadLines = doLoad(reader, idxCurrentGroup, skipHeadLines);
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

  private SpreadsheetReader createReader(InputStream is, List<String> headers) throws IOException {
    switch (myFormat) {
      case CSV:
        return new CsvReaderImpl(is, createCSVFormat(headers));
      case XLS:
        return new XlsReaderImpl(is, headers);
      default:
        throw new IllegalArgumentException("Unsupported format: " + myFormat);
    }
  }

  private CSVFormat createCSVFormat(List<String> headers) {
    CSVFormat format = CSVFormat.DEFAULT.withIgnoreEmptyLines(false).withIgnoreSurroundingSpaces(true);
    if (myCsvOptions != null) {
      format = format.withDelimiter(myCsvOptions.sSeparatedChar.charAt(0)).withQuote(myCsvOptions.sSeparatedTextChar.charAt(0));
    }
    if (headers != null) {
      format = format.withHeader(headers.toArray(new String[0]));
    }
    return format;
  }

  private static SpreadsheetFormat createSpreadsheetFormat(File file) {
    String extension = getExtension(file);
    if (extension.isEmpty()) {
      throw new IllegalArgumentException("No file extension!");
    }
    return SpreadsheetFormat.Companion.getSpreadsheetFormat(extension);
  }
}
