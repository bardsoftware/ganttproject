/*
Copyright 2017 Roman Torkhov, BarD Software s.r.o

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

import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * @author torkhov
 */
class CsvReaderImpl implements SpreadsheetReader {

  private final CSVParser myParser;

  CsvReaderImpl(InputStream is, CSVFormat format) throws IOException {
    myParser = new CSVParser(new InputStreamReader(is, Charsets.UTF_8), format);
  }

  @Override
  public void close() throws IOException {
    myParser.close();
  }

  @Override
  public Iterator<SpreadsheetRecord> iterator() {
    return Iterators.transform(myParser.iterator(), CsvRecordImpl::new);
  }
}