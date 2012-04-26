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
    GanttCSVOpen opener = new GanttCSVOpen(selectedFile, getProject().getTaskManager());
    try {
      opener.load();
    } catch (IOException e) {
      GPLogger.log(e);
    }
  }
}
