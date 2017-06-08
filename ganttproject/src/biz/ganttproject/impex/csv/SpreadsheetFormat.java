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

import com.google.common.base.Preconditions;

/**
 * @author torkhov
 */
public enum SpreadsheetFormat {

  CSV("csv"), XLS("xls");

  private final String myExtension;

  SpreadsheetFormat(String extension) {
    myExtension = extension;
  }

  public String getExtension() {
    return myExtension;
  }

  public static SpreadsheetFormat getSpreadsheetFormat(String extension) {
    extension = Preconditions.checkNotNull(extension);
    for (SpreadsheetFormat format : values()) {
      if (format.getExtension().equalsIgnoreCase(extension)) {
        return format;
      }
    }
    throw new IllegalArgumentException("No enum constant extension: " + extension);
  }

  @Override
  public String toString() {
    return "impex.csv.fileformat." + name().toLowerCase();
  }
}