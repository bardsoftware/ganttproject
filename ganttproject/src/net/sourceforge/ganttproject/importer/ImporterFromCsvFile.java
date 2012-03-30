package net.sourceforge.ganttproject.importer;

import java.io.File;

import net.sourceforge.ganttproject.io.GanttCSVOpen;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ImporterFromCsvFile extends ImporterBase implements Importer {

  @Override
  public String getFileNamePattern() {
    return "csv";
  }

  @Override
  public String getFileTypeDescription() {
    return GanttLanguage.getInstance().getText("csvFiles");
  }

  @Override
  public void run(File selectedFile) {
    GanttCSVOpen opener = new GanttCSVOpen(selectedFile, getProject().getTaskManager());
    opener.load();
  }
}
