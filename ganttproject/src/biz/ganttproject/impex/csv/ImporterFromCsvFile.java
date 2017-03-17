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

import biz.ganttproject.core.option.GPOption;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.util.collect.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * Controls the process of importing CSV file.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ImporterFromCsvFile extends ImporterBase {
  private final HumanResourceMerger.MergeResourcesOption myMergeResourcesOption = new HumanResourceMerger.MergeResourcesOption();
  private final GPOption[] myOptions = new GPOption[] { myMergeResourcesOption };

  public ImporterFromCsvFile() {
    super("csvFiles");
    myMergeResourcesOption.loadPersistentValue(HumanResourceMerger.MergeResourcesOption.BY_ID);
  }

  @Override
  protected GPOption[] getOptions() {
    return myOptions;
  }

  @Override
  public String getFileNamePattern() {
    return "csv";
  }

  @Override
  public String getFileTypeDescription() {
    return language.getText("csvFiles");
  }

  @Override
  public void run() {
    File selectedFile = getFile();
    BufferProject bufferProject = new BufferProject(getProject(), getUiFacade());
    GanttCSVOpen opener = new GanttCSVOpen(selectedFile, bufferProject.getTaskManager(),
        bufferProject.getHumanResourceManager(), bufferProject.getRoleManager(),
        bufferProject.getTimeUnitStack());
    opener.setOptions(((GanttProject)getProject()).getGanttOptions().getCSVOptions());
    try {
      List<Pair<Level, String>> errors = opener.load();
      ImporterFromGanttFile.importBufferProject(getProject(), bufferProject, getUiFacade(), myMergeResourcesOption, null);
      reportErrors(errors, "CSV");
    } catch (IOException e) {
      GPLogger.log(e);
    }
  }
}
