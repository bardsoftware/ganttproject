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
package net.sourceforge.ganttproject.importer;

import java.io.File;
import java.io.IOException;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.io.GanttCSVOpen;

public class ImporterFromCsvFile extends ImporterBase {

  @Override
  public String getFileNamePattern() {
    return "csv";
  }

  @Override
  public String getFileTypeDescription() {
    return language.getText("csvFiles");
  }

  @Override
  public void run(File selectedFile) {
    GanttCSVOpen opener = new GanttCSVOpen(selectedFile, getProject().getTaskManager(), getProject().getHumanResourceManager());
    try {
      opener.load();
    } catch (IOException e) {
      GPLogger.log(e);
    }
  }
}
