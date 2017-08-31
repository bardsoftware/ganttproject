/*
Copyright 2017 Alexandr Kurutin, BarD Software s.r.o

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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Calendar;

/**
 * @author akurutin on 04.04.2017.
 */
public class CsvWriterImpl implements SpreadsheetWriter {
  private final CSVPrinter myCsvPrinter;

  CsvWriterImpl(OutputStream stream, CSVFormat format) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);
    myCsvPrinter = new CSVPrinter(writer, format);
  }

  @Override
  public void print(String value) throws IOException {
    myCsvPrinter.print(value);
  }

  @Override
  public void println() throws IOException {
    myCsvPrinter.println();
  }

  @Override
  public void close() throws IOException {
    myCsvPrinter.flush();
    myCsvPrinter.close();
  }

  @Override
  public void newSheet() throws IOException {
    println();
    println();
  }

  @Override
  public void newSheet(String name) throws IOException {
    newSheet();
  }

  @Override
  public void print(Double value) throws IOException {
    myCsvPrinter.print(String.valueOf(value));
  }

  @Override
  public void print(Integer value) throws IOException {
    myCsvPrinter.print(String.valueOf(value));
  }

  @Override
  public void print(BigDecimal value) throws IOException {
    myCsvPrinter.print(value.toPlainString());
  }

  @Override
  public void print(Calendar value) throws IOException {
    myCsvPrinter.print(value.toString());
  }
}
